# Modern Pet Store Platform - High-Level Design (HLD)

> **Modernization Context**: This document details the high-level architecture, deployment topology, network boundaries, and data flow pipelines of the **migrated modern cloud-native Pet Store platform**, powered by **Java 21 LTS**, **Spring Boot 3.3.x**, **MongoDB 7.0 (Replica Set `rs0`)**, **Apache Kafka (KRaft mode)**, and **React 18 + Vite + TypeScript**.

---

## 1. Executive System Architecture

The modernized system departs from the 2002 monolithic EJB architecture by adopting a **reactive, event-driven microservices architecture** governed by the **Strangler Fig Application Pattern** and the **Dual-Write & Shadow Reconciliation Pattern**:

```mermaid
flowchart TB
    subgraph ClientTier["Client & Presentation Tier"]
        SPA["Modern React 18 SPA (Vite + TypeScript)<br/>Port: 3000"]
        LEGACY_CLIENT["Legacy TomEE / Swing Client (Coexistence)<br/>Port: 8000 / 8080"]
    end

    subgraph GatewayTier["API Gateway & Reverse Proxy (Vite / Ingress)"]
        ProxyRouter["Vite Dev / Nginx Gateway Router"]
        RBAC["Role-Based Access Control (RBAC)<br/>ROLE_CUSTOMER | ROLE_ADMIN | ROLE_SUPPLIER | ROLE_ENGINEER"]
    end

    subgraph MicroservicesTier["Modern Microservices Tier (Java 21 LTS / Spring Boot 3.3 / Project Loom)"]
        CATALOG["petstore-catalog-service<br/>Port: 8081<br/>Multilingual Catalog & Warehouse Stock"]
        ORDER["petstore-order-service<br/>Port: 8082<br/>Checkout & Order Approval Lifecycle"]
        MIGRATION["petstore-migration-service<br/>Port: 8085<br/>Dual-Write Consumer, DLQ & Parity Auditor"]
    end

    subgraph StreamingTier["Distributed Event Streaming Mesh (Apache Kafka 3.7+ KRaft)"]
        KAFKA[("Kafka Cluster Broker<br/>Port: 9092")]
        T_DW["petstore.orders.dualwrite<br/>(Transactional Mirroring)"]
        T_APP["petstore.orders.approved<br/>(Domain Event Stream)"]
        T_DLQ["petstore.orders.dlq<br/>(Fault-Isolated Dead-Letter Queue)"]
        
        KAFKA --- T_DW
        KAFKA --- T_APP
        KAFKA --- T_DLQ
    end

    subgraph PersistenceTier["Modern Persistence Tier (Document Datastore)"]
        MONGO[("MongoDB 7.0 Community (Replica Set rs0)<br/>Port: 27017")]
        C_ORDERS[("Collection: petstore_orders")]
        C_PRODUCTS[("Collection: petstore_products")]
        C_CATEGORIES[("Collection: categories")]
        C_PARITY[("Collection: parity_audits")]

        MONGO --- C_ORDERS
        MONGO --- C_PRODUCTS
        MONGO --- C_CATEGORIES
        MONGO --- C_PARITY
    end

    subgraph LegacyCoexistence["Legacy System of Record (Preserved Coexistence)"]
        TOMEE["Apache TomEE 1.7.5 Container<br/>petstore-baseline"]
        LEGACY_DB[("Legacy HSQLDB System of Record<br/>petstoredb.script")]
        TOMEE --> LEGACY_DB
    end

    %% Client and Gateway Routing
    SPA --> ProxyRouter
    ProxyRouter --> RBAC
    RBAC -->|"/api/v1/categories/**, /api/v1/items/**"| CATALOG
    RBAC -->|"/api/v1/orders/**"| ORDER
    RBAC -->|"/api/v1/migration/**"| MIGRATION

    %% Backend Persistence & Streaming Flows
    CATALOG -->|"Sub-millisecond Document Lookups & Atomic Stock Updates"| MONGO
    ORDER -->|"Transactional Write (ACID Document Update)"| C_ORDERS
    ORDER -->|"Publish Dual-Write Event"| T_DW
    ORDER -->|"Publish Domain Events"| T_APP

    T_DW -->|"Async Consumer Subscription"| MIGRATION
    MIGRATION -->|"Asynchronous Document Reconcile"| C_ORDERS
    MIGRATION -->|"Error Backoff & Dead-Letter Isolation"| T_DLQ

    MIGRATION -.->|"Non-Blocking Shadow Read Audit (JDBC)"| LEGACY_DB
    MIGRATION -.->|"Compare & Audit Parity"| C_ORDERS

    LEGACY_CLIENT --> TOMEE
```

