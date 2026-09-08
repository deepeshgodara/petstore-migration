# Service Catalog & Technical Specifications

This document defines the interface specifications, runtime configurations, network ports, health endpoints, and database bindings for all services in the Pet Store modern ecosystem.

---

## 1. `petstore-catalog-service` (Port 8081)

- **Description**: Manages multilingual product categories, products, and inventory SKU items. Provides atomic inventory updates for suppliers.
- **Runtime**: Java 21 LTS, Spring Boot 3.3.3 (Project Loom Virtual Threads enabled).
- **Configuration**: `petstore-modern/petstore-catalog-service/src/main/resources/application.yml`
- **Database Binding**: MongoDB 7.0 (`mongodb://localhost:27017/petstore?replicaSet=rs0`), Collection: `petstore_products`

### REST Endpoints
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/categories?locale={locale}` | List all pet categories with localized names and descriptions | Public |
| `GET` | `/api/v1/categories/{id}?locale={locale}` | Retrieve category by identifier | Public |
| `GET` | `/api/v1/products?categoryId={cat}&query={q}&locale={locale}` | Search / filter products with child items | Public |
| `GET` | `/api/v1/products/{id}?locale={locale}` | Retrieve product with inventory SKUs | Public |
| `GET` | `/api/v1/items?locale={locale}` | List all inventory SKUs across categories | Public / Supplier |
| `GET` | `/api/v1/items/{itemId}?locale={locale}` | Retrieve single inventory SKU details | Public / Supplier |
| `PUT` | `/api/v1/items/{itemId}/inventory?locale={locale}` | Update warehouse stock quantity (Atomic MongoDB update) | `ROLE_SUPPLIER`, `ROLE_ADMIN` |
| `GET` | `/actuator/health` | Spring Boot actuator health & liveness probe | Internal / Ops |

---

## 2. `petstore-order-service` (Port 8082)

- **Description**: Manages customer checkout, user account registration & BCrypt authentication, order lifecycle transitions (`PENDING`, `APPROVED`, `COMPLETED`, `DENIED`, `CANCELLED`), transactional outbox relay publishing, and administrative analytics.
- **Runtime**: Java 21 LTS, Spring Boot 3.3.3.
- **Configuration**: `petstore-modern/petstore-order-service/src/main/resources/application.yml`
- **Database Binding**: MongoDB 7.0 (`mongodb://localhost:27017/petstore?replicaSet=rs0`), Collections: `petstore_orders`, `petstore_outbox`, `petstore_users`
- **Key Architectural Features**:
  - **Multi-Document ACID Transactions**: Backed by `MongoTransactionManager` on replica set `rs0`.
  - **Optimistic Concurrency Control**: `@Version private Long version;` on `OrderDocument` preventing race conditions via CAS.
  - **Transactional Outbox Relay**: `OutboxRelayScheduler` polls `petstore_outbox` every 500ms and guarantees zero-data-loss publishing to Kafka.
  - **Dynamic Feature Toggle**: `migration.dualwrite.enabled` kill switch controllable at runtime via Spring Actuator `/actuator/refresh`.
  - **Password Security**: Salted `BCryptPasswordEncoder` with lazy upgrade on legacy user login.
- **Kafka Integration**: Producer to `petstore.orders.created`, `petstore.orders.approved`, `petstore.orders.completed`, `petstore.orders.dualwrite`.

