# Legacy Pet Store (2002) - High-Level Design (HLD)

> **Heritage Context**: This document details the high-level architecture, deployment topology, network boundaries, and data flow pipelines of the **Sun Microsystems Java Pet Store Demo 1.3.1_02 (circa 2002)**, built as the official enterprise reference blueprint for **J2EE 1.3**.

---

## 1. Executive System Architecture

The 2002 Java Pet Store is decomposed into four decoupled enterprise applications adhering to the multi-tier J2EE BluePrints pattern:
1. **PetStore Web & Storefront (`petstore.ear`)**: E-commerce customer storefront for browsing pet categories, managing conversational shopping carts, user authentication, and placing orders.
2. **Order Processing Center - OPC (`opc.ear`)**: Enterprise integration hub managing asynchronous order validation, credit approval, supplier procurement, and email notification via JMS Message-Driven Beans (MDBs).
3. **Supplier Subsystem (`supplier.ear`)**: Independent supply-chain fulfillment and inventory tracking system.
4. **PetStore Administrator (`petstoreadmin.ear`)**: Administrative back-office client for managing orders and inventory status.

```mermaid
flowchart TB
    subgraph ClientTier["Client Tier"]
        Browser["Web Browser (HTTP / Cookie Session)"]
        AdminClient["Java Web Start / Swing Admin Client"]
    end

    subgraph WebTier["Presentation Tier (WAF Web Container)"]
        MainServlet["MainServlet (Front Controller)"]
        TemplateServlet["TemplateServlet (Screen Engine)"]
        JSPViews["JSP Screens & Custom Tags (category, product, item, cart)"]
        CatalogHelper["CatalogHelper (Web Tier Adapter)"]
    end

    subgraph EjbTier["Business Logic Tier (EJB Container)"]
        ShoppingClientFacade["ShoppingClientFacadeLocalEJB (SFSB)"]
        ShoppingController["ShoppingControllerLocalEJB (SLSB)"]
        ShoppingCart["ShoppingCartLocalEJB (SFSB)"]
        SignOnEJB["SignOnLocalEJB (SLSB)"]
        AsyncSender["AsyncSenderLocalEJB (JMS Producer)"]
        UIDGen["UniqueIdGeneratorLocalEJB (BMP)"]
        
        subgraph CMPBeans["CMP 2.0 Entity Beans"]
            UserEJB["UserEJB"]
            CustomerEJB["CustomerEJB"]
            AccountEJB["AccountEJB"]
            ProfileEJB["ProfileEJB"]
            AddressEJB["AddressEJB"]
            CreditCardEJB["CreditCardEJB"]
        end
    end

    subgraph MessagingTier["Asynchronous Messaging Subsystem (JMS)"]
        OrderQueue[("jms/opc/OrderQueue")]
        OrderApprovalQueue[("jms/opc/OrderApprovalQueue")]
        PurchaseOrderQueue[("jms/supplier/PurchaseOrderQueue")]
        InvoiceTopic[("jms/opc/InvoiceTopic")]
        MailQueue[("jms/opc/MailQueue")]
        
        subgraph MDBs["Message-Driven Beans (MDB)"]
            OrderApprovalMDB["OrderApprovalMDB (OPC)"]
            PurchaseOrderMDB["PurchaseOrderMDB (OPC)"]
            SupplierOrderMDB["SupplierOrderMDB (Supplier)"]
            InvoiceMDB["InvoiceMDB (OPC)"]
            MailInvoiceMDB["MailInvoiceMDB (OPC)"]
        end
    end

    subgraph DataTier["Persistence Tier (Relational DBMS)"]
        PetStoreDB[("PetStore DB (Cloudscape / Derby)")]
        OPCDB[("OPC DB")]
        SupplierDB[("Supplier DB")]
    end

    %% Client communication
    Browser -->|"HTTP GET/POST /petstore"| MainServlet
    AdminClient -->|"HTTP POST /admin/AdminRequestProcessor"| MainServlet
    
    %% Web Tier Flow
    MainServlet --> ShoppingClientFacade
    MainServlet --> TemplateServlet
    TemplateServlet --> JSPViews
    JSPViews --> CatalogHelper

    %% FastLane vs EJB Pattern
    CatalogHelper -.->|"FastLane Reader (Direct JDBC)"| PetStoreDB

    %% Business Tier Flow
    ShoppingClientFacade --> ShoppingCart
    ShoppingClientFacade --> ShoppingController
    ShoppingClientFacade --> SignOnEJB
    ShoppingController --> AsyncSender
    ShoppingController --> UIDGen
    SignOnEJB --> UserEJB
    ShoppingController --> CMPBeans

    %% Messaging Connections
    AsyncSender -->|"JMS Send"| OrderQueue
    OrderQueue --> OrderApprovalMDB
    OrderApprovalMDB -->|"Approved"| OrderApprovalQueue
    OrderApprovalQueue --> PurchaseOrderMDB
    PurchaseOrderMDB -->|"PO Dispatch"| PurchaseOrderQueue
    PurchaseOrderQueue --> SupplierOrderMDB
    SupplierOrderMDB -->|"Invoice Publish"| InvoiceTopic
    InvoiceTopic --> InvoiceMDB
    InvoiceTopic --> MailInvoiceMDB
    MailInvoiceMDB -->|"Queue Mail"| MailQueue

    %% Database Connections
    CMPBeans --> PetStoreDB
    UIDGen --> PetStoreDB
    OrderApprovalMDB --> OPCDB
    PurchaseOrderMDB --> OPCDB
    SupplierOrderMDB --> SupplierDB
```

