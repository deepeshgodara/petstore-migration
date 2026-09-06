# Legacy Pet Store (2002) - Low-Level Design (LLD)

> **Heritage Context**: This document details the micro-architectural design of the 2002 Sun Microsystems Java Pet Store v1.3.1_02, covering class hierarchies, sequence execution flows, physical relational ER schemas, state machines, and key algorithmic flowcharts.

---

## 1. Class Diagrams

### 1.1 Web Application Framework (WAF) Presentation Tier

The Web Application Framework (WAF) implements the Model-View-Controller (MVC) pattern for Servlet 2.3 and JSP 1.2.

```mermaid
classDiagram
    class HttpServlet {
        +doGet(HttpServletRequest, HttpServletResponse)
        +doPost(HttpServletRequest, HttpServletResponse)
    }

    class MainServlet {
        -WebController webController
        -URLMappings mappings
        +init(ServletConfig)
        +process(HttpServletRequest, HttpServletResponse)
        +doGet(HttpServletRequest, HttpServletResponse)
        +doPost(HttpServletRequest, HttpServletResponse)
    }

    class TemplateServlet {
        -ScreenDefinitions screenDefs
        +init(ServletConfig)
        +doGet(HttpServletRequest, HttpServletResponse)
        +insertTemplate(HttpServletRequest, HttpServletResponse, Screen)
    }

    class WebController {
        <<interface>>
        +handleRequest(HttpServletRequest)
        +processEvent(Event)
    }

    class ShoppingWebController {
        -ShoppingClientFacadeLocal facade
        +handleRequest(HttpServletRequest)
        +processEvent(Event)
    }

    class FlowHandler {
        <<interface>>
        +processFlow(HttpServletRequest)
        +doStart(HttpServletRequest)
        +doEnd(HttpServletRequest)
    }

    class Action {
        <<interface>>
        +perform(HttpServletRequest) EventResponse
    }

    class CartAction {
        +perform(HttpServletRequest) EventResponse
    }

    class OrderAction {
        +perform(HttpServletRequest) EventResponse
    }

    HttpServlet <|-- MainServlet
    HttpServlet <|-- TemplateServlet
    MainServlet --> WebController
    WebController <|.. ShoppingWebController
    ShoppingWebController --> FlowHandler
    ShoppingWebController --> Action
    Action <|.. CartAction
    Action <|.. OrderAction
```

### 1.2 Session Facade & Business Tier

```mermaid
classDiagram
    class SessionBean {
        <<interface>>
        +ejbActivate()
        +ejbPassivate()
        +ejbRemove()
        +setSessionContext(SessionContext)
    }

    class ShoppingClientFacadeLocal {
        <<interface>>
        +getCart() ShoppingCartLocal
        +createOrder(OrderModel)
        +authenticate(String, String) boolean
        +getCustomerDetails() CustomerModel
    }

    class ShoppingClientFacadeLocalEJB {
        -ShoppingCartLocal cart
        -CustomerLocal customer
        -ShoppingControllerLocal controller
        +ejbCreate()
        +getCart() ShoppingCartLocal
        +createOrder(OrderModel)
        +authenticate(String, String) boolean
    }

    class ShoppingCartLocal {
        <<interface>>
        +addItem(String, int)
        +removeItem(String)
        +updateQuantity(String, int)
        +getItems() Collection
        +getSubtotal() double
    }

    class ShoppingCartLocalEJB {
        -Map items
        -double subtotal
        +ejbCreate()
        +addItem(String, int)
        +removeItem(String)
        +updateQuantity(String, int)
        +getItems() Collection
        +getSubtotal() double
    }

    class ShoppingControllerLocal {
        <<interface>>
        +processOrder(OrderModel)
    }

    class ShoppingControllerLocalEJB {
        -AsyncSenderLocal asyncSender
        -UniqueIdGeneratorLocal uidGen
        +processOrder(OrderModel)
    }

    SessionBean <|.. ShoppingClientFacadeLocalEJB
    ShoppingClientFacadeLocal <|.. ShoppingClientFacadeLocalEJB
    SessionBean <|.. ShoppingCartLocalEJB
    ShoppingCartLocal <|.. ShoppingCartLocalEJB
    SessionBean <|.. ShoppingControllerLocalEJB
    ShoppingControllerLocal <|.. ShoppingControllerLocalEJB
    ShoppingClientFacadeLocalEJB --> ShoppingCartLocal
    ShoppingClientFacadeLocalEJB --> ShoppingControllerLocal
```

### 1.3 FastLane Reader Catalog DAO Tier