---

## 2. Deployment Topology Diagram

The platform is containerized using Docker and Docker Compose, establishing clean process boundaries, port assignments, and shared bridge networking:

```mermaid
graph TD
    subgraph DockerHost["Docker Container Infrastructure (bridge: petstore-network)"]
        
        subgraph MongoContainer["petstore-mongo (mongo:7.0)"]
            MongoEngine["mongod --replSet rs0"]
            MongoPort["Port: 27017"]
            MongoVol["Volume: mongo_data -> /data/db"]
        end

        subgraph KafkaContainer["petstore-kafka (confluentinc/cp-kafka:7.6.1)"]
            KafkaKRaft["Kafka Broker (KRaft Controller)"]
            KafkaPortInt["Internal: kafka:29092"]
            KafkaPortExt["External: localhost:9092"]
        end

        subgraph KafkaUIContainer["petstore-kafka-ui (provectuslabs/kafka-ui:latest)"]
            KafkaUIApp["Kafka Web GUI"]
            KafkaUIPort["Port: 8087"]
        end

        subgraph MongoExpressContainer["petstore-mongo-express (mongo-express:1.0.2)"]
            MEApp["Mongo Express Web GUI"]
            MEPort["Port: 8086"]
        end

        subgraph LegacyTomEEContainer["petstore-baseline (Ubuntu 20.04 + OpenJDK 8 + TomEE)"]
            TomEEApp["TomEE 1.7.5 Plus (J2EE 1.3 Baseline)"]
            TomEEPort["Port: 8000 / 8088"]
        end
    end

    subgraph HostSystem["Host Operating System (macOS / Linux)"]
        subgraph BackendServices["Spring Boot 3.3 Microservices (Java 21 LTS)"]
            CatalogApp["petstore-catalog-service<br/>PID / Port: 8081"]
            OrderApp["petstore-order-service<br/>PID / Port: 8082"]
            MigrationApp["petstore-migration-service<br/>PID / Port: 8085"]
        end

        subgraph FrontendSPA["Vite Frontend Engine (Node 20+)"]
            ViteDev["Vite Dev Server (TypeScript + React 18)<br/>Port: 3000"]
        end
    end

    %% Network links
    ViteDev -->|"Proxy /api/v1/categories"| CatalogApp
    ViteDev -->|"Proxy /api/v1/orders"| OrderApp
    ViteDev -->|"Proxy /api/v1/migration"| MigrationApp

    CatalogApp -->|"mongodb://localhost:27017/petstore?replicaSet=rs0"| MongoPort
    OrderApp -->|"mongodb://localhost:27017/petstore?replicaSet=rs0"| MongoPort
    OrderApp -->|"localhost:9092 (Kafka Producer)"| KafkaPortExt

    MigrationApp -->|"localhost:9092 (Kafka Consumer)"| KafkaPortExt
    MigrationApp -->|"mongodb://localhost:27017/petstore?replicaSet=rs0"| MongoPort
    MigrationApp -.->|"jdbc:hsqldb:file:.../petstoredb"| LegacyTomEEContainer

    KafkaUIApp -->|"kafka:29092"| KafkaPortInt
    MongoExpressContainer -->|"petstore-mongo:27017"| MongoPort
```