---

## 2. Enterprise Application Packaging & Deployment Topology

The application is deployed across four `.ear` archives within a certified **J2EE 1.3 Container** (Apache TomEE Plus / JBoss):

```mermaid
graph TD
    subgraph J2EEContainer["J2EE 1.3 Application Server Container"]
        
        subgraph PetStoreEAR["petstore.ear (Storefront Application)"]
            PetStoreWAR["petstore.war<br/>(JSPs, Servlets, WAF Web Controller)"]
            PetStoreEJB["petstore-ejb.jar<br/>(ShoppingController, ShoppingClientFacade)"]
            CustomerEJB["customer-ejb.jar<br/>(Customer, Account, Profile, Address, CreditCard CMP)"]
            CartEJB["cart-ejb.jar<br/>(ShoppingCart SFSB)"]
            SignOnEJB["signon-ejb.jar<br/>(SignOn SLSB, User CMP)"]
            CatalogEJB["catalog-ejb.jar<br/>(CatalogDAO, CloudscapeCatalogDAO)"]
            UIDGenEJB["uidgen-ejb.jar<br/>(UniqueIdGenerator BMP, Counter CMP)"]
            AsyncSenderEJB["asyncsender-ejb.jar<br/>(AsyncSender SLSB)"]
        end

        subgraph OPCEAR["opc.ear (Order Processing Center)"]
            OPCEJB["opc-ejb.jar<br/>(OrderApprovalMDB, PurchaseOrderMDB, InvoiceMDB, MailInvoiceMDB)"]
            OPCAdminEJB["opc-admin-ejb.jar<br/>(OPCAdminFacade SLSB)"]
        end

        subgraph SupplierEAR["supplier.ear (Supplier Inventory System)"]
            SupplierWAR["supplier.war<br/>(Supplier Web Admin UI)"]
            SupplierEJB["supplier-ejb.jar<br/>(SupplierOrderMDB, OrderFulfillmentFacade, Inventory CMP)"]
        end

        subgraph PetStoreAdminEAR["petstoreadmin.ear (Admin Back-Office)"]
            AdminWAR["admin.war<br/>(Administrator Web Interface)"]
        end

        subgraph JMSBroker["Embedded / External JMS Message Broker"]
            Q_Order["jms/opc/OrderQueue"]
            Q_Approval["jms/opc/OrderApprovalQueue"]
            Q_PO["jms/supplier/PurchaseOrderQueue"]
            T_Invoice["jms/opc/InvoiceTopic"]
            Q_Mail["jms/opc/MailQueue"]
        end
    end

    subgraph RelationalDB["Relational Database Instances (Cloudscape / Derby / Oracle)"]
        DB_PetStore[("jdbc/petstore/PetStoreDB<br/>(Catalog, Customers, Accounts, Users)")]
        DB_OPC[("jdbc/opc/OPCDB<br/>(Orders, LineItems, Invoices)")]
        DB_Supplier[("jdbc/supplier/SupplierDB<br/>(Inventory, SupplierOrders)")]
    end

    %% EAR connections
    PetStoreWAR --> PetStoreEJB
    PetStoreWAR --> CartEJB
    PetStoreWAR --> CatalogEJB
    PetStoreEJB --> AsyncSenderEJB
    AsyncSenderEJB -->|"Produce"| Q_Order

    Q_Order -->|"Consume"| OPCEJB
    OPCEJB -->|"Produce"| Q_PO
    Q_PO -->|"Consume"| SupplierEJB
    SupplierEJB -->|"Publish"| T_Invoice
    T_Invoice -->|"Subscribe"| OPCEJB
    OPCEJB -->|"Produce"| Q_Mail

    PetStoreEAR --> DB_PetStore
    OPCEAR --> DB_OPC
    SupplierEAR --> DB_Supplier
```

