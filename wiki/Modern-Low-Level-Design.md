# Modern Pet Store Platform - Low-Level Design (LLD)

> **Modernization Context**: This document details the micro-architectural design of the modernized Spring Boot 3.3.x, React 18, Apache Kafka, and MongoDB architecture. It covers class hierarchies, sequence execution flows, MongoDB BSON schemas, state machine transitions, and algorithmic flowcharts.

---

## 1. Microservice Class & Component Diagrams

### 1.1 Catalog Microservice (`petstore-catalog-service`)

Manages the pet catalog aggregates, localized multilingual descriptions, and real-time inventory levels:

```mermaid
classDiagram
    class CategoryController {
        -CategoryRepository categoryRepo
        -ProductRepository productRepo
        +getAllCategories() ResponseEntity~List~CategoryDocument~~
        +getCategoryById(String) ResponseEntity~CategoryDocument~
        +getProductsByCategory(String) ResponseEntity~List~ProductDocument~~
    }

    class ItemController {
        -ProductRepository productRepo
        -MongoTemplate mongoTemplate
        +getAllItems() ResponseEntity~List~ItemResponse~~
        +getItemById(String) ResponseEntity~ItemResponse~
        +updateInventory(String, InventoryUpdateRequest) ResponseEntity~ItemResponse~
    }

    class CategoryRepository {
        <<interface>>
        +findAll() List~CategoryDocument~
        +findById(String) Optional~CategoryDocument~
    }

    class ProductRepository {
        <<interface>>
        +findByCategoryId(String) List~ProductDocument~
        +findByItemsItemId(String) Optional~ProductDocument~
    }

    class CategoryDocument {
        -String id
        -String categoryId
        -Map~String, String~ names
        -Map~String, String~ descriptions
        -String image
        +getName(String) String
        +getDescription(String) String
    }

    class ProductDocument {
        -String id
        -String productId
        -String categoryId
        -Map~String, String~ names
        -Map~String, String~ descriptions
        -String image
        -List~ItemDocument~ items
    }

    class ItemDocument {
        -String itemId
        -String productId
        -BigDecimal listPrice
        -BigDecimal unitCost
        -String attribute1
        -String image
        -int quantity
    }

    CategoryController --> CategoryRepository
    CategoryController --> ProductRepository
    ItemController --> ProductRepository
    ProductRepository --> ProductDocument
    CategoryRepository --> CategoryDocument
    ProductDocument "1" *-- "many" ItemDocument
```

### 1.2 Order Microservice (`petstore-order-service`)

Coordinates customer checkout, atomic MongoDB persistence, order approvals, and Kafka event publishing:

```mermaid
classDiagram
    class OrderController {
        -OrderService orderService
        +createOrder(OrderRequest) ResponseEntity~OrderResponse~
        +getOrderById(String) ResponseEntity~OrderResponse~
        +getUserOrders(String) ResponseEntity~List~OrderResponse~~
        +getAdminSummary() ResponseEntity~AdminSummaryResponse~
        +updateOrderStatus(String, StatusUpdateRequest) ResponseEntity~OrderResponse~
    }

    class OrderService {
        -OrderRepository orderRepo
        -DualWritePublisher dualWritePublisher
        -OrderEventProducer orderEventProducer
        -OutboxRepository outboxRepo
        -ObjectMapper objectMapper
        +placeOrder(CreateOrderRequest) OrderDocument
        +createOrder(OrderDocument) OrderDocument
        +updateOrderStatus(String, OrderStatus) OrderDocument
        +getAdminAnalytics() AdminAnalyticsResponse
    }

    class OutboxDocument {
        -String id
        -String aggregateType
        -String aggregateId
        -String eventType
        -String topic
        -String payload
        -String status
        -int retryCount
        -Instant createdAt
        -Instant processedAt
    }

    class OutboxRelayScheduler {
        -OutboxRepository outboxRepo
        -KafkaTemplate kafkaTemplate
        -ObjectMapper objectMapper
        +relayPendingEvents()
    }

    class DualWritePublisher {
        -KafkaTemplate~String, OrderDualWriteEvent~ kafkaTemplate
        -MigrationParityMetrics metrics
        +publishOrderCreated(OrderDocument)
        +publishOrderStatusUpdated(OrderDocument, OrderStatus, OrderStatus)
    }

    class OrderRepository {
        <<interface>>
        +findByUserId(String) List~OrderDocument~
        +findByStatus(OrderStatus) List~OrderDocument~
    }

    class OutboxRepository {
        <<interface>>
        +findByStatusOrderByCreatedAtAsc(String, Pageable) List~OutboxDocument~
    }

    class OrderDocument {
        -String id
        -Long version
        -String userId
        -OrderStatus status
        -BigDecimal totalPrice
        -Instant orderDate
        -List~LineItemDocument~ lineItems
        -AddressDocument shipping
        -PaymentDocument payment
        -String locale
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        APPROVED
        DENIED
        COMPLETED
        CANCELLED
    }

    class LineItemDocument {
        -String itemId
        -String productId
        -String description
        -String attribute1
        -String image
        -int quantity
        -BigDecimal unitPrice
        -BigDecimal total
    }

    OrderController --> OrderService
    OrderService --> OrderRepository
    OrderService --> DualWritePublisher
    OrderRepository --> OrderDocument
    OrderDocument --> OrderStatus
    OrderDocument "1" *-- "many" LineItemDocument
```

