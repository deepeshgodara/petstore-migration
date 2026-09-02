# High-Level Design (HLD): Data Flow Diagrams (DFD)

This document presents the **Level 0 (Context)**, **Level 1 (Subsystems)**, and **Level 2 (Order & Async Event Processing)** Data Flow Diagrams for the 2002 Java Pet Store baseline.

---

## 1. DFD Level 0: System Context Diagram

The Level 0 diagram defines the system boundary and the interactions with external entities: **Shopper (Customer)**, **Administrator**, and **External Supplier**.

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

---

## 2. DFD Level 1: Subsystem Data Flow

The Level 1 diagram breaks down internal components: **Catalog Browsing**, **User & Account Management**, **Shopping Cart**, **Order Controller**, **OPC Messaging Subsystem**, and **Supplier Fulfillment**.

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

---

## 3. DFD Level 2: Detailed Order Lifecycle & Asynchronous JMS Flow

The Level 2 diagram captures the exact asynchronous event flow from initial checkout click to order finalization and notification.

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
