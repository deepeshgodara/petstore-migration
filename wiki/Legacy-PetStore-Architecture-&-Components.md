# Legacy Pet Store (v1.3.1_02) Architecture & Component Deep Dive

> **Heritage Context**: The Java Pet Store 1.3.1_02 was designed by Sun Microsystems in 2002 as the official reference implementation for the **Java 2 Enterprise Edition (J2EE 1.3) BluePrints specification**. It demonstrated how to architect multi-tier enterprise web applications using Servlets, JSPs, Enterprise JavaBeans (EJBs), Java Message Service (JMS), and relational database persistence.

---

## 1. High-Level Monolithic Architecture

The legacy system was packaged as four interconnected Enterprise Application Archives (`.ear` files) deployed inside a heavy J2EE application server (such as Sun Java System Application Server, JBoss 3/4, or Apache TomEE):

```
+----------------------------------------------------------------------------------------------------+
|                                LEGACY 2002 J2EE APPLICATION SERVER                                 |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ petstore.ear ]                  [ opc.ear ]                    [ supplier.ear ]                 |
|  - Customer Storefront Web App     - Order Processing Center      - Supplier Inventory Portal      |
|  - WAF (MVC Framework)             - Asynchronous Order Engine    - InventoryEJB & Stock Mgmt      |
|  - ShoppingCart Stateful EJB       - Message-Driven Beans (MDB)   - SupplierOrderEJB               |
|  - Catalog Stateless EJB           - CreditCard & Invoice EJB     - JSP Stock Management Screens   |
|  - JSP Presentation Pages          - Point-to-Point JMS Queues                                     |
|                                                                                                    |
|  [ petstoreadmin.ear ]                                                                             |
|  - Java Web Start & Java Swing Desktop GUI (`AdminApp`)                                            |
|  - RMI-IIOP Remote EJB Invocations                                                                 |
|  - Order Approval Queue & Sales Category Pie/Bar Charts                                            |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
                                      |                     |
                              (JMS Message Broker)     (JDBC 2.0 Pool)
                                      v                     v
                           Point-to-Point Queues   HSQLDB / Cloudscape
                           `queue/opc/OrderQueue`   12+ Relational Tables
```

---

## 2. The Four Enterprise Archives (.ear)

### 2.1 `petstore.ear` (Customer Storefront Monolith)
The primary customer-facing web application responsible for browsing, account management, cart manipulation, and order submission.
- **Packaging**: Contains `petstore.war` (Web Archive) and supporting EJB-JARs (`catalog-ejb.jar`, `cart-ejb.jar`, `customer-ejb.jar`, `signon-ejb.jar`).
- **Presentation**: Built using the Sun **Web Application Framework (WAF)**, an early precursor to Apache Struts and Spring MVC.
- **Session State**: Relies heavily on **Stateful Session Beans (SFSB)** (`ShoppingCartEJB`) pinned to the user's HTTP session in application server memory.

### 2.2 `opc.ear` (Order Processing Center)
The asynchronous fulfillment and back-office order processing engine.
- **Decoupled Messaging**: Communicates with the storefront strictly via JMS point-to-point queues (`queue/opc/OrderQueue`).
- **Orchestration**: Employs `OrderProcessManager` to coordinate credit card verification, inventory validation, invoice creation, and supplier purchase orders.
- **Message-Driven Beans (MDBs)**: Consumes incoming orders off JMS queues without blocking the storefront user thread.

### 2.3 `supplier.ear` (Supplier Inventory Portal)
The inventory and replenishment portal used by wholesale pet suppliers.
- **Web UI**: Served via JSP screens at `/supplier/displayinventory.jsp`.
- **Business Logic**: `InventoryEJB` (Entity Bean representing current item stock levels) and `SupplierOrderEJB` (purchase orders sent to external breeders and hatcheries).
- **Replenishment**: Allows suppliers to inspect low-stock alerts and submit batch replenishment quantities.

### 2.4 `petstoreadmin.ear` (Java Web Start & Swing Desktop Client)
The administrator console for back-office operators.
- **Architecture**: A desktop application (`AdminApp.jar`) launched via **Java Web Start** (JNLP protocol) or executed locally via `run_admin_client.sh`.
- **UI Framework**: Java Swing (`JFrame`, `JTable`, `JPanel`, custom Java 2D rendering).
- **Connectivity**: Connects to the application server via **RMI-IIOP (Remote Method Invocation over Internet Inter-ORB Protocol)** through JNDI naming context `corbaloc:iiop:localhost:2809`.
- **Features**:
  - Review pending orders and transition them to `APPROVED` or `DENIED`.
  - Sales-by-category charts rendered using custom Java 2D vector graphics.