### 1.3 Migration & Parity Microservice (`petstore-migration-service`)

Consumes dual-write events from Kafka, manages Dead-Letter Queues (DLQ), and executes real-time shadow audits:

```mermaid
classDiagram
    class MongoDualWriteConsumer {
        -OrderRepository modernOrderRepo
        -KafkaTemplate dlqTemplate
        +consumeDualWriteOrder(DualWriteOrderEvent, Acknowledgment)
    }

    class ShadowReconciliationAuditor {
        -JdbcTemplate legacyJdbcTemplate
        -MongoTemplate modernMongoTemplate
        +executeAudit() ParityReport
        +auditOrderById(String) OrderComparisonResult
    }

    class MongoDiagnosticsService {
        -MongoDatabase mongoDatabase
        +getDatabaseDiagnostics() MongoDiagnosticsReport
    }

    class ParityReport {
        -double parityPercentage
        -String status
        -int totalComparisons
        -int totalMatches
        -int totalDrifts
        -Instant executionTimestamp
    }

    class MigrationController {
        -ShadowReconciliationAuditor auditor
        -MongoDiagnosticsService diagnosticsService
        +getParityStatus(boolean) ResponseEntity~ParityReport~
        +getMongoDiagnostics() ResponseEntity~MongoDiagnosticsReport~
    }

    MigrationController --> ShadowReconciliationAuditor
    MigrationController --> MongoDiagnosticsService
    ShadowReconciliationAuditor --> ParityReport
```

### 1.4 Modern React 18 Frontend Architecture

```mermaid
classDiagram
    class App {
        -AuthProvider
        -CartProvider
        -AppRoutes()
    }

    class Navbar {
        -LanguageSelector
        -CartDrawerTrigger
        -UserMenu
        -NavigationLinks
    }

    class Storefront {
        -CategoryTabs
        -ProductGrid
        -ItemModal
        +handleAddToCart(Item, language)
    }

    class CartDrawer {
        -CartItemList
        -SubtotalCalculator
        -CheckoutButton
        +renderItemsWithOriginalLanguage()
    }

    class AdminDashboard {
        -KPICards
        -PendingApprovalsTable
        -SalesCategoryCharts (Donut & Bar)
        -CohortMetrics
        +handleApproveOrder(orderId)
    }

    class SupplierPortal {
        -InventoryTable
        -StockStatusBadges
        -QuickRestockModal
        +handleStockUpdate(itemId, quantity)
    }

    class OpsMonitor {
        -ReplicationHealthCard
        -DLQAlertPanel
        -ShadowAuditRunner
        -CompassConnectWidget
        +triggerAudit()
    }

    App --> Navbar
    App --> Storefront
    App --> CartDrawer
    App --> AdminDashboard
    App --> SupplierPortal
    App --> OpsMonitor
```

---

## 2. Sequence Execution Diagrams