```mermaid
classDiagram
    class CatalogDAO {
        <<interface>>
        +getCategory(String, Locale) CategoryModel
        +getCategories(int, int, Locale) Page
        +getProduct(String, Locale) ProductModel
        +getProducts(String, int, int, Locale) Page
        +getItem(String, Locale) ItemModel
        +getItems(String, int, int, Locale) Page
        +searchProducts(String, int, int, Locale) Page
    }

    class CloudscapeCatalogDAO {
        -DataSource ds
        -Connection getConnection()
        +getCategory(String, Locale) CategoryModel
        +getProduct(String, Locale) ProductModel
        +getItem(String, Locale) ItemModel
        +searchProducts(String, int, int, Locale) Page
    }

    class CatalogHelper {
        -CatalogDAO dao
        +getCategory(String) CategoryModel
        +getProduct(String) ProductModel
        +getItem(String) ItemModel
    }

    CatalogDAO <|.. CloudscapeCatalogDAO
    CatalogHelper --> CatalogDAO
```

### 1.4 CMP 2.0 Entity Bean Tier

```mermaid
classDiagram
    class EntityBean {
        <<interface>>
        +ejbLoad()
        +ejbStore()
        +ejbActivate()
        +ejbPassivate()
        +ejbRemove()
        +setEntityContext(EntityContext)
    }

    class UserEJB {
        <<abstract>>
        +getUserName()* String
        +getPassword()* String
        +setUserName(String)*
        +setPassword(String)*
    }

    class CustomerEJB {
        <<abstract>>
        +getCustomerId()* String
        +getUserName()* String
        +getAccount()* AccountLocal
        +setAccount(AccountLocal)*
    }

    class AccountEJB {
        <<abstract>>
        +getAccountId()* String
        +getEmail()* String
        +getPhone()* String
        +getAddress()* AddressLocal
        +getCreditCard()* CreditCardLocal
    }

    class AddressEJB {
        <<abstract>>
        +getAddressId()* String
        +getStreetName()* String
        +getCity()* String
        +getState()* String
        +getZipCode()* String
        +getCountry()* String
    }

    class CreditCardEJB {
        <<abstract>>
        +getCardNumber()* String
        +getCardType()* String
        +getExpiryDate()* String
    }

    EntityBean <|.. UserEJB
    EntityBean <|.. CustomerEJB
    EntityBean <|.. AccountEJB
    EntityBean <|.. AddressEJB
    EntityBean <|.. CreditCardEJB
    CustomerEJB "1" --> "1" AccountEJB : cmr-field
    AccountEJB "1" --> "1" AddressEJB : cmr-field
    AccountEJB "1" --> "1" CreditCardEJB : cmr-field
```

---

## 2. Sequence Diagrams

### 2.1 FastLane Catalog Browsing Flow

Demonstrates the bypass of EJB container overhead for high-performance read-only queries:

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant MainServlet as MainServlet (WAF)
    participant ScreenDef as ScreenDefinition
    participant TemplateServlet as TemplateServlet
    participant JSP as category.jsp
    participant CatalogHelper as CatalogHelper
    participant CatalogDAO as CloudscapeCatalogDAO
    participant DB as PetStoreDB (Cloudscape)

    Browser->>MainServlet: GET /petstore/category.screen?category_id=FISH
    MainServlet->>ScreenDef: getScreen("category")
    ScreenDef-->>MainServlet: Screen (template.jsp, body=category.jsp)
    MainServlet->>TemplateServlet: forward(request, response)
    TemplateServlet->>JSP: include(category.jsp)
    JSP->>CatalogHelper: getProducts("FISH", start=0, count=2)
    CatalogHelper->>CatalogDAO: getProducts("FISH", 0, 2, "en_US")
    CatalogDAO->>DB: SELECT * FROM PRODUCT WHERE CATEGORY_ID='FISH'
    DB-->>CatalogDAO: ResultSet (FI-SW-01, FI-FW-02)
    CatalogDAO-->>CatalogHelper: Page (ProductModel List)
    CatalogHelper-->>JSP: ProductList
    JSP-->>TemplateServlet: Rendered HTML Table
    TemplateServlet-->>Browser: HTTP 200 OK (Rendered Screen)
```

### 2.2 Conversational Shopping Cart Flow

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant MainServlet as MainServlet
    participant ShoppingWebController as ShoppingWebController
    participant CartAction as CartAction
    participant Facade as ShoppingClientFacadeLocalEJB (SFSB)
    participant Cart as ShoppingCartLocalEJB (SFSB)

    Browser->>MainServlet: POST /petstore/cart.do?action=add&itemId=EST-1
    MainServlet->>ShoppingWebController: handleRequest(request)
    ShoppingWebController->>CartAction: perform(request)
    CartAction->>Facade: getCart()
    Facade-->>CartAction: ShoppingCartLocal reference
    CartAction->>Cart: addItem("EST-1", quantity=1)
    Cart->>Cart: Recalculate subtotal in memory
    Cart-->>CartAction: Success
    CartAction-->>ShoppingWebController: EventResponse(success)
    ShoppingWebController-->>MainServlet: Forward to cart.screen
    MainServlet-->>Browser: HTTP 302 Redirect (/petstore/cart.screen)
```