---

## 3. Web Application Framework (WAF) Deep Dive

Before Struts or Spring MVC became standard, Sun created the **WAF** within the Pet Store BluePrints to demonstrate the Front Controller, View Helper, and Composite View design patterns.

```
Incoming HTTP Request
        │
        ▼
┌───────────────────┐
│   MainServlet     │  (Front Controller: intercept request, create WebClientController)
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  RequestProcessor │  (Parse action URL, resolve RequestMapping from requestmappings.xml)
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│    Event / Action │  (e.g., CartDoEvent, OrderEvent -> invokes Business Delegate / EJB)
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ TemplateServlet   │  (Composite View: loads screendefinitions.xml, injects header, body, footer)
└─────────┬─────────┘
          │
          ▼
    HTML Response (via JSP Taglibs: <waf:screen>, <waf:parameter>)
```

### Key WAF Components:
1. **`MainServlet`**: The Front Controller servlet intercepting all `*.do` and action requests.
2. **`requestmappings.xml`**: Declarative XML configuration mapping request URLs (e.g., `cart.do`, `checkout.do`) to action classes and event handlers.
3. **`screendefinitions.xml`**: Declarative layout definition file assembling composite JSP screens from modular fragments (`banner.jsp`, `sidebar.jsp`, `body.jsp`, `footer.jsp`).
4. **`TemplateServlet`**: Injects screen fragments into a master HTML template at runtime.

---

## 4. Enterprise JavaBeans (EJB 2.0) Architecture

The legacy application made extensive use of all three EJB 2.0 component types:

### 4.1 Stateless Session Beans (SLSB)
- **`CatalogEJB`**: Provides catalog read queries (`getCategories()`, `getProducts()`, `getItems()`).
- **`SignOnEJB`**: Handles user authentication, credential validation, and JAAS integration.
- **`OrderApprovalEJB`**: Provides administrative methods for approving or denying customer orders.

### 4.2 Stateful Session Beans (SFSB)
- **`ShoppingCartEJB`**: Maintains the user's active shopping cart items, quantities, and subtotal across multiple HTTP requests.
  > [!WARNING]
  > **Architectural Pitfall**: Stateful session beans prevent horizontal scaling because every user session is tightly pinned to a specific application server node's heap memory. Session replication requires costly distributed memory clustering.

### 4.3 Entity Beans (CMP & BMP)
- **Container-Managed Persistence (CMP 2.0)**: Used in `PurchaseOrderEJB`, `LineItemEJB`, `AddressEJB`, `CreditCardEJB`, and `AccountEJB`.
- Database access was declared through abstract getters/setters and EJB-QL deployment descriptors (`ejb-jar.xml` and `sun-j2ee-ri.xml`).
- Object-relational mapping (ORM) was handled by the container, generating complex nested SQL queries and primary key counters (`CounterEJBTable`).

### 4.4 Message-Driven Beans (MDB)
- **`OrderApprovalMDB`**: Listens asynchronously to `queue/opc/OrderQueue` for newly placed orders.
- **`MailerMDB`**: Consumes from `queue/mailer/MailQueue` to dispatch JavaMail SMTP order confirmations.

---

## 5. Legacy Relational Database Schema (3NF)

The legacy database was modeled in strict **Third Normal Form (3NF)** across more than a dozen normalized tables in Cloudscape/HSQLDB:

```
+-------------------+        +-------------------+        +-------------------+
|     CATEGORY      | 1    * |      PRODUCT      | 1    * |       ITEM        |
+-------------------+------->+-------------------+------->+-------------------+
| catid (PK)        |        | productid (PK)    |        | itemid (PK)       |
|                   |        | catid (FK)        |        | productid (FK)    |
+-------------------+        +-------------------+        | listprice         |
          | 1                          | 1                | unitcost          |
          | *                          | *                +-------------------+
          v                            v                            | 1
+-------------------+        +-------------------+                  | *
| CATEGORY_DETAILS  |        |  PRODUCT_DETAILS  |                  v
+-------------------+        +-------------------+        +-------------------+
| catid (PK)        |        | productid (PK)    |        |   ITEM_DETAILS    |
| locale (PK)       |        | locale (PK)       |        +-------------------+
| name              |        | name              |        | itemid (PK)       |
| description       |        | description       |        | locale (PK)       |
+-------------------+        +-------------------+        | attr1 (e.g. Size) |
                                                          +-------------------+
```