### REST Endpoints
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/orders` | Place customer purchase order (ACID document + outbox persist) | Public / Customer |
| `GET` | `/api/v1/orders/{orderId}` | Retrieve order document by ID | Customer / Admin |
| `GET` | `/api/v1/orders?userId={user}&status={st}` | Query orders filtered by user ID or lifecycle status | Customer / Admin |
| `PUT` / `PATCH` | `/api/v1/orders/{orderId}/status` | Transition order status (`APPROVED`, `COMPLETED`, `DENIED`) | `ROLE_ADMIN` |
| `GET` | `/api/v1/orders/admin/summary` | Aggregate order count, revenue, and status breakdown | `ROLE_ADMIN` |
| `GET` | `/api/v1/orders/admin/analytics` | Category sales metrics, unique/returning customer counts, AOV | `ROLE_ADMIN` |
| `POST` | `/api/v1/users/register` | Register new customer account with BCrypt password hashing | Public |
| `POST` | `/api/v1/users/login` | Authenticate user with legacy plain/SHA-1 validation & lazy BCrypt upgrade | Public |
| `GET` | `/api/v1/users/{username}` | Retrieve user profile details by username | Customer / Admin |
| `GET` | `/api/v1/users` | List registered customer profiles | `ROLE_ADMIN` |
| `GET` | `/actuator/health` | Service health status and MongoDB replica ping | Internal / Ops |
| `POST` | `/actuator/refresh` | Dynamically refresh runtime configurations (e.g. dual-write kill switch) | Internal / Ops |

---

## 3. `petstore-migration-service` (Port 8085)

- **Description**: Historical data migration worker, dual-write asynchronous consumer, reverse write-back consumer for live legacy sync, shadow read reconciler, and live MongoDB engine telemetry provider.
- **Runtime**: Java 21 LTS, Spring Boot 3.3.3.
- **Configuration**: `petstore-modern/petstore-migration-service/src/main/resources/application.yml`
- **Database Bindings**:
  - Legacy HSQLDB via JDBC: `jdbc:hsqldb:hsql://localhost:9001/petstore` (Container: `petstore-baseline`)
  - Target MongoDB 7.0: `mongodb://localhost:27017/petstore?replicaSet=rs0`
- **Key Architectural Features**:
  - **O(1) In-Memory Shadow Reconciliation**: `ShadowReadComparator` pre-indexes MongoDB documents into hash maps for lightning-fast audit scans.
  - **Reverse Write-Back Synchronization**: `LegacyWriteBackConsumer` listens to `petstore.orders.created` and mirrors modern orders into legacy HSQLDB for zero-downtime rollback safety.
  - **BCrypt Credential Parity**: Recognizes BCrypt hashed passwords and performs cryptographic match verification against legacy credentials.
- **Kafka Integration**: Consumer for `petstore.orders.dualwrite`, Consumer for `petstore.orders.created` (Legacy write-back), Producer to `petstore.orders.dlq`.

### REST Endpoints
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/migration/extract-baseline` | Trigger historical baseline ETL from legacy HSQLDB to MongoDB | `ROLE_OPS`, `ROLE_ADMIN` |
| `GET` | `/api/v1/migration/parity?runAudit={bool}` | Real-time reconciliation metrics, match counts, and drift reports | `ROLE_OPS`, `ROLE_ADMIN` |
| `GET` | `/api/v1/migration/mongo-diagnostics` | Live MongoDB engine telemetry, connection pool, cache, and Compass URI | `ROLE_OPS`, `ROLE_ADMIN` |
| `GET` | `/actuator/health` | Health probe verifying HSQLDB and MongoDB connectivity | Internal / Ops |

---

## 4. `petstore-frontend` (Port 3000)

- **Description**: Modern Single-Page Application (SPA) built with React 18, Vite, and TypeScript.
- **Dev Server Command**: `npm run dev` (starts on port 3000).
- **Reverse Proxy**: Vite proxy configured in `vite.config.ts` mapping `/api` calls directly to backend microservices.

### Route Packages
| Route | Component | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `/` | `StorefrontLayout` / `StorefrontPage` | Public | Catalog browsing, multi-lingual switching (EN/JA/ZH), shopping cart |
| `/account` | `AccountPage` | Customer | Customer profile, past order history, line item delivery status |
| `/admin` | `AdminLayout` / `AdminOrdersPage` | `ROLE_ADMIN` | Order approval queue, sales category charts, customer metrics |
| `/supplier` | `SupplierLayout` / `SupplierInventoryPage` | `ROLE_SUPPLIER`, `ROLE_ADMIN` | Stock level editor, warehouse health KPIs, quick replenish |
| `/ops` | `OpsLayout` / `ParityMonitorPage` | `ROLE_OPS`, `ROLE_ADMIN` | Shadow reconciliation audit, MongoDB engine telemetry & Compass |

---

## 5. Infrastructure & Supporting Containers

| Component | Port | Technology | Purpose |
| :--- | :--- | :--- | :--- |
| **Legacy Pet Store** | 8000 | Apache TomEE 7 + Java 1.4/EJB 2.0 | Original System of Record (`/petstore`) |
| **Legacy Database** | 9001 | HSQLDB 1.7.1 / Cloudscape | Relational 3NF database (Container: `petstore-baseline`) |
| **MongoDB Replica Set** | 27017 | MongoDB 7.0.40 (`rs0`) | Primary target document datastore |
| **Kafka Broker** | 9092 | Apache Kafka 3.7 (KRaft mode) | Distributed event streaming backbone |
