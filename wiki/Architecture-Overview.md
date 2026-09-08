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
                    ┌────────────────┴────────────────┐
                    ▼ (Atomic @Transactional Unit)    ▼
         [MongoDB Replica Set rs0]        [petstore_outbox]
         - petstore_orders (@Version)     - Event UUID
         - petstore_users (BCrypt)        - Payload & Status: PENDING
                                                      │
                                                      ▼ (Polled every 500ms)
                                           [OutboxRelayScheduler]
                                                      │
                                                      ▼
                                   Kafka Topic: petstore.orders.dualwrite
                                                      │
                                                      ▼
                                    [petstore-migration-service]
                                     ┌────────────────┴────────────────┐
                                     ▼                                 ▼
                          [DualWriteConsumer]             [LegacyWriteBackConsumer]
                                     │                                 │
                          (Mirrors to MongoDB)             (Replays SQL to HSQLDB)
                                     │                                 │
                                     └──────────────┬──────────────────┘
                                                    │
                                                    ▼
                                          [ShadowReadComparator]
                                            (O(1) Map Pre-Indexing)
                                                    │
                                    ┌───────────────┴───────────────┐
                                    ▼                               ▼
                            Match (100% Parity)            Drift Detected
                                    │                               │
                           Increments Metric                Logs Discrepancy &
                         totalMatches in /ops               Triggers Idempotent Upsert
```

### Key Safety & Production Hardening Mechanisms:
1. **Transactional Outbox Pattern**: Order mutations and outbound event notifications are persisted atomically in a single multi-document `@Transactional` boundary inside MongoDB replica set `rs0` (`petstore_orders` and `petstore_outbox`). The background `OutboxRelayScheduler` polls pending events and reliably relays them to Kafka. Even if Kafka is temporarily offline, customer orders are never lost or blocked.
2. **Optimistic Concurrency Control (`@Version`)**: `OrderDocument` is protected by `@Version private Long version;`. Any concurrent state change (e.g., simultaneous customer cancellation and admin approval) is detected and resolved safely via Compare-And-Swap (CAS), preventing lost updates without costly table locks.
3. **Multi-Document ACID Transactions**: Configured via `MongoTransactionManager` bound to replica set `rs0`. Mutations across orders and outbox collections commit or roll back atomically.
4. **BSON Decimal128 Monetary Precision**: All monetary values (`totalPrice`, `unitCost`, `listPrice`) use `java.math.BigDecimal` mapped to BSON `Decimal128` (IEEE 754-2008) via `MongoConfig` custom converters, preventing floating-point rounding errors and string-sorting aggregation bugs.
5. **Reverse Write-Back for Zero-Downtime Rollback**: `LegacyWriteBackConsumer` listens to `petstore.orders.created` and replicates new modern orders back into the legacy HSQLDB relational tables. If the business ever needs to fail back to the legacy monolith during cutover, legacy data remains completely up to date.
6. **O(1) Pre-Indexed Shadow Reconciliation**: `ShadowReadComparator` loads target MongoDB records into an in-memory hash map keyed by deterministic IDs (`_id`), transforming audit verification from $O(N \times M)$ nested scans into an ultra-fast $O(N)$ lookup.
7. **Enterprise Password Hardening & Lazy Migration**: `UserService` integrates `BCryptPasswordEncoder`. During login, legacy SHA-1/plain hashes are validated and lazily upgraded in MongoDB to salted BCrypt hashes (work factor 10), securing credentials while maintaining seamless backward compatibility.
8. **Dynamic Dual-Write Kill Switch**: `@Value("${migration.dualwrite.enabled:true}")` allows operations teams to pause or resume dual-write traffic instantly at runtime via Spring Boot Actuator `/actuator/refresh` without restarting JVMs.

---

> [!TIP]
> **Legacy System Deep Dive**: For an exhaustive architectural breakdown of the 2002 J2EE BluePrints implementation (the 4 `.ear` archives, WAF framework, EJB 2.0 component model, 3NF schema, and order flow), read the [Legacy Pet Store Architecture & Component Deep Dive](Legacy-PetStore-Architecture-&-Components).


