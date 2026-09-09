# Pet Store Modernization: Master Class & Sequence Diagrams

This document serves as the comprehensive architectural reference for the modernized Java Pet Store application, detailing the end-to-end class interactions and sequence diagrams for all API requests.

---

## Table of Contents
1. [Master Class & Component Interaction Diagram](#1-master-class--component-interaction-diagram)
2. [Sequence Diagrams for All API Request Types](#2-sequence-diagrams-for-all-api-request-types)
   - [Flow 1: Customer Checkout (`POST /api/v1/orders`)](#flow-1-customer-checkout-post-apiv1orders)
   - [Flow 2: User Authentication & Lazy BCrypt Upgrade (`POST /api/v1/users/login`)](#flow-2-user-authentication--lazy-bcrypt-upgrade-post-apiv1userslogin)
   - [Flow 3: Admin Approval Workflow (`PUT /api/v1/orders/{orderId}/status`)](#flow-3-admin-approval-workflow-put-apiv1ordersorderidstatus)
   - [Flow 4: Supplier Inventory Restock (`PUT /api/v1/items/{itemId}/inventory`)](#flow-4-supplier-inventory-restock-put-apiv1itemsitemidinventory)
   - [Flow 5: Shadow Reconciliation & Real-Time Parity Audit (`GET /api/v1/migration/parity?runAudit=true`)](#flow-5-shadow-reconciliation--real-time-parity-audit-get-apiv1migrationparityrunaudittrue)
   - [Flow 6: Secondary Datastore Outage & Chaos DLQ Recovery](#flow-6-secondary-datastore-outage--chaos-dlq-recovery)
3. [Key Architectural Highlights & Interview Defenses](#3-key-architectural-highlights--interview-defenses)

---

## 1. Master Class & Component Interaction Diagram

This diagram maps all tiers from customer and administrator browser interactions down to the physical storage and message streaming layers.

```mermaid
classDiagram
    direction TB

    %% ================= FRONTEND TIER =================
    namespace Frontend_React_Vite {
        class StorefrontUI {
            +renderCatalog()
            +handleAddToCart()
            +submitCheckout()
        }
        class AdminDashboardUI {
            +viewPendingOrders()
            +approveOrder(orderId)
            +renderSalesKPIs()
        }
        class SupplierPortalUI {
            +browseInventory()
            +updateStock(itemId, qty)
        }
        class OpsParityDashboardUI {
            +triggerAudit()
            +viewDrifts()
        }
        class FrontendServices {
            <<TypeScript Axios/Fetch>>
            +orderService.placeOrder()
            +catalogService.getItems()
            +supplierService.updateInventory()
            +migrationService.getParity()
            +userService.login()
        }
    }

    %% ================= GATEWAY / PROXY =================
    namespace Gateway_Layer {
        class ViteReverseProxy {
            <<vite.config.ts>>
            +route /api/v1/categories -> :8081
            +route /api/v1/orders -> :8082
            +route /api/v1/users -> :8082
            +route /api/v1/migration -> :8085
        }
    }

    %% ================= BACKEND CONTROLLERS =================
    namespace Microservice_Controllers {
        class CatalogController {
            <<Port 8081>>
            +getCategories()
            +getItems()
            +updateInventory(itemId, qty)
        }
        class OrderController {
            <<Port 8082>>
            +placeOrder(CreateOrderRequest)
            +updateOrderStatus(orderId, status)
            +getOrderById(orderId)
            +getAdminAnalytics()
        }
        class UserController {
            <<Port 8082>>
            +register(UserRegistrationRequest)
            +login(UserLoginRequest)
        }
        class MigrationController {
            <<Port 8085>>
            +triggerBaseline()
            +getParity(runAudit)
            +getMongoDiagnostics()
        }
    }

    %% ================= BACKEND SERVICES =================
    namespace Microservice_Services {
        class CatalogService {
            +getAllCategories()
            +updateItemInventory(itemId, qty)
        }
        class OrderService {
            +generateUniqueOrderId() String
            +placeOrder(CreateOrderRequest) OrderDocument
            +updateOrderStatus(orderId, status)
        }
        class UserService {
            +register(request) UserResponse
            +login(request) UserResponse
            -upgradeLegacyPassword(doc, raw)
        }
        class ShadowReadComparator {
            +compareOrders()
            +compareCatalog()
            +compareUsers()
        }
        class ParityDashboardService {
            +getDashboardSummary(runAudit)
        }
        class DiscrepancyLogger {
            +recordComparison(result)
            +calculateCutoverScore()
        }
    }

    %% ================= EVENT / ASYNC LAYER =================
    namespace Event_And_Outbox_Layer {
        class DualWritePublisher {
            +publishOrderCreated(order)
            +publishOrderStatusUpdated(order)
        }
        class OutboxRelayScheduler {
            +relayPendingOutboxRecords()
        }
        class OrderEventProducer {
            +publishOrderCreated(order)
            +publishOrderStatusUpdated(order)
        }
        class MongoDualWriteConsumer {
            <<@KafkaListener>>
            +onDualWriteEvent(event)
        }
        class LegacyWriteBackConsumer {
            <<@KafkaListener>>
            +onOrderEventForLegacyWriteBack(event)
        }
        class OrderDlqConsumer {
            <<@KafkaListener>>
            +onDlqEvent(record)
        }
    }

    %% ================= REPOSITORIES =================
    namespace Persistence_Layer {
        class OrderRepository {
            <<MongoRepository>>
            +existsById(id)
            +save(order)
            +findById(id)
        }
        class OutboxRepository {
            <<MongoRepository>>
            +save(outboxDoc)
            +findByRelayedFalse()
        }
        class UserRepository {
            <<MongoRepository>>
            +findByUsername(username)
            +save(userDoc)
        }
        class ProductRepository {
            <<MongoRepository>>
            +findFirstByItemsItemId(itemId)
        }
        class LegacyOrderCursorReader {
            <<JdbcTemplate>>
            +readAllLegacyOrders()
        }
    }

    %% ================= DATASTORES =================
    namespace Datastores {
        class MongoDB_rs0 {
            <<Document Store>>
            petstore_orders
            petstore_users
            petstore_products
            petstore_outbox
        }
        class Kafka_KRaft {
            <<Message Bus>>
            petstore.orders.dualwrite
            petstore.orders.created
            petstore.orders.approved
            petstore.orders.dlq
        }
        class Legacy_HSQLDB {
            <<Relational 2002 DB>>
            PURCHASEORDER
            LINEITEM
            ACCOUNT
        }
    }

    %% WIRING RELATIONSHIPS
    StorefrontUI --> FrontendServices
    AdminDashboardUI --> FrontendServices
    SupplierPortalUI --> FrontendServices
    OpsParityDashboardUI --> FrontendServices
    FrontendServices --> ViteReverseProxy

    ViteReverseProxy --> CatalogController : :8081
    ViteReverseProxy --> OrderController : :8082
    ViteReverseProxy --> UserController : :8082
    ViteReverseProxy --> MigrationController : :8085

    CatalogController --> CatalogService
    OrderController --> OrderService
    UserController --> UserService
    MigrationController --> ParityDashboardService
    MigrationController --> ShadowReadComparator

    OrderService --> OrderRepository
    OrderService --> OutboxRepository
    OrderService --> DualWritePublisher
    OrderService --> OrderEventProducer
    UserService --> UserRepository
    CatalogService --> ProductRepository

    OutboxRelayScheduler --> OutboxRepository
    OutboxRelayScheduler --> DualWritePublisher

    DualWritePublisher ..> Kafka_KRaft : produces to petstore.orders.dualwrite
    OrderEventProducer ..> Kafka_KRaft : produces to petstore.orders.created / approved

    Kafka_KRaft ..> MongoDualWriteConsumer : consumes dualwrite
    Kafka_KRaft ..> LegacyWriteBackConsumer : consumes dualwrite (disabled)
    Kafka_KRaft ..> OrderDlqConsumer : consumes dlq

    OrderRepository --> MongoDB_rs0
    OutboxRepository --> MongoDB_rs0
    UserRepository --> MongoDB_rs0
    ProductRepository --> MongoDB_rs0
    MongoDualWriteConsumer --> MongoDB_rs0

    ShadowReadComparator --> MongoDB_rs0 : Reads modern state
    ShadowReadComparator --> LegacyOrderCursorReader : Reads legacy state
    LegacyOrderCursorReader --> Legacy_HSQLDB : JDBC read-only
    LegacyWriteBackConsumer -.-> Legacy_HSQLDB : (Disabled SQL write-back)
    ShadowReadComparator --> DiscrepancyLogger
    ParityDashboardService --> ShadowReadComparator
```

---

## 2. Sequence Diagrams for All API Request Types

### Flow 1: Customer Checkout (`POST /api/v1/orders`)
**Core Principle**: Generates a 17-digit collision-free numeric ID, atomically saves to MongoDB & Outbox in a single local transaction, and non-blockingly emits to Kafka without locking the legacy database.

```mermaid
sequenceDiagram
    autonumber
    actor Shopper as Customer (Browser)
    participant UI as StorefrontUI / orderService.ts
    participant Proxy as Vite Reverse Proxy (:3000)
    participant Ctrl as OrderController (:8082)
    participant Svc as OrderService
    participant Mongo as MongoDB (rs0)
    participant Kafka as Kafka (KRaft :9092)
    participant DualConsumer as MongoDualWriteConsumer (:8085)
    participant WriteBack as LegacyWriteBackConsumer (:8085)
    participant LegacyDB as Legacy HSQLDB (TomEE)

    Shopper->>UI: Click "Place Order" ($1742.00)
    UI->>Proxy: POST /api/v1/orders { userId, lineItems, payment, address }
    Proxy->>Ctrl: Forward to http://localhost:8082/api/v1/orders
    Ctrl->>Svc: placeOrder(CreateOrderRequest)
    
    rect rgb(240, 248, 255)
    Note over Svc, Mongo: Phase 1: Unique Numeric ID Generation & Validation
    loop Up to 5 optimistic retries
        Svc->>Svc: candidateId = currentTimeMillis() + random4Digits()
        Svc->>Mongo: orderRepository.existsById(candidateId)
        Mongo-->>Svc: false (Available!)
    end
    end

    rect rgb(245, 255, 245)
    Note over Svc, Mongo: Phase 2: Atomic Local Persistence (@Transactional)
    Svc->>Mongo: orderRepository.save(OrderDocument #178892...)
    Svc->>Mongo: outboxRepository.save(OutboxDocument)
    Mongo-->>Svc: Success (ACID write complete)
    end

    rect rgb(255, 250, 240)
    Note over Svc, Kafka: Phase 3: Asynchronous Non-Blocking Event Dispatch
    Svc->>Kafka: DualWritePublisher sends OrderDualWriteEvent -> petstore.orders.dualwrite
    Svc->>Kafka: OrderEventProducer sends OrderDomainEvent -> petstore.orders.created
    end

    Svc-->>Ctrl: Saved OrderDocument
    Ctrl-->>Proxy: 201 Created (OrderDocument #178892...)
    Proxy-->>UI: 201 Created
    UI-->>Shopper: Display Order Confirmation (#178892...)

    rect rgb(250, 240, 255)
    Note over Kafka, LegacyDB: Phase 4: Downstream Asynchronous Processing
    Kafka-->>DualConsumer: Consume from petstore.orders.dualwrite
    DualConsumer->>Mongo: Check exists(orderId) -> Already present -> Skip insert
    
    Kafka-->>WriteBack: Consume from petstore.orders.dualwrite
    alt writeBackEnabled == false (Current Safe Mode)
        WriteBack->>WriteBack: Log debug & return (NO LOCK on Legacy DB!)
    else writeBackEnabled == true (Optional Legacy Sync Mode)
        WriteBack->>LegacyDB: INSERT INTO PURCHASEORDER... (Buffered via Kafka)
    end
    end
```

---

### Flow 2: User Authentication & Lazy BCrypt Upgrade (`POST /api/v1/users/login`)
**Core Principle**: Transparently upgrades legacy plaintext passwords (`LEGACY_PLAINTEXT:`) to 12-round BCrypt hashes upon first successful login without requiring user password resets.

```mermaid
sequenceDiagram
    autonumber
    actor User as Customer / Admin
    participant UI as LoginModal / userService.ts
    participant Ctrl as UserController (:8082)
    participant Svc as UserService
    participant Repo as UserRepository
    participant Mongo as MongoDB (petstore_users)

    User->>UI: Enter username="shopper", password="password"
    UI->>Ctrl: POST /api/v1/users/login { username, password }
    Ctrl->>Svc: login(UserLoginRequest)
    Svc->>Repo: findByUsername("shopper")
    Repo->>Mongo: Query {'username': 'shopper'}
    Mongo-->>Repo: UserDocument
    Repo-->>Svc: UserDocument

    alt Password starts with "LEGACY_PLAINTEXT:"
        Note over Svc: Legacy 2002 password detected!
        Svc->>Svc: Compare raw password against legacy text -> MATCH
        Svc->>Svc: newHash = BCrypt.hashpw(rawPassword, 12 rounds)
        Svc->>Svc: userDoc.setPassword(newHash)
        Svc->>Repo: save(userDoc)
        Repo->>Mongo: Update {'password': '$2a$12$...'}
        Note over Svc, Mongo: Password lazily upgraded to BCrypt!
    else Password is standard BCrypt ($2a$)
        Svc->>Svc: BCrypt.checkpw(rawPassword, storedHash) -> MATCH
    end

    Svc-->>Ctrl: UserResponse (userId, username, token)
    Ctrl-->>UI: 200 OK with UserResponse
    UI-->>User: Authenticated & Session Started
```

---

### Flow 3: Admin Approval Workflow (`PUT /api/v1/orders/{orderId}/status`)
**Core Principle**: Administrators transition orders from `PENDING` $\to$ `APPROVED`, updating MongoDB and dispatching dedicated domain events to Kafka.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Store Administrator
    participant UI as AdminDashboardUI
    participant Ctrl as OrderController (:8082)
    participant Svc as OrderService
    participant Mongo as MongoDB (petstore_orders & outbox)
    participant Kafka as Kafka (KRaft :9092)

    Admin->>UI: Click "Approve Order" for #178892...
    UI->>Ctrl: PUT /api/v1/orders/178892.../status { status: "APPROVED" }
    Ctrl->>Svc: updateOrderStatus("178892...", APPROVED)
    Svc->>Mongo: orderRepository.findById("178892...")
    Mongo-->>Svc: OrderDocument (status=PENDING)
    
    Svc->>Svc: order.setStatus(APPROVED), order.setUpdatedAt(now)
    Svc->>Mongo: orderRepository.save(order)
    Svc->>Mongo: outboxRepository.save(OutboxDocument: ORDER_STATUS_UPDATED)
    
    Svc->>Kafka: publishOrderStatusUpdated -> petstore.orders.dualwrite
    Svc->>Kafka: publishOrderStatusUpdated -> petstore.orders.approved
    
    Svc-->>Ctrl: Updated OrderDocument (status=APPROVED)
    Ctrl-->>UI: 200 OK (OrderDocument)
    UI-->>Admin: Green badge: Status updated to APPROVED
```

---

### Flow 4: Supplier Inventory Restock (`PUT /api/v1/items/{itemId}/inventory`)
**Core Principle**: Warehouse suppliers restock inventory directly in MongoDB with instant consistency across customer catalog queries.

```mermaid
sequenceDiagram
    autonumber
    actor Supplier as Warehouse Supplier
    participant UI as SupplierPortalUI
    participant Ctrl as CatalogController (:8081)
    participant Svc as CatalogService
    participant Repo as ProductRepository
    participant Mongo as MongoDB (petstore_products)

    Supplier->>UI: Select Item "EST-1" (Angelfish), set Stock = 100
    UI->>Ctrl: PUT /api/v1/items/EST-1/inventory { quantity: 100 }
    Ctrl->>Svc: updateItemInventory("EST-1", 100)
    Svc->>Repo: findFirstByItemsItemId("EST-1")
    Repo->>Mongo: Query {'items.itemId': 'EST-1'}
    Mongo-->>Repo: ProductDocument containing ItemDocument EST-1
    
    Svc->>Svc: Update item.setQuantity(100)
    Svc->>Repo: save(ProductDocument)
    Repo->>Mongo: Update document in petstore_products
    
    Svc-->>Ctrl: Updated ItemResponse (itemId="EST-1", quantity=100)
    Ctrl-->>UI: 200 OK (ItemResponse)
    UI-->>Supplier: Updated Stock: 100 units
```

---

### Flow 5: Shadow Reconciliation & Real-Time Parity Audit (`GET /api/v1/migration/parity?runAudit=true`)
**Core Principle**: Samples legacy relational state and modern MongoDB state side-by-side to continuously compute cutover readiness scores with zero downtime.

```mermaid
sequenceDiagram
    autonumber
    actor Ops as DevOps / Migration Engineer
    participant UI as OpsParityDashboardUI
    participant Ctrl as MigrationController (:8085)
    participant ParitySvc as ParityDashboardService
    participant Recon as ShadowReadComparator
    participant LegacyReader as LegacyOrderCursorReader
    participant LegacyDB as Legacy HSQLDB (:8000)
    participant Mongo as MongoDB (rs0 :27017)
    participant Logger as DiscrepancyLogger

    Ops->>UI: Click "Trigger Shadow Parity Audit"
    UI->>Ctrl: GET /api/v1/migration/parity?runAudit=true
    Ctrl->>ParitySvc: getDashboardSummary(true)
    ParitySvc->>Recon: compareAllEntities()
    
    rect rgb(240, 248, 255)
    Note over Recon, LegacyDB: Step A: Query Legacy State
    Recon->>LegacyReader: readAllLegacyOrders()
    LegacyReader->>LegacyDB: SELECT * FROM PURCHASEORDER (Read-Only JDBC)
    LegacyDB-->>LegacyReader: List of LegacyOrderRows (e.g. 100113, 100114)
    end

    rect rgb(245, 255, 245)
    Note over Recon, Mongo: Step B: Query Modern State
    Recon->>Mongo: mongoTemplate.findAll(OrderDocument.class)
    Mongo-->>Recon: List of OrderDocuments
    end

    rect rgb(255, 250, 240)
    Note over Recon, Logger: Step C: Field-by-Field Parity Comparison
    loop For each order entity
        Recon->>Recon: Compare total prices, item counts, statuses
        alt Modern-only order placed post-migration
            Recon->>Logger: recordComparison(Match: MODERN_ORDER)
        else Legacy and Mongo match perfectly
            Recon->>Logger: recordComparison(Match: 100% PARITY)
        else Discrepancy found
            Recon->>Logger: recordComparison(Drift: MISMATCH)
        end
    end
    end

    Logger->>Logger: Compute Parity = 100.0%, Status = CUTOVER_READY
    Logger-->>ParitySvc: DiscrepancyReport
    ParitySvc-->>Ctrl: ParityDashboardResponse
    Ctrl-->>UI: 200 OK (JSON: { parityPercentage: 100.0, status: "CUTOVER_READY" })
    UI-->>Ops: Green KPI: 100.0% Parity | 0 Active Drifts | Ready for Cutover
```

---

### Flow 6: Secondary Datastore Outage & Chaos DLQ Recovery
**Core Principle**: Proves fault isolation: when the secondary datastore fails, messages divert to `petstore.orders.dlq` without blocking legacy users or crashing the checkout thread.

```mermaid
sequenceDiagram
    autonumber
    actor Chaos as Chaos Test / Real Outage
    participant Mongo as MongoDB (Secondary Store)
    participant Kafka as Kafka (KRaft)
    participant Consumer as MongoDualWriteConsumer
    participant DLQConsumer as OrderDlqConsumer
    participant Metrics as MigrationParityMetrics

    Chaos->>Mongo: Simulate Outage (Container Paused / Unreachable)
    Kafka->>Consumer: Deliver OrderDualWriteEvent from topic petstore.orders.dualwrite
    
    Consumer->>Mongo: mongoTemplate.insert(order)
    Mongo--XConsumer: ConnectionException / Timeout! (Mongo is DOWN)
    
    Note over Consumer, Kafka: Spring DefaultErrorHandler intercepts failure
    Consumer->>Metrics: recordDualWriteFailure()
    Consumer->>Kafka: Route failed message to topic: petstore.orders.dlq
    
    Kafka->>DLQConsumer: Consume from petstore.orders.dlq
    DLQConsumer->>DLQConsumer: Log dead-letter event & safely isolate payload
    Note over DLQConsumer: Zero blast radius: Primary checkout is NOT blocked!

    Chaos->>Mongo: Outage Restored (Container Unpaused)
    Note over Mongo, DLQConsumer: System self-heals during next reconciliation audit!
```

---

## 3. Key Architectural Highlights & Interview Defenses

1. **Why Legacy Write-Back is Asynchronous via Kafka (Blast-Radius Defense)**:
   - Synchronously writing to the 2002 relational database would cause lock contention on `PURCHASEORDER` and `LINEITEM`, degrading the experience for active legacy users.
   - Decoupling with Kafka ensures the modern checkout latency remains sub-millisecond, while Kafka serves as an asynchronous shock absorber if two-way sync is ever activated.

2. **Why the Transactional Outbox Pattern is Required**:
   - Persisting `OrderDocument` and `OutboxDocument` in the same MongoDB transaction guarantees that an order cannot exist without an accompanying outbox event.
   - If Kafka is unreachable during checkout, the checkout still succeeds, and the background `OutboxRelayScheduler` replays pending messages upon broker recovery.

3. **Collision-Free Partitioned Numeric IDs**:
   - Modern IDs are 17-digit numeric strings in the range $\ge 1.78 \times 10^{16}$, completely disjoint from legacy 5-6 digit IDs ($< 10^7$).
   - Fits within signed 64-bit integer (`long` / `int64`) with an optimistic retry loop.