### 2.1 Web Tier Modules (WAR)
| Archive | Context Root | Description & Key Servlets |
| :--- | :--- | :--- |
| `petstore.war` | `/petstore` | Main customer-facing storefront. Contains `MainServlet` (Front Controller), `TemplateServlet` (Layout Engine), `PopulateServlet` (Database Seeder), and standard JSTL taglibs. |
| `supplier.war` | `/supplier` | Supply-chain partner portal for checking inventory stock levels and manually triggering re-stocks. |
| `admin.war` | `/admin` | Store administrator dashboard for viewing orders, approving pending purchases, and tracking status. |

### 2.2 Enterprise JavaBean Modules (EJB-JAR)
| EJB JAR | EJB Name | Type | Purpose |
| :--- | :--- | :--- | :--- |
| `petstore-ejb.jar` | `ShoppingControllerEJB` | Stateless Session (SLSB) | Manages order creation, customer checkout transaction coordination. |
| `petstore-ejb.jar` | `ShoppingClientFacadeEJB` | Stateful Session (SFSB) | Conversational facade for an active customer browsing and purchasing. |
| `cart-ejb.jar` | `ShoppingCartEJB` | Stateful Session (SFSB) | Manages in-memory shopping cart items and subtotals. |
| `signon-ejb.jar` | `SignOnEJB` | Stateless Session (SLSB) | Handles password verification and authentication. |
| `signon-ejb.jar` | `UserEJB` | Entity Bean (CMP 2.0) | Stores username and password credentials. |
| `customer-ejb.jar` | `CustomerEJB` | Entity Bean (CMP 2.0) | Manages customer identity and preferences. |
| `customer-ejb.jar` | `AccountEJB` | Entity Bean (CMP 2.0) | Contains contact info, billing info, and credit card relationships. |
| `uidgen-ejb.jar` | `UniqueIdGeneratorEJB` | Entity Bean (BMP) | Generates monotonic unique ID sequences using transactional block allocation. |
| `asyncsender-ejb.jar` | `AsyncSenderEJB` | Stateless Session (SLSB) | JMS message publisher for order events. |
| `opc-ejb.jar` | `OrderApprovalMDB` | Message-Driven (MDB) | Consumes `OrderQueue`, validates order limits. |
| `opc-ejb.jar` | `PurchaseOrderMDB` | Message-Driven (MDB) | Consumes approved orders, issues PO messages to suppliers. |
| `opc-ejb.jar` | `InvoiceMDB` | Message-Driven (MDB) | Consumes invoices published by suppliers. |
| `opc-ejb.jar` | `MailInvoiceMDB` | Message-Driven (MDB) | Consumes invoices, formats confirmation emails for `MailQueue`. |
| `supplier-ejb.jar` | `SupplierOrderMDB` | Message-Driven (MDB) | Consumes `PurchaseOrderQueue`, adjusts inventory stock. |
| `supplier-ejb.jar` | `InventoryEJB` | Entity Bean (CMP 2.0) | Represents warehouse stock counts per item. |

### 2.3 JMS Destination Registry
| Destination Name | JNDI Lookup Name | Type | Producers | Consumers |
| :--- | :--- | :--- | :--- | :--- |
| `OrderQueue` | `jms/opc/OrderQueue` | Queue (P2P) | `AsyncSenderEJB` | `OrderApprovalMDB` |
| `OrderApprovalQueue` | `jms/opc/OrderApprovalQueue` | Queue (P2P) | `OrderApprovalMDB` | `PurchaseOrderMDB` |
| `PurchaseOrderQueue` | `jms/supplier/PurchaseOrderQueue` | Queue (P2P) | `PurchaseOrderMDB` | `SupplierOrderMDB` |
| `InvoiceTopic` | `jms/opc/InvoiceTopic` | Topic (Pub/Sub)| `SupplierOrderMDB` | `InvoiceMDB`, `MailInvoiceMDB` |
| `MailQueue` | `jms/opc/MailQueue` | Queue (P2P) | `MailInvoiceMDB` | `MailerService` (SMTP) |

---

## 3. Network Topology & Port Registry