### 2.1 Customer Checkout & Kafka Dual-Write Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Shopper Client
    participant UI as React 18 SPA (Vite)
    participant Gateway as Reverse Proxy (:3000)
    participant OrderCtrl as OrderController (:8082)
    participant OrderSvc as OrderService (:8082)
    participant Mongo as MongoDB rs0 (:27017)
    participant KafkaPub as DualWritePublisher (:8082)
    participant Kafka as Apache Kafka (:9092)
    participant Consumer as MongoDualWriteConsumer (:8085)

    Customer->>UI: Click "Place Order" (Submit Checkout Modal)
    UI->>Gateway: POST /api/v1/orders (Payload with LineItems & Locale)
    Gateway->>OrderCtrl: Forward request
    OrderCtrl->>OrderSvc: createOrder(OrderRequest)
    OrderSvc->>OrderSvc: Generate monotonic orderId (#1788691...)
    OrderSvc->>Mongo: save(OrderDocument - status=PENDING)
    Mongo-->>OrderSvc: Confirmed saved document
    OrderSvc->>KafkaPub: publishOrderCreated(OrderDocument)
    KafkaPub->>Kafka: send("petstore.orders.dualwrite", key, event)
    Kafka-->>KafkaPub: RecordMetadata (TopicOffset, Partition)
    OrderSvc-->>OrderCtrl: OrderResponse(id, total, PENDING)
    OrderCtrl-->>Gateway: HTTP 201 Created
    Gateway-->>UI: JSON Order Confirmation
    UI-->>Customer: Render Order Confirmation Modal & Empty Cart

    %% Asynchronous Event Pipeline
    Note over Kafka,Consumer: Asynchronous Secondary Synchronization
    Kafka->>Consumer: onMessage(DualWriteOrderEvent)
    Consumer->>Consumer: Validate payload structure
    Consumer->>Mongo: Reconcile / Index secondary document
    Consumer->>Kafka: ack.acknowledge()
```

### 2.2 Admin Order Approval & Event Emission Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Store Administrator
    participant UI as Admin Dashboard (React 18)
    participant OrderCtrl as OrderController (:8082)
    participant OrderSvc as OrderService (:8082)
    participant Mongo as MongoDB rs0 (:27017)
    participant Kafka as Apache Kafka (:9092)

    Admin->>UI: Click "Approve Order #1788691..."
    UI->>OrderCtrl: PUT /api/v1/orders/1788691.../status {"status": "APPROVED"}
    OrderCtrl->>OrderSvc: updateOrderStatus(orderId, APPROVED)
    OrderSvc->>Mongo: findByIdAndSetStatus(orderId, APPROVED)
    Mongo-->>OrderSvc: Updated OrderDocument
    OrderSvc->>Kafka: send("petstore.orders.approved", ApprovedEvent)
    OrderSvc-->>OrderCtrl: Updated OrderResponse
    OrderCtrl-->>UI: HTTP 200 OK
    UI->>UI: Move order from Pending to Approved table
    UI->>UI: Re-render SVG Category Analytics and KPIs
    UI-->>Admin: Display "Order Approved" Toast Alert
```

### 2.3 Supplier Inventory Adjustment & Immediate Reflection Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Supplier as Wholesale Supplier
    actor Shopper as Browsing Shopper
    participant SupplierUI as Supplier Portal (React 18)
    participant ItemCtrl as ItemController (:8081)
    participant Mongo as MongoDB rs0 (:27017)
    participant ShopperUI as Storefront Catalog (React 18)

    Supplier->>SupplierUI: Adjust stock for EST-1 (Angelfish) to 10,360 units
    SupplierUI->>ItemCtrl: PUT /api/v1/items/EST-1/inventory {"quantity": 10360}
    ItemCtrl->>Mongo: updateOne({"items.itemId": "EST-1"}, {$set: {"items.$.quantity": 10360}})
    Mongo-->>ItemCtrl: UpdateResult (matched: 1, modified: 1)
    ItemCtrl-->>SupplierUI: HTTP 200 OK (Updated ItemResponse)
    SupplierUI-->>Supplier: Green "Stock Synchronized" Badge

    %% Immediate Reflection
    Shopper->>ShopperUI: Refresh or search "Angelfish"
    ShopperUI->>ItemCtrl: GET /api/v1/items/EST-1
    ItemCtrl->>Mongo: findOne({"items.itemId": "EST-1"})
    Mongo-->>ItemCtrl: Fresh Document (quantity = 10,360)
    ItemCtrl-->>ShopperUI: HTTP 200 OK
    ShopperUI-->>Shopper: Renders "In Stock: 10,360 units"
```

### 2.4 Chaos Outage, DLQ Isolation & Shadow Reconciliation Sequence

```mermaid
sequenceDiagram
    autonumber
    actor SRE as Chaos Experimenter
    participant Mongo as MongoDB Container (:27017)
    participant LegacyApp as Legacy Pet Store (:8000)
    participant Kafka as Apache Kafka (:9092)
    participant DLQConsumer as MongoDualWriteConsumer (:8085)
    participant Auditor as ShadowReconciliationAuditor (:8085)

    SRE->>Mongo: docker pause petstore-mongo (Simulate Outage)
    
    %% Resilience check
    SRE->>LegacyApp: GET /petstore/category.screen
    LegacyApp-->>SRE: HTTP 200 OK (Zero Blast Radius: Legacy Monolith Unaffected)

    %% DLQ Isolation
    Kafka->>DLQConsumer: Deliver Order Event
    DLQConsumer->>Mongo: Attempt Document Write
    Mongo--xDLQConsumer: Connection Timeout / Unavailable
    DLQConsumer->>DLQConsumer: Exhaust 3 Retry Attempts (Exponential Backoff)
    DLQConsumer->>Kafka: send("petstore.orders.dlq", PoisonPillEvent)
    DLQConsumer->>Kafka: ack.acknowledge() (Prevent Consumer Lag Blocker)

    %% Recovery & Reconciliation
    SRE->>Mongo: docker unpause petstore-mongo (Restore Health)
    SRE->>Auditor: Trigger Shadow Audit (GET /api/v1/migration/parity?runAudit=true)
    Auditor->>LegacyApp: SELECT FROM PURCHASEORDER (HSQLDB)
    Auditor->>Mongo: find() FROM petstore_orders
    Auditor->>Auditor: Compare All Field Attributes
    Auditor-->>SRE: Parity Report (100.0% Synchronized, Status: SYNCHRONIZED)
```

---

## 3. Document Schema & Entity-Relationship (ER) Diagrams

### 3.1 MongoDB Document Schemas

```mermaid
erDiagram
    CATEGORIES ||--o{ PRODUCTS : "contains"
    PRODUCTS ||--|{ ITEMS : "embeds"
    ORDERS ||--|{ LINE_ITEMS : "embeds"
    ORDERS ||--|| ADDRESS : "embeds shipping"
    ORDERS ||--|| PAYMENT : "embeds payment"
    PARITY_AUDITS ||--o{ AUDIT_DETAILS : "embeds"

    CATEGORIES {
        string _id PK
        string categoryId
        map names "en_US, zh_CN, ja_JP"
        map descriptions "en_US, zh_CN, ja_JP"
        string image
    }

    PRODUCTS {
        string _id PK
        string productId
        string categoryId FK
        map names "en_US, zh_CN, ja_JP"
        map descriptions "en_US, zh_CN, ja_JP"
        string image
        array items "Embedded ItemDocument array"
    }

    ITEMS {
        string itemId PK
        string productId FK
        decimal listPrice
        decimal unitCost
        string attribute1
        string image
        int quantity "Real-time warehouse stock"
    }

    ORDERS {
        string _id PK
        string orderId
        string userId
        string status "PENDING, APPROVED, REJECTED, COMPLETED"
        decimal totalAmount
        string locale
        date orderDate
        array lineItems "Embedded LineItemDocument array"
        object shippingAddress "Embedded AddressDocument"
        object payment "Embedded PaymentDocument"
    }

    PARITY_AUDITS {
        string _id PK
        double parityPercentage
        string status "SYNCHRONIZED, DRIFT_DETECTED"
        int totalComparisons
        int totalMatches
        int totalDrifts
        date timestamp
    }
```

### 3.2 Schema Transformation: Relational 3NF vs. Modern Document Aggregates

| Legacy Relational Tables (12+ Tables) | Modern MongoDB Collections (3 Aggregates) | Modern Design Advantage |
| :--- | :--- | :--- |
| `CATEGORY` + `CATEGORY_DETAILS` | `categories` Collection | Multilingual map eliminates SQL JOINs across locale tables. |
| `PRODUCT` + `PRODUCT_DETAILS` + `ITEM` + `ITEM_DETAILS` + `INVENTORY` | `petstore_products` Collection (Embedded `items` array) | Single atomic query fetches product with all SKU items and stock. |
| `PURCHASEORDER` + `LINEITEM` + `CONTACTINFO` + `ADDRESS` + `CREDITCARD` | `petstore_orders` Collection (Embedded `lineItems`, `shipping`, `payment`) | Entire order tree persisted and retrieved in one atomic BSON read/write. |

---

## 4. State Machine Diagrams

### 4.1 Modern Order Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : Customer Checkout (POST /api/v1/orders)
    PENDING --> APPROVED : Admin Approves (PUT /api/v1/orders/:id/status)
    PENDING --> REJECTED : Admin Rejects / Fraud Filter
    APPROVED --> PROCESSING : Sent to Warehouse Fulfillment
    PROCESSING --> COMPLETED : Dispatched & Shipped
    PENDING --> CANCELLED : Customer Cancels
    
    state DLQ_HANDLING {
        [*] --> DLQ_RECORDED : Secondary Persistence Failed
        DLQ_RECORDED --> RETRY_SUCCESS : Background Reconciler Retries
        DLQ_RECORDED --> MANUAL_INVESTIGATION : Alert Escalated
    }

    REJECTED --> [*]
    COMPLETED --> [*]
    CANCELLED --> [*]
```

### 4.2 Supplier Inventory Stock Level State Machine

```mermaid
stateDiagram-v2
    [*] --> IN_STOCK : Stock > 10 units (Normal Operation)
    IN_STOCK --> LOW_STOCK : Stock between 1 and 10 units (Warning Badge)
    LOW_STOCK --> OUT_OF_STOCK : Stock reaches 0 units (Disabled Add-to-Cart)
    OUT_OF_STOCK --> IN_STOCK : Supplier Restocks (PUT /api/v1/items/:id/inventory)
    LOW_STOCK --> IN_STOCK : Supplier Restocks
```

### 4.3 Shadow Reconciliation Parity State Machine

```mermaid
stateDiagram-v2
    [*] --> SYNCHRONIZED : Parity == 100.0% (Zero Drifts)
    SYNCHRONIZED --> DRIFT_DETECTED : Field Discrepancy Found in Legacy vs Mongo
    DRIFT_DETECTED --> RECONCILING : Asynchronous Auto-Heal Triggered
    RECONCILING --> SYNCHRONIZED : Discrepancy Resolved
    DRIFT_DETECTED --> SRE_ALERTED : Unresolved Drift > Threshold
```

---

## 5. Algorithmic Flowcharts & Logic

### 5.1 Asynchronous Dual-Write & Non-Blocking Resilience Pipeline

```mermaid
flowchart TD
    Start(["Inbound Order Checkout Request"]) --> SaveMongo["Save Order to MongoDB rs0 (Primary Store)"]
    SaveMongo --> CheckSave{"Mongo Write Successful?"}
    
    CheckSave -->|No| ReturnError(["Return HTTP 500 Internal Error"])
    CheckSave -->|Yes| PublishKafka["Publish Dual-Write Event to Kafka (petstore.orders.dualwrite)"]
    PublishKafka --> ReturnSuccess(["Return HTTP 201 Created to Customer"])
    
    %% Asynchronous Consumer Branch
    PublishKafka -.-> AsyncConsume["Kafka Consumer Receives Event"]
    AsyncConsume --> AttemptSecondary["Mirror Document / Index in Analytics Cache"]
    AttemptSecondary --> CheckSecondary{"Write Successful?"}
    
    CheckSecondary -->|Yes| AckMsg["Commit Kafka Offset (ack.acknowledge())"]
    CheckSecondary -->|No| RetryBackoff{"Retry Count < 3?"}
    
    RetryBackoff -->|Yes| WaitExp["Exponential Backoff Sleep (1s, 2s, 4s)"]
    WaitExp --> AttemptSecondary
    
    RetryBackoff -->|No| RouteDLQ["Route Event to petstore.orders.dlq"]
    RouteDLQ --> FireAlert["Emit Prometheus Metric / On-Call SRE Alert"]
    FireAlert --> AckMsg
```

### 5.2 Multilingual Cart Language Preservation Algorithm

Ensures that when a shopper changes the storefront language, items previously added to the cart preserve the exact localized name and attributes from the time of addition:

```mermaid
flowchart TD
    ItemAdd(["Customer clicks 'Add to Cart'"]) --> ReadCurrentLocale["Read Active Storefront Locale (e.g., zh_CN)"]
    ReadCurrentLocale --> ResolveStrings["Resolve localized name: product.names[locale]"]
    ResolveStrings --> ResolveAttrs["Resolve localized attribute: item.attribute1[locale]"]
    ResolveAttrs --> BuildCartItem["Construct CartItem with immutable captured name & captured locale"]
    BuildCartItem --> SaveLocalCart["Store in LocalStorage / State: cart.items.push(CartItem)"]
    
    SaveLocalCart --> SwitchLocale(["User changes Storefront language (e.g., en_US)"])
    SwitchLocale --> ReRenderStore["Re-render Storefront in English"]
    SwitchLocale --> ReRenderCart["Re-render Cart Drawer"]
    ReRenderCart --> CheckItemLocale["Check CartItem.capturedLanguage"]
    CheckItemLocale --> DisplayItem(["Display original Chinese text in Cart item without overriding"])
```