### 2.3 User Authentication & SignOn Flow

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant MainServlet as MainServlet
    participant SignOnAction as SignOnAction
    participant Facade as ShoppingClientFacadeLocalEJB
    participant SignOnEJB as SignOnLocalEJB (SLSB)
    participant UserEJB as UserEJB (CMP 2.0)
    participant DB as PetStoreDB

    Browser->>MainServlet: POST /petstore/signon.do (username="j2ee", password="password")
    MainServlet->>SignOnAction: perform(request)
    SignOnAction->>Facade: authenticate("j2ee", "password")
    Facade->>SignOnEJB: authenticate("j2ee", "password")
    SignOnEJB->>UserEJB: findByPrimaryKey("j2ee")
    UserEJB->>DB: SELECT PASSWORD FROM USER WHERE USERNAME='j2ee'
    DB-->>UserEJB: "password" (Plaintext)
    UserEJB-->>SignOnEJB: UserEJB instance
    SignOnEJB->>SignOnEJB: String compare (entered == stored)
    SignOnEJB-->>Facade: true
    Facade->>Facade: Bind authenticated Customer to Session
    Facade-->>SignOnAction: Authentication Successful
    SignOnAction-->>Browser: HTTP 302 Redirect (/petstore/main.screen)
```

### 2.4 Asynchronous Order Fulfillment Flow

```mermaid
sequenceDiagram
    autonumber
    actor Shopper as Customer
    participant Controller as ShoppingControllerLocalEJB
    participant UIDGen as UniqueIdGeneratorEJB
    participant AsyncSender as AsyncSenderLocalEJB
    participant OrderQueue as jms/opc/OrderQueue
    participant OrderApprovalMDB as OrderApprovalMDB
    participant POQueue as jms/supplier/PurchaseOrderQueue
    participant SupplierMDB as SupplierOrderMDB
    participant InvoiceTopic as jms/opc/InvoiceTopic
    participant MailMDB as MailInvoiceMDB

    Shopper->>Controller: createOrder(OrderModel)
    Controller->>UIDGen: getNextId("ORDER_ID")
    UIDGen-->>Controller: "1001"
    Controller->>AsyncSender: sendOrderMessage(OrderModel)
    AsyncSender->>OrderQueue: JMS ObjectMessage (Order 1001)
    Controller-->>Shopper: Order Submitted (Status: PENDING)

    %% Asynchronous Background
    OrderQueue->>OrderApprovalMDB: onMessage(Order 1001)
    OrderApprovalMDB->>OrderApprovalMDB: Check Credit Limit
    OrderApprovalMDB->>POQueue: JMS ObjectMessage (PurchaseOrder 1001)
    POQueue->>SupplierMDB: onMessage(PO 1001)
    SupplierMDB->>SupplierMDB: Deduct stock from InventoryEJB
    SupplierMDB->>InvoiceTopic: JMS ObjectMessage (Invoice 1001)
    InvoiceTopic->>MailMDB: onMessage(Invoice 1001)
    MailMDB-->>Shopper: Send Confirmation Email via SMTP
