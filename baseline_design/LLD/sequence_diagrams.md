# Low-Level Design (LLD): Sequence Diagrams

This document captures the detailed chronological interaction sequence for the primary execution flows in the 2002 Java Pet Store:
1. **FastLane Catalog Browsing Flow**
2. **Shopping Cart Conversational State Flow**
3. **User Authentication & Sign-On Flow**
4. **End-to-End Asynchronous Order Placement & OPC Fulfillment Flow**

---

## 1. FastLane Catalog Browsing Sequence Diagram

Demonstrates how the presentation tier bypasses EJB entity beans to directly query the database via JDBC.

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant TemplateServlet as TemplateServlet (WAF)
    participant CategoryJSP as category.jsp
    participant CatalogHelper as CatalogHelper (JavaBean)
    participant DAOFactory as CatalogDAOFactory
    participant CloudscapeDAO as CloudscapeCatalogDAO
    participant DataSource as DataSource (jdbc/petstore/PetStoreDB)
    participant Database as Cloudscape / Derby DB

    Browser->>TemplateServlet: GET /petstore/category.screen?category_id=FISH&start=0&count=2
    TemplateServlet->>CategoryJSP: Forward to category.jsp via template.jsp
    CategoryJSP->>CatalogHelper: setCategoryId("FISH")
    CategoryJSP->>CatalogHelper: setStart(0)
    CategoryJSP->>CatalogHelper: setCount(2)
    CategoryJSP->>CatalogHelper: setLocale("en_US")
    CategoryJSP->>CatalogHelper: getProducts()

    alt FastLane Mode Active
        CatalogHelper->>DAOFactory: getDAO()
        DAOFactory-->>CatalogHelper: CloudscapeCatalogDAO Instance
        CatalogHelper->>CloudscapeDAO: getProducts("FISH", 0, 2, Locale.US)
        CloudscapeDAO->>DataSource: getConnection()
        DataSource-->>CloudscapeDAO: java.sql.Connection
        CloudscapeDAO->>Database: PreparedStatement.executeQuery("SELECT ... FROM product WHERE catid=?")
        Database-->>CloudscapeDAO: ResultSet (Scrollable)
        CloudscapeDAO->>CloudscapeDAO: Map rows to Page(List<Product>, start=0, hasNext=true)
        CloudscapeDAO-->>CatalogHelper: Page Object
    else Fallback to EJB Mode
        CatalogHelper->>CatalogHelper: getCatalogEJB().getProducts(...)
    end

    CatalogHelper-->>CategoryJSP: Return Page(List<Product>)
    CategoryJSP->>CategoryJSP: Render HTML table with JSTL <c:forEach>
    CategoryJSP-->>Browser: Return 200 OK HTML
```

---

## 2. Shopping Cart Conversational State Sequence Diagram

Demonstrates Stateful Session Bean (`ShoppingCartLocalEJB`) state tracking across user actions.

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant MainServlet as MainServlet (Front Controller)
    participant WebController as ShoppingWebController
    participant CartAction as CartAction
    participant Facade as ShoppingClientFacadeLocalEJB (SFSB)
    participant CartEJB as ShoppingCartLocalEJB (SFSB)
    participant CatalogDAO as CloudscapeCatalogDAO

    Browser->>MainServlet: POST /petstore/cart.do?action=add&itemId=EST-1
    MainServlet->>WebController: handleRequest(HttpServletRequest)
    WebController->>CartAction: perform(HttpServletRequest)
    
    CartAction->>CatalogDAO: getItem("EST-1", Locale.US)
    CatalogDAO-->>CartAction: Item Details (Angelfish Male, $16.50)
    
    CartAction->>Facade: getCart()
    Facade-->>CartAction: ShoppingCartLocal Reference
    
    CartAction->>CartEJB: addItem(item, product, quantity=1)
    CartEJB->>CartEJB: Update in-memory CartItem Map & calculate subtotal
    CartEJB-->>CartAction: Success
    
    CartAction-->>WebController: EventResponse (SUCCESS)
    WebController-->>MainServlet: Target Screen = "cart"
    MainServlet-->>Browser: HTTP 302 Redirect to /petstore/cart.screen
    
    Browser->>MainServlet: GET /petstore/cart.screen (with Cookie JSESSIONID)
    MainServlet-->>Browser: Render Shopping Cart View (Subtotal: $16.50, Count: 1)
```

---

## 3. User Authentication & Sign-On Sequence Diagram

