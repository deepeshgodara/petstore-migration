# Architecture Overview

## 1. Executive Modernization Strategy

The Java Pet Store modernization is built upon the **Dual-Write and Shadow Reconciliation Pattern**, a robust evolution of Martin Fowler’s *Strangler Fig Pattern* tailored for stateful enterprise e-commerce systems.

Rather than executing a risky "Big Bang" offline migration, the platform preserves business continuity by running the modern microservices alongside the legacy 2002 J2EE monolith, verifying 100% data parity continuously through automated shadow reads.

```
+----------------------------------------------------------------------------------------------------+
|                                    TARGET MODERN ARCHITECTURE                                      |
+----------------------------------------------------------------------------------------------------+
|  CLIENT LAYER:                                                                                     |
|  - React 18 SPA (Vite + TypeScript) with RBAC (/storefront, /account, /admin, /supplier, /ops)     |
+----------------------------------------------------------------------------------------------------+
|  API ROUTING & REVERSE PROXY:                                                                      |
|  - Vite Proxy / Gateway forwarding:                                                                |
|      /api/v1/categories/** -> Catalog Service (:8081)                                             |
|      /api/v1/products/**   -> Catalog Service (:8081)                                             |
|      /api/v1/items/**      -> Catalog Service (:8081)                                             |
|      /api/v1/orders/**     -> Order Service (:8082)                                               |
|      /api/v1/migration/**  -> Migration Service (:8085)                                           |
+----------------------------------------------------------------------------------------------------+
|  EVENT-DRIVEN BACKBONE (Apache Kafka 3.7+ KRaft):                                                  |
|  - petstore.orders.created    -> Domain event for customer checkout                                |
|  - petstore.orders.approved   -> Domain event for administrative approval                          |
|  - petstore.orders.completed  -> Domain event for fulfillment completion                           |
|  - petstore.orders.dualwrite  -> Asynchronous propagation to secondary datastores                 |
|  - petstore.orders.dlq        -> Dead-letter queue isolating unparseable or transient failures     |
+----------------------------------------------------------------------------------------------------+
|  PERSISTENCE TIER:                                                                                 |
|  - Modern Store: MongoDB 7.0 Replica Set 'rs0' (Collections: petstore_orders, petstore_products)    |
|  - Legacy System of Record: Cloudscape / HSQLDB in TomEE container (:8000/petstore)                |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. Domain-Driven Design (DDD) & Aggregate Boundaries

### 2.1 Catalog Context (`petstore-catalog-service`)
- **Aggregate Root**: `ProductDocument`
  - Encapsulates product details, images, category membership, and embedded child `ItemDocument` inventory SKUs.
  - Features polymorphic multi-lingual maps (`names`, `descriptions`, `attributes`) eliminating 3NF relational detail tables (`PRODUCT_DETAILS`, `ITEM_DETAILS`).
  - Supports live inventory updates from suppliers via atomic document mutations.

### 2.2 Order & Checkout Context (`petstore-order-service`)
- **Aggregate Root**: `OrderDocument`
  - Encapsulates the entire purchase order lifecycle:
    - Customer snapshot (`userId`, `locale`)
    - Billing & Shipping addresses (`AddressDocument`)
    - Payment information (`PaymentDocument` with tokenized/masked card details)
    - Purchased line items (`List<LineItemDocument>` with localized names, attributes, prices, and images)
  - Eliminates 5-table relational joins across legacy tables: `PURCHASEORDER`, `LINEITEM`, `CONTACTINFO`, `ADDRESS`, and `CREDITCARD`.

### 2.3 Migration & Reconciliation Context (`petstore-migration-service`)
- Encapsulates:
  - Spring Batch historical cursor reader (`LegacyDatabaseConfig`, `LegacyOrderCursorReader`).
  - Asynchronous dual-write consumer (`DualWriteConsumer`).
  - Real-time shadow read comparator (`ShadowReadComparator`).
  - Live database engine diagnostics (`MongoDiagnosticsService`).

---

## 3. Dual-Write & Shadow Reconciliation Flow

```
                      Customer Checkout / Admin Action
                                     │
                                     ▼
                        [petstore-order-service]
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼ (Primary Transaction)                 ▼ (Asynchronous Dual-Write)
      [MongoDB Replica Set rs0]                 Kafka Topic: petstore.orders.dualwrite
                 │                                       │
                 │                                       ▼
                 │                         [DualWriteConsumer (Migration Service)]
                 │                                       │
                 │                                       ▼
                 │                             [Legacy HSQLDB / TomEE]
                 │                                       │
                 └──────────────┐        ┌───────────────┘
                                ▼        ▼
                          [ShadowReadComparator]
                                     │
                     ┌───────────────┴───────────────┐
                     ▼                               ▼
             Match (100% Parity)            Drift Detected
                     │                               │
            Increments Metric                Logs Discrepancy &
          totalMatches in /ops               Triggers Idempotent Upsert
```

### Key Safety Mechanisms:
1. **Asynchronous Decoupling**: Secondary datastore writes run asynchronously via Kafka. If MongoDB or the legacy database experiences latency or an outage, customer transactions are never blocked.
2. **Dead-Letter Queue (DLQ)**: If a write fails permanently after retries, it routes to `petstore.orders.dlq` for SRE inspection and replay.
3. **Idempotency**: All MongoDB writes use deterministic IDs (`_id: orderId`). Replayed dual-write events result in safe idempotent upserts.

---

> [!TIP]
> **Legacy System Deep Dive**: For an exhaustive architectural breakdown of the 2002 J2EE BluePrints implementation (the 4 `.ear` archives, WAF framework, EJB 2.0 component model, 3NF schema, and order flow), read the [Legacy Pet Store Architecture & Component Deep Dive](Legacy-PetStore-Architecture-&-Components).