```

---

## 3. Relational Entity-Relationship (ER) Diagrams

The legacy architecture fragments data across three separate databases in 3NF normalized schema:

```mermaid
erDiagram
    %% PetStoreDB
    CATEGORY ||--o{ CATEGORY_DETAILS : "has localized"
    CATEGORY ||--o{ PRODUCT : "contains"
    PRODUCT ||--o{ PRODUCT_DETAILS : "has localized"
    PRODUCT ||--o{ ITEM : "contains"
    ITEM ||--o{ ITEM_DETAILS : "has localized"
    USER ||--|| CUSTOMER : "identifies"
    CUSTOMER ||--|| ACCOUNT : "has"
    ACCOUNT ||--|| ADDRESS : "billing address"
    ACCOUNT ||--|| CREDITCARD : "payment method"
    ACCOUNT ||--|| PROFILE : "preferences"

    %% OPCDB
    PURCHASEORDER ||--|{ LINEITEM : "contains"
    PURCHASEORDER ||--|| CONTACTINFO : "shipping contact"
    PURCHASEORDER ||--|| INVOICE : "generates"

    %% SupplierDB
    SUPPLIERORDER ||--|{ SUPPLIERORDERLINE : "contains"
    INVENTORY ||--|| ITEM : "tracks stock for"

    CATEGORY {
        string catid PK
    }
    CATEGORY_DETAILS {
        string catid FK
        string locale PK
        string name
        string descn
    }
    PRODUCT {
        string productid PK
        string catid FK
    }
    PRODUCT_DETAILS {
        string productid FK
        string locale PK
        string name
        string descn
        string image
    }
    ITEM {
        string itemid PK
        string productid FK
    }
    ITEM_DETAILS {
        string itemid FK
        string locale PK
        decimal listprice
        decimal unitcost
        string attr1
    }
    USER {
        string userid PK
        string password
    }
    CUSTOMER {
        string customerid PK
        string userid FK
        string accountid FK
    }
    ACCOUNT {
        string accountid PK
        string email
        string phone
        string addressid FK
        string cardnumber FK
    }
    PURCHASEORDER {
        int orderid PK
        string userid
        date orderdate
        decimal totalprice
        string status
    }
    LINEITEM {
        int orderid FK
        int linenum PK
        string itemid
        int quantity
        decimal unitprice
    }
    INVENTORY {
        string itemid PK
        int qty
    }
```

---

## 4. State Machine Diagrams

### 4.1 Purchase Order State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : Order Created (ShoppingController)
    PENDING --> APPROVED : Credit Verified (OrderApprovalMDB)
    PENDING --> REJECTED : Credit Declined / Limit Exceeded
    APPROVED --> PROCESSING : Purchase Order Issued (PurchaseOrderMDB)
    PROCESSING --> COMPLETED : Invoice Generated & Shipped (SupplierOrderMDB)
    REJECTED --> [*]
    COMPLETED --> [*]
```

### 4.2 Stateful Session Bean (SFSB) Shopping Cart Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DOES_NOT_EXIST
    DOES_NOT_EXIST --> ACTIVE : ejbCreate() / Client session starts
    ACTIVE --> ACTIVE : addItem() / updateQuantity() / removeItem()
    ACTIVE --> PASSIVATED : ejbPassivate() (Server memory pressure)
    PASSIVATED --> ACTIVE : ejbActivate() (Session re-accessed)
    ACTIVE --> DOES_NOT_EXIST : ejbRemove() / Session Timeout (30 min)
    PASSIVATED --> DOES_NOT_EXIST : Container Timeout Sweep
```

---

## 5. Algorithmic Flowcharts & Pseudocode

### 5.1 WAF Front Controller Request Processing Algorithm

```mermaid
flowchart TD
    Start(["HTTP Request Inbound"]) --> ParseURL["Extract Path: request.getPathInfo()"]
    ParseURL --> LookupAction{"Action Mapped in screendefinitions.xml?"}
    
    LookupAction -->|Yes| ExecAction["Execute Action.perform(request)"]
    LookupAction -->|No| CheckScreen{"Direct .screen Request?"}
    
    ExecAction --> CheckResult{"Action Result Status"}
    CheckResult -->|Success| GetFlowTarget["Resolve FlowHandler target screen"]
    CheckResult -->|Error| GetErrorScreen["Set error.screen"]
    
    GetFlowTarget --> ForwardScreen["Forward to TemplateServlet"]
    GetErrorScreen --> ForwardScreen
    CheckScreen -->|Yes| ForwardScreen
    CheckScreen -->|No| Send404["Send HTTP 404 Not Found"]
    
    ForwardScreen --> LoadLayout["Load template.jsp (Banner, Sidebar, Footer)"]
    LoadLayout --> IncludeBody["jsp:include screen body JSP"]
    IncludeBody --> RenderClient(["Stream HTML to Browser Client"])
```

### 5.2 Unique ID Generation Algorithm (Block Allocation)

```mermaid
flowchart TD
    ReqID(["Client requests getNextId('ORDER_ID')"]) --> CheckBlock{"Current Block Remaining? (current < maxId)"}
    CheckBlock -->|Yes| Increment["current = current + 1"]
    Increment --> ReturnID(["Return allocated ID"])
    
    CheckBlock -->|No| StartTx["Begin RequiresNew Database Transaction"]
    StartTx --> SelectForUpdate["SELECT NEXT_ID, BLOCK_SIZE FROM UIDGEN WHERE NAME='ORDER_ID' FOR UPDATE"]
    SelectForUpdate --> CalcNew["newNextId = NEXT_ID + BLOCK_SIZE"]
    CalcNew --> UpdateDB["UPDATE UIDGEN SET NEXT_ID = newNextId WHERE NAME='ORDER_ID'"]
    UpdateDB --> CommitTx["Commit Database Transaction"]
    CommitTx --> UpdateLocal["current = NEXT_ID; maxId = newNextId"]
    UpdateLocal --> Increment
```
