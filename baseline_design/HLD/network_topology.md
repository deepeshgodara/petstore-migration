# High-Level Design (HLD): Network Topology

This document details the network connectivity, ports, protocols, security boundaries, and communication paths across the baseline 2002 Java Pet Store deployment.

---

## 1. Network Boundary & Topology Diagram

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
            HTTPListener["HTTP/1.1 Web Connector (Port 8080)"]
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

---

## 2. Port & Protocol Registry

| Protocol | Default Port | Source | Destination | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **HTTP 1.1** | `8080` | Web Browser Clients | Application Server | Storefront web navigation, shopping cart actions, JSP templates, image downloads, REST-like form posts. |
| **AJP 1.3** | `8009` | Apache Web Server / Mod_JK | Application Server | Reverse proxy routing for production J2EE deployments. |
| **RMI-IIOP** | `1099` / `4201` | Web Container / Client | EJB Container | Remote Enterprise JavaBean method invocations and JNDI context lookups. |
| **JMS (OpenWire)** | `61616` / `vm://` | AsyncSenderEJB / MDBs | ActiveMQ Broker | Asynchronous queue and topic message delivery for orders, invoices, and mail. |
| **JDBC** | `1527` (Derby) / `1521` (Oracle) | EJB Container / FastLane DAO | Database Engine | Relational persistence, transactions, and data seeding. |
| **SMTP** | `25` | Mailer Service | Mail Server | Dispatching order confirmation emails to customers. |

---

## 3. Communication Patterns

1. **Synchronous HTTP Request-Response**:
   - The browser maintains a stateful session via the `JSESSIONID` HTTP cookie.
   - All front controller requests are submitted synchronously to `http://localhost:8080/petstore/`.
2. **In-JVM Local EJB Inter-Process Communication**:
   - Servlets and Web Controllers invoke EJB 2.0 Local Homes (`EJBLocalHome`) and Local Interfaces (`EJBLocalObject`) within the same JVM memory space, eliminating RMI serialization overhead.
3. **Asynchronous P2P and Pub/Sub Messaging**:
   - Cross-subsystem coordination (Storefront -> Order Processing Center -> Supplier) is completely decoupled using message queues (`OrderQueue`, `PurchaseOrderQueue`) and topics (`InvoiceTopic`).