### 2.1 Service Directory & Port Mapping
| Service / Container | Base Image / Technology | Host Port | Internal Network Port | Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **`petstore-frontend`** | React 18, Vite 5.4, TypeScript | `3000` | N/A | Single-Page Application: Storefront, Account, Admin, Supplier, Ops. |
| **`petstore-catalog-service`** | Spring Boot 3.3, Java 21 LTS | `8081` | N/A | REST endpoints for Categories, Products, Items, and Supplier inventory updates. |
| **`petstore-order-service`** | Spring Boot 3.3, Java 21 LTS | `8082` | N/A | Customer checkout, order approval lifecycle, dual-write Kafka producer. |
| **`petstore-migration-service`**| Spring Boot 3.3, Java 21 LTS | `8085` | N/A | Dual-write Kafka consumer, DLQ error handler, live shadow parity auditor. |
| **`petstore-mongo`** | MongoDB 7.0 Community (rs0) | `27017` | `27017` | High-throughput document persistence (`petstore_orders`, `petstore_products`). |
| **`petstore-kafka`** | Confluent Kafka 7.6.1 (KRaft) | `9092` | `29092` | Distributed event streaming bus (`orders.dualwrite`, `orders.approved`, `orders.dlq`). |
| **`petstore-kafka-ui`** | Provectus Labs Kafka-UI | `8087` | `8080` | Web administration for Kafka topics, offsets, and consumer group lags. |
| **`petstore-mongo-express`**| Mongo Express 1.0.2 | `8086` | `8081` | Web database administration interface for MongoDB collections. |
| **`petstore-baseline`** | Apache TomEE Plus 1.7.5 | `8000` | `8088` | Preserved 2002 J2EE baseline container executing legacy EAR archives. |

---

## 3. Network Topology & Security Boundary Diagram

```mermaid
flowchart TB
    subgraph PublicInternet["Public Internet Zone"]
        CustomerUser["Shopper Client (Chrome / Safari / Firefox)"]
        AdminUser["Store Administrator (Admin Console)"]
        SupplierUser["Wholesale Supplier (Supplier Portal)"]
        EngineerUser["Site Reliability Engineer (Ops Dashboard)"]
    end

    subgraph SecurityPerimeter["Edge Security & Ingress Tier"]
        ReverseProxy["Reverse Proxy / Vite Ingress (Port 3000)"]
        CORSPolicy["CORS Filter (Allowed Origins: localhost:3000)"]
        RBACFilter["RBAC Authorization Context"]
    end

    subgraph ServiceNetwork["Microservices Private Network Zone"]
        CatalogAPI["Catalog Microservice (Port 8081)"]
        OrderAPI["Order Microservice (Port 8082)"]
        MigrationAPI["Migration Microservice (Port 8085)"]
    end

    subgraph DataNetwork["Secure Datastore & Streaming Zone"]
        KafkaBroker["Apache Kafka Broker (Port 9092 / 29092)"]
        MongoCluster["MongoDB Replica Set rs0 (Port 27017)"]
        LegacyDB["HSQLDB File Datastore (Port 8000 Container)"]
    end

    %% Inbound connections
    CustomerUser -->|"HTTP/HTTPS GET/POST /"| ReverseProxy
    AdminUser -->|"HTTP/HTTPS GET/PUT /admin"| ReverseProxy
    SupplierUser -->|"HTTP/HTTPS GET/PUT /supplier"| ReverseProxy
    EngineerUser -->|"HTTP/HTTPS GET /ops"| ReverseProxy

    ReverseProxy --> CORSPolicy
    CORSPolicy --> RBACFilter

    RBACFilter -->|"ROLE_CUSTOMER, Public"| CatalogAPI
    RBACFilter -->|"ROLE_CUSTOMER, ROLE_ADMIN"| OrderAPI
    RBACFilter -->|"ROLE_SUPPLIER"| CatalogAPI
    RBACFilter -->|"ROLE_ENGINEER"| MigrationAPI

    CatalogAPI --> MongoCluster
    OrderAPI --> MongoCluster
    OrderAPI --> KafkaBroker
    MigrationAPI --> KafkaBroker
    MigrationAPI --> MongoCluster
    MigrationAPI -.-> LegacyDB
```

---

## 4. Data Flow Diagrams (DFD)

### 4.1 DFD Level 0: System Context Diagram