### Order Domain Relational Tables:
- **`PURCHASEORDER`**: `poId` (PK), `poDate`, `poLocale`, `poUserId`, `poValue`, `poEmailId`.
- **`LINEITEM`**: `__PMPrimaryKey` (PK), `poId` (FK), `lineNumber`, `itemId`, `productId`, `categoryId`, `quantity`, `unitPrice`.
- **`MANAGER`**: `orderId` (PK), `status` (`PENDING`, `APPROVED`, `DENIED`, `COMPLETED`).
- **`CONTACTINFO`**: `familyName`, `givenName`, `telephone`, `email`.
- **`ADDRESS`**: `streetName1`, `streetName2`, `city`, `state`, `zipCode`, `country`.
- **`CREDITCARD`**: `cardNumber`, `cardType`, `expiryDate`.
- **`COUNTER`**: `name` (PK), `counter` (High-low primary key generator).

---

## 6. End-to-End Legacy Order Lifecycle

```
[ Customer Browser ]
        │
   1. Submits Checkout Form (Credit Card, Billing, Shipping)
        │
        ▼
[ MainServlet / WAF Controller ]
        │
   2. Delegates to ShoppingCartEJB (Stateful Session Bean)
        │
        ▼
[ PurchaseOrderEJB (Entity Bean) ]
        │
   3. Generates poId via CounterEJB table lock
   4. Inserts into PURCHASEORDER, LINEITEM, ADDRESS, CREDITCARD tables
        │
        ▼
[ JMS OrderQueue ]  ◄── 5. Places serialized XML order message onto queue
        │
   (Asynchronous Boundary - HTTP thread returns "Order Received" to customer)
        │
        ▼
[ opc.ear (Order Processing Center) ]
        │
   6. OrderApprovalMDB consumes message
   7. OrderProcessManager validates credit card & checks inventory
   8. Inserts entry into MANAGER table with status = 'PENDING'
        │
        ▼
[ petstoreadmin.ear (Admin Swing Client) ]
        │
   9. Operator connects via RMI-IIOP and reviews pending order queue
  10. Operator clicks "Approve" -> updates MANAGER table to 'APPROVED'
        │
        ▼
[ supplier.ear (Supplier Inventory) ]
        │
  11. Supplier checks /supplier/displayinventory.jsp
  12. Decrements InventoryEJBTable stock quantity
```

---

## 7. Key Deficiencies That Drove Modernization

| Deficiency Area | Legacy J2EE Implementation | Modern Architecture Solution |
| :--- | :--- | :--- |
| **Monolithic Coupling** | All components coupled via `.ear` files; single point of failure. | **Spring Boot 3.3 Microservices** (`catalog-service`, `order-service`, `migration-service`). |
| **Session Brittleness** | Stateful Session Beans (`ShoppingCartEJB`) pinned in server RAM. | **Stateless REST APIs** with client-side reactive state in React 18 / localStorage. |
| **Relational Join Overhead** | Complex 3NF joins across 12+ tables for a single order view. | **MongoDB Document Aggregates** (`petstore_orders` with embedded line items and addresses). |
| **Localization Complexity** | Separate `_DETAILS` tables keyed by `(ID, LOCALE)`; fragile session locale. | **Polymorphic multi-language dictionaries** (`name: { en_US: "...", zh_CN: "...", ja_JP: "..." }`). |
| **Runtime Portability** | Requires 32-bit x86 JVM, obsolete Java Web Start, and CORBA/RMI network bridges. | **Dockerized Java 21 LTS containers** running natively on ARM64 and x86_64 cloud infrastructure. |
| **Admin Client Obsolescence** | Java Web Start desktop Swing client (`petstoreadmin.ear`). | **Modern React 18 Web Admin Console** with real-time SVG charts and cohort analytics. |
| **Message Broker** | Point-to-point JMS queues with local queue persistence. | **Apache Kafka 3.7+ cluster** with event streaming, partition scalability, and replayability. |

---

## 8. Preserving Legacy Compatibility During Migration

To ensure zero risk and non-destructive operation throughout the phased migration:
1. **Zero Legacy Modifications**: The legacy 2002 source files in `petstore-legacy/src/`, `petstore-legacy/build.xml`, and `petstore-legacy/setup.sh` are **100% frozen, isolated, and untouched**.
2. **Containerized Execution**: The legacy runtime is encapsulated in `docker/legacy-tomee/`, running on port `8000`.
3. **Dual-Write Synchronization**: New transactions are synchronized to both legacy and modern stores via Kafka-backed dual-write pipelines.
4. **Instant Rollback**: If an issue occurs, the traffic router can instantly revert traffic to the legacy container (`http://localhost:8000/petstore`) because the legacy database remains continuously synchronized.