```mermaid
flowchart TB
    subgraph InternetZone["Public Internet / Client Tier"]
        ClientBrowser["Web Browser (Shopper Client)"]
        AdminUser["Web Browser (Admin Client)"]
    end

    subgraph DMZ["DMZ / Perimeter Network"]
        FirewallExt["External Firewall (Port 80/443 & 8080)"]
    end

    subgraph AppServerZone["Application Tier (Private Network Zone)"]
        subgraph J2EEServer["J2EE Application Server (TomEE / JBoss / Sun RI)"]
            HTTPListener["HTTP/1.1 Web Connector (Port 8000 / 8080)"]
            AJPListener["AJP 1.3 Connector (Port 8009)"]
            JNDIBus["JNDI Naming / RMI-IIOP Bus (Port 1099 / 4201)"]
            ActiveMQBroker["Embedded JMS Broker (Port 61616 / VM In-Memory)"]
            
            EJBContainer["EJB 2.0 Business Logic Container"]
            WebContainer["Servlet 2.3 / JSP 1.2 Web Container"]
        end
    end

    subgraph DatabaseZone["Database Tier (Secure Isolated Zone)"]
        FirewallInt["Internal DB Firewall (Port 1527 / 1521)"]
        DerbyListener["Derby Network Server / Embedded Engine (Port 1527)"]
        PetStoreDB[("PetStoreDB")]
        OPCDB[("OPCDB")]
        SupplierDB[("SupplierDB")]
    end

    %% Network Connections
    ClientBrowser -->|"HTTP TCP:8080 /petstore"| FirewallExt
    AdminUser -->|"HTTP TCP:8080 /admin"| FirewallExt
    FirewallExt --> HTTPListener

    HTTPListener --> WebContainer
    WebContainer -->|"Internal RMI/JNDI Local References"| EJBContainer
    EJBContainer -->|"JMS OpenWire TCP:61616 or VM Direct"| ActiveMQBroker

    EJBContainer --> FirewallInt
    WebContainer -.->|"FastLane Direct JDBC"| FirewallInt
    FirewallInt --> DerbyListener
    DerbyListener --> PetStoreDB
    DerbyListener --> OPCDB
    DerbyListener --> SupplierDB
```

| Protocol | Default Port | Source | Destination | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **HTTP 1.1** | `8000` / `8080` | Web Browser / Clients | Application Server | Storefront navigation, cart updates, JSP templates, image downloads. |
| **AJP 1.3** | `8009` | Apache Web Server / Mod_JK | Application Server | Reverse proxy routing for production J2EE deployments. |
| **RMI-IIOP** | `1099` / `4201` | Web Container / Client | EJB Container | Remote Enterprise JavaBean method invocations and JNDI context lookups. |
| **JMS (OpenWire)** | `61616` / `vm://` | AsyncSenderEJB / MDBs | ActiveMQ Broker | Asynchronous queue and topic message delivery for orders, invoices, and email. |
| **JDBC** | `1527` (Derby) / `1521` (Oracle) | EJB Container / FastLane DAO | Database Engine | Relational persistence, transactions, and data seeding. |

---

## 4. Data Flow Diagrams (DFD)

### 4.1 DFD Level 0: System Context Diagram

```mermaid
flowchart LR
    Shopper["Shopper / Customer"]
    Admin["Administrator"]
    Supplier["External Supplier"]
    
    subgraph PetStoreSystem["Pet Store Enterprise System (J2EE 1.3)"]
        Core["Pet Store Core Application"]
    end
    
    %% Inbound / Outbound Flows
    Shopper -->|"1. Browse Catalog / Search"| Core
    Shopper -->|"2. Manage Cart / Items"| Core
    Shopper -->|"3. Sign In / Register"| Core
    Shopper -->|"4. Submit Order & Payment"| Core
    Core -->|"5. Order Confirmation & Email"| Shopper
    
    Admin -->|"6. Review Pending Orders"| Core
    Admin -->|"7. Approve / Cancel Order"| Core
    
    Core -->|"8. Dispatch Purchase Order (PO)"| Supplier
    Supplier -->|"9. Order Fulfillment & Invoices"| Core
    Supplier -->|"10. Restock Notifications"| Core
```

### 4.2 DFD Level 1: Subsystem Data Flow