```mermaid
flowchart LR
    Shopper["Shopper / Customer"]
    Admin["Store Administrator"]
    Supplier["Wholesale Supplier"]
    SRE["Site Reliability Engineer / Ops"]

    subgraph ModernPetStore["Modern Pet Store Platform (Spring Boot 3.3 + React 18 + Kafka + Mongo)"]
        CorePlatform["Modern Reactive Core"]
    end

    %% Inbound / Outbound Flows
    Shopper -->|"1. Multilingual Browse & Search"| CorePlatform
    Shopper -->|"2. Language-Preserving Cart Actions"| CorePlatform
    Shopper -->|"3. Place Order (POST /api/v1/orders)"| CorePlatform
    CorePlatform -->|"4. Real-Time Order Confirmation"| Shopper

    Admin -->|"5. Review Pending Approvals & Sales Analytics"| CorePlatform
    Admin -->|"6. Approve / Reject Order (PUT /api/v1/orders/:id/status)"| CorePlatform

    Supplier -->|"7. View Warehouse Inventory (28 SKUs)"| CorePlatform
    Supplier -->|"8. Adjust Stock Levels (PUT /api/v1/items/:id/inventory)"| CorePlatform

    SRE -->|"9. Monitor Dual-Write & Telemetry"| CorePlatform
    SRE -->|"10. Trigger Shadow Parity Audit & Compass Connect"| CorePlatform
```

### 4.2 DFD Level 1: Subsystem Data Flow

```mermaid
flowchart TB
    Shopper["Shopper"]
    Admin["Administrator"]
    Supplier["Supplier"]
    SRE["SRE / Ops"]

    subgraph Frontend["Modern React 18 SPA Layer"]
        F1["1.0 Storefront & Multilingual Cart"]
        F2["2.0 Customer Account & Orders"]
        F3["3.0 Admin Dashboard (Swing 2.0)"]
        F4["4.0 Supplier Inventory Portal"]
        F5["5.0 Migration & Parity Monitor"]
    end

    subgraph BackendServices["Spring Boot Microservices Tier"]
        S1["6.0 Catalog Aggregation Service (:8081)"]
        S2["7.0 Order Processing & Lifecycle (:8082)"]
        S3["8.0 Dual-Write Publisher (:8082)"]
        S4["9.0 Dual-Write Consumer (:8085)"]
        S5["10.0 Shadow Reconciliation Auditor (:8085)"]
        S6["11.0 Live Database Diagnostics (:8085)"]
    end

    subgraph EventStreaming["Apache Kafka KRaft Event Mesh"]
        Q_DW[("petstore.orders.dualwrite")]
        Q_APP[("petstore.orders.approved")]
        Q_DLQ[("petstore.orders.dlq")]
    end

    subgraph Persistence["Datastore Tier"]
        DB_MONGO[("MongoDB rs0 (petstore_orders, petstore_products)")]
        DB_LEGACY[("Legacy Relational DB (petstoredb)")]
    end

    %% User Interactions
    Shopper -->|"Browse / Cart Actions"| F1
    F1 -->|"GET /api/v1/categories, /products"| S1
    S1 -->|"Read Aggregates"| DB_MONGO

    Shopper -->|"Submit Order"| F1
    F1 -->|"POST /api/v1/orders"| S2
    S2 -->|"Save OrderDocument"| DB_MONGO
    S2 -->|"Emit Dual-Write Event"| S3
    S3 -->|"Publish Event"| Q_DW

    Q_DW -->|"Subscribe & Consume"| S4
    S4 -->|"Async Mirror Document"| DB_MONGO
    S4 -.->|"Transient Failure / Error"| Q_DLQ

    Admin -->|"Review & Approve"| F3
    F3 -->|"PUT /api/v1/orders/:id/status"| S2
    S2 -->|"Update Status (APPROVED)"| DB_MONGO
    S2 -->|"Emit Event"| Q_APP

    Supplier -->|"Manage Warehouse Stock"| F4
    F4 -->|"PUT /api/v1/items/:id/inventory"| S1
    S1 -->|"Atomic Stock Update"| DB_MONGO
    DB_MONGO -.->|"Immediately Reflected in Search"| S1

    SRE -->|"Inspect Parity & Diagnostics"| F5
    F5 -->|"GET /api/v1/migration/parity"| S5
    S5 -->|"Shadow Read"| DB_LEGACY
    S5 -->|"Shadow Read"| DB_MONGO
    S5 -->|"Compute Parity %"| F5
    F5 -->|"GET /api/v1/migration/diagnostics/mongo"| S6
    S6 -->|"Fetch Topology & Metrics"| DB_MONGO
```