Demonstrates credential verification and session association.

```mermaid
sequenceDiagram
    autonumber
    actor Browser as User Browser
    participant SignOnFilter as SignOnFilter
    participant MainServlet as MainServlet
    participant SignOnAction as SignOnAction
    participant SignOnEJB as SignOnLocalEJB (SLSB)
    participant UserEJB as UserLocalEJB (CMP Entity)
    participant CustomerEJB as CustomerLocalEJB (CMP Entity)
    participant Facade as ShoppingClientFacadeLocalEJB

    Browser->>MainServlet: POST /petstore/signon.do (userId="j2ee", password="j2ee")
    SignOnFilter->>MainServlet: Pass through filter chain
    MainServlet->>SignOnAction: perform(request)
    
    SignOnAction->>SignOnEJB: authenticate("j2ee", "j2ee")
    SignOnEJB->>UserEJB: findByPrimaryKey("j2ee")
    UserEJB-->>SignOnEJB: UserLocal (password="j2ee")
    SignOnEJB->>SignOnEJB: Match plaintext passwords
    SignOnEJB-->>SignOnAction: Authentication Approved
    
    SignOnAction->>CustomerEJB: findByPrimaryKey("j2ee")
    CustomerEJB-->>SignOnAction: Customer Details & Preferences
    
    SignOnAction->>Facade: setCustomer(CustomerLocal)
    SignOnAction-->>MainServlet: EventResponse(SUCCESS)
    MainServlet-->>Browser: HTTP 302 Redirect to /petstore/main.screen (User Authenticated)
```

---

## 4. End-to-End Order Placement & Asynchronous JMS OPC Flow

Demonstrates the asynchronous messaging decoupling between Storefront, Order Processing Center (OPC), and Supplier.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Shopper
    participant WebTier as MainServlet / OrderAction
    participant Facade as ShoppingClientFacadeLocalEJB
    participant Controller as ShoppingControllerLocalEJB
    participant UIDGen as UniqueIdGeneratorEJB
    participant AsyncSender as AsyncSenderLocalEJB
    participant OrderQueue as jms/opc/OrderQueue
    participant OrderApprovalMDB as OrderApprovalMDB (OPC)
    participant POQueue as jms/supplier/PurchaseOrderQueue
    participant SupplierOrderMDB as SupplierOrderMDB (Supplier)
    participant InvoiceTopic as jms/opc/InvoiceTopic
    participant MailInvoiceMDB as MailInvoiceMDB (OPC)
    participant MailQueue as jms/opc/MailQueue

    Customer->>WebTier: POST /petstore/order.do (Payment, Shipping Info)
    WebTier->>Facade: createOrder(shippingAddr, billingAddr, creditCard, "en_US")
    Facade->>Controller: createOrder(userId, cart, addresses, creditCard)
    Controller->>UIDGen: getNextId("ORDER_ID")
    UIDGen-->>Controller: Return new Order ID "1001"
    
    Controller->>AsyncSender: sendOrder(OrderObject)
    AsyncSender->>OrderQueue: send(ObjectMessage: Order 1001)
    Controller-->>Facade: Order Status: PENDING
    Facade-->>WebTier: Order Created
    WebTier-->>Customer: Render Order Confirmation Screen (Order #1001)

    %% Asynchronous OPC Background Worker
    Note over OrderQueue,OrderApprovalMDB: Asynchronous Validation in OPC
    OrderQueue->>OrderApprovalMDB: onMessage(Order 1001)
    OrderApprovalMDB->>OrderApprovalMDB: Check Credit Card & Verify Order Total
    OrderApprovalMDB->>POQueue: send(ObjectMessage: PurchaseOrder 1001)

    %% Asynchronous Supplier Background Worker
    Note over POQueue,SupplierOrderMDB: Asynchronous Supplier Fulfillment
    POQueue->>SupplierOrderMDB: onMessage(PurchaseOrder 1001)
    SupplierOrderMDB->>SupplierOrderMDB: Deduct Warehouse Inventory & Pack Order
    SupplierOrderMDB->>InvoiceTopic: publish(ObjectMessage: Invoice 1001)

    %% Asynchronous Notification Fan-Out
    Note over InvoiceTopic,MailInvoiceMDB: Asynchronous Notification Distribution
    InvoiceTopic->>MailInvoiceMDB: onMessage(Invoice 1001)
    MailInvoiceMDB->>MailQueue: send(TextMessage: "Your order #1001 has shipped!")
```