```mermaid
flowchart TB
    Shopper["Shopper"]
    Admin["Administrator"]

    subgraph Presentation["Presentation Tier (WAF)"]
        P1["1.0 Catalog Browse & Search"]
        P2["2.0 User Session & Auth"]
        P3["3.0 Shopping Cart State"]
        P4["4.0 Checkout & Order Submission"]
    end

    subgraph Business["EJB Business Tier"]
        B1["5.0 SignOn & Profile Services"]
        B2["6.0 FastLane JDBC Catalog Reader"]
        B3["7.0 Order Coordinator & ID Generator"]
    end

    subgraph Messaging["JMS Asynchronous Queue & Topic Hub"]
        Q1[("jms/opc/OrderQueue")]
        Q2[("jms/opc/OrderApprovalQueue")]
        Q3[("jms/supplier/PurchaseOrderQueue")]
        Q4[("jms/opc/InvoiceTopic")]
        Q5[("jms/opc/MailQueue")]
    end

    subgraph Backend["Fulfillment & Notifications"]
        M1["8.0 OPC Order Processing & Validation"]
        M2["9.0 Supplier Fulfillment & Restocking"]
        M3["10.0 Email Notification Service"]
    end

    subgraph Datastores["Database Stores"]
        D1[("PetStore DB (Products, Users, Accounts)")]
        D2[("OPC DB (Orders, LineItems)")]
        D3[("Supplier DB (PO, Inventory)")]
    end

    %% Flows
    Shopper -->|"Category / Product Request"| P1
    P1 -->|"Fetch Category/Item"| B2
    B2 -->|"Direct SQL Query"| D1
    D1 -.->|"Result Set"| B2
    B2 -.->|"Page Model"| P1

    Shopper -->|"Credentials"| P2
    P2 -->|"Validate User"| B1
    B1 <-->|"Read / Write User CMP"| D1

    Shopper -->|"Add / Update / Remove Item"| P3
    P3 <-->|"Conversational Cart State"| P4

    Shopper -->|"Submit Order"| P4
    P4 -->|"Create Order"| B3
    B3 -->|"Generate ID"| D1
    B3 -->|"Publish New Order"| Q1

    Q1 -->|"Consume Order"| M1
    M1 <-->|"Read / Write Order Status"| D2
    M1 -->|"Order Approved"| Q2
    Q2 -->|"Create PO"| Q3

    Q3 -->|"Consume PO"| M2
    M2 <-->|"Check / Deduct Stock"| D3
    M2 -->|"Publish Invoice"| Q4

    Q4 -->|"Invoice Event"| M1
    Q4 -->|"Invoice Event"| M3
    M3 -->|"Queue Email"| Q5
    Q5 -.->|"Send SMTP Email"| Shopper

    Admin -->|"Review / Update Status"| M1
```

### 4.3 DFD Level 2: Detailed Asynchronous Order Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Shopper
    participant WAF as MainServlet / WAF
    participant Controller as ShoppingControllerLocalEJB
    participant UIDGen as UniqueIdGeneratorEJB
    participant AsyncSender as AsyncSenderLocalEJB
    participant OrderQueue as jms/opc/OrderQueue
    participant OrderApprovalMDB as OrderApprovalMDB (OPC)
    participant PurchaseOrderQueue as jms/supplier/PurchaseOrderQueue
    participant SupplierOrderMDB as SupplierOrderMDB (Supplier)
    participant InvoiceTopic as jms/opc/InvoiceTopic
    participant MailInvoiceMDB as MailInvoiceMDB (OPC)
    participant MailQueue as jms/opc/MailQueue

    Customer->>WAF: POST /petstore/order.do (Shipping, Billing, Cart)
    WAF->>Controller: createOrder(userId, cart, addresses, creditCard)
    Controller->>UIDGen: getNextId("ORDER_ID")
    UIDGen-->>Controller: Returns new Order ID (e.g., "1001")
    Controller->>AsyncSender: sendOrderMessage(OrderModel)
    AsyncSender->>OrderQueue: JMS ObjectMessage (Order)
    Controller-->>WAF: Order Created (Status: PENDING)
    WAF-->>Customer: Render Order Confirmation Screen

    %% Asynchronous Backend Processing
    Note over OrderQueue,OrderApprovalMDB: Asynchronous Order Verification
    OrderQueue->>OrderApprovalMDB: onMessage(Order)
    OrderApprovalMDB->>OrderApprovalMDB: Validate Credit Card & Inventory
    OrderApprovalMDB->>PurchaseOrderQueue: JMS ObjectMessage (PurchaseOrder)

    Note over PurchaseOrderQueue,SupplierOrderMDB: Asynchronous Supplier Processing
    PurchaseOrderQueue->>SupplierOrderMDB: onMessage(PurchaseOrder)
    SupplierOrderMDB->>SupplierOrderMDB: Update Warehouse Inventory & Pack Order
    SupplierOrderMDB->>InvoiceTopic: JMS ObjectMessage (Invoice)

    Note over InvoiceTopic,MailInvoiceMDB: Asynchronous Notification Fan-Out
    InvoiceTopic->>MailInvoiceMDB: onMessage(Invoice)
    MailInvoiceMDB->>MailQueue: JMS TextMessage (Email Content)
    MailQueue-->>Customer: Order Shipped Confirmation Email
```
