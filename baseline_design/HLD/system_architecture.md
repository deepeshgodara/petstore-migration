# High-Level Design (HLD): System Architecture

## 1. Executive Architecture Overview

The **Java™ Pet Store Demo 1.3.1_02** is structured as an **N-Tier Enterprise Architecture** adhering to the Sun J2EE 1.3.1 BluePrints pattern. The system is decomposed into four primary decoupled enterprise applications:
1. **PetStore Web & Storefront (`petstore.ear`)**: Front-facing e-commerce storefront for browsing pet categories, managing shopping carts, customer account management, and placing orders.
2. **Order Processing Center - OPC (`opc.ear`)**: Enterprise integration hub managing asynchronous order validation, credit approval, invoicing, and email notification via JMS Message-Driven Beans (MDBs).
3. **Supplier Subsystem (`supplier.ear`)**: Independent supply-chain fulfillment and inventory tracking system.
4. **PetStore Administrator (`petstoreadmin.ear`)**: Administrative back-office client for managing inventory and order statuses.

```mermaid
flowchart TB
    subgraph ClientTier["Client Tier"]
        Browser["Web Browser (HTTP / Cookie Session)"]
        AdminBrowser["Admin Web Browser"]
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

    %% Presentation Tier Links
    Browser -->|"HTTP GET/POST /petstore/*"| MainServlet
    Browser -->|"HTTP GET /petstore/*.screen"| TemplateServlet
    TemplateServlet --> JSPViews
    MainServlet --> ShoppingClientFacade
    JSPViews --> CatalogHelper

    %% Fast Lane Reader Pattern
    CatalogHelper -.->|"Direct FastLane JDBC DAO"| PetStoreDB

    %% Business Links
    ShoppingClientFacade --> ShoppingController
    ShoppingClientFacade --> ShoppingCart
    ShoppingController --> AsyncSender
    ShoppingController --> UIDGen
    ShoppingClientFacade --> CustomerEJB
    MainServlet --> SignOnEJB
    SignOnEJB --> UserEJB
    CustomerEJB --> AccountEJB
    AccountEJB --> ProfileEJB
    AccountEJB --> AddressEJB
    AccountEJB --> CreditCardEJB

    %% JMS Links
    AsyncSender -->|"JMS ObjectMessage"| OrderQueue
    OrderQueue --> OrderApprovalMDB
    OrderApprovalMDB -->|"Approved"| OrderApprovalQueue
    OrderApprovalQueue --> PurchaseOrderMDB
    PurchaseOrderMDB -->|"Purchase Order"| PurchaseOrderQueue
    PurchaseOrderQueue --> SupplierOrderMDB
    SupplierOrderMDB -->|"Invoice Published"| InvoiceTopic
    InvoiceTopic --> InvoiceMDB
    InvoiceTopic --> MailInvoiceMDB
    MailInvoiceMDB -->|"Mail Request"| MailQueue

    %% Database Links
    CMPBeans -->|"Container Managed JDBC"| PetStoreDB
    OrderApprovalMDB -->|"JDBC"| OPCDB
    PurchaseOrderMDB -->|"JDBC"| OPCDB
    SupplierOrderMDB -->|"JDBC"| SupplierDB
    UIDGen -->|"JDBC"| PetStoreDB
```

---

## 2. Subsystem Descriptions

### 2.1 PetStore Web & Storefront (`petstore.ear`)
- **Web Application Framework (WAF)**: Implements an early MVC design pattern. Requests are routed through `MainServlet`, which delegates to `WebController` and `FlowHandler` to execute business actions and choose target screen definitions.
- **Fast Lane Reader Pattern**: A critical architectural pattern in 2002. Instead of loading read-only product catalog items through heavy EJB 2.0 entity beans, `CatalogHelper` directly invokes `CloudscapeCatalogDAO` to run fast scrolling SQL queries against `PetStoreDB`.
- **Conversational State**: Shopping carts and user context are preserved in memory via Stateful Session Beans (`ShoppingCartLocalEJB`, `ShoppingClientFacadeLocalEJB`) tied to HTTP session cookies.

### 2.2 Order Processing Center - OPC (`opc.ear`)
- **Enterprise Service Bus (JMS)**: Acts as the decoupled backend orchestration layer.
- When an order is placed in the storefront, `AsyncSenderEJB` sends an asynchronous `OrderQueue` message.
- `OrderApprovalMDB` verifies credit limit and inventory.
- Once approved, `PurchaseOrderMDB` generates a formal Purchase Order (PO) and routes it to the external `PurchaseOrderQueue`.

### 2.3 Supplier Subsystem (`supplier.ear`)
- Represents an external 3rd-party pet breeder/supplier.
- Consumes messages from `PurchaseOrderQueue` via `SupplierOrderMDB`.
- Restocks items, checks physical warehouse stock (`InventoryEJB`), and generates an `Invoice` published to `jms/opc/InvoiceTopic`.

### 2.4 Persistence Layer
- **CMP 2.0 (Container-Managed Persistence)**: Relational mapping for customer profiles, credit cards, addresses, and user accounts.
- **BMP (Bean-Managed Persistence)**: Used by `UniqueIdGeneratorEJB` to manage transaction-safe unique ID block allocation.
- **Direct JDBC (FastLane DAO)**: High-performance read queries for Categories, Products, and Items.
