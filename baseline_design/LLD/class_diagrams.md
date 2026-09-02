# Low-Level Design (LLD): Class Diagrams

This document contains detailed class diagrams covering the four core layers of the 2002 Java Pet Store application:
1. **Web Application Framework (WAF) & Presentation Tier**
2. **Session Facade & Business Tier**
3. **FastLane Reader Catalog DAO Tier**
4. **CMP 2.0 Entity Bean Tier**

---

## 1. WAF Presentation Tier Class Diagram

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

    class CatalogHelper {
        -String categoryId
        -String productId
        -String itemId
        -int start
        -int count
        -Locale locale
        -boolean useFastLane
        -CatalogDAO dao
        +getProducts() Page
        +getItems() Page
        +getItem() Item
        +setCategoryId(String)
        +setProductId(String)
        +setStart(int)
        +setCount(int)
        +setLocale(String)
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

---

## 2. Session Facade & Business Tier Class Diagram

```mermaid
classDiagram
    class ShoppingClientFacadeLocal {
        <<interface>>
        +getCart() ShoppingCartLocal
        +createOrder(Address, Address, CreditCard, String) Order
        +getCustomer() CustomerLocal
        +setCustomer(CustomerLocal)
        +newOrder()
    }

    class ShoppingClientFacadeLocalEJB {
        -ShoppingCartLocal cart
        -CustomerLocal customer
        -ShoppingControllerLocal controller
        +ejbCreate()
        +getCart() ShoppingCartLocal
        +createOrder(Address, Address, CreditCard, String) Order
    }

    class ShoppingControllerLocal {
        <<interface>>
        +createOrder(String, ShoppingCartLocal, Address, Address, CreditCard) Order
        +getOrders(String) Collection
    }

    class ShoppingControllerLocalEJB {
        -AsyncSenderLocal asyncSender
        -UniqueIdGeneratorLocal uidGen
        +createOrder(String, ShoppingCartLocal, Address, Address, CreditCard) Order
        +sendOrder(Order)
    }

    class ShoppingCartLocal {
        <<interface>>
        +addItem(Item, Product, int)
        +deleteItem(String)
        +updateItemQuantity(String, int)
        +getItems() Collection
        +getSubTotal() double
        +empty()
    }

    class ShoppingCartLocalEJB {
        -Map items
        +addItem(Item, Product, int)
        +deleteItem(String)
        +updateItemQuantity(String, int)
        +getItems() Collection
        +getSubTotal() double
        +empty()
    }

    class AsyncSenderLocal {
        <<interface>>
        +sendOrder(Order)
    }

    class AsyncSenderLocalEJB {
        -QueueConnectionFactory qcf
        -Queue orderQueue
        +sendOrder(Order)
    }

    class UniqueIdGeneratorLocal {
        <<interface>>
        +getNextId(String) int
    }

    class UniqueIdGeneratorLocalEJB {
        -int currentId
        -int maxId
        -int blockSize
        +getNextId(String) int
        -reserveBlock(String)
    }

    ShoppingClientFacadeLocal <|.. ShoppingClientFacadeLocalEJB
    ShoppingControllerLocal <|.. ShoppingControllerLocalEJB
    ShoppingCartLocal <|.. ShoppingCartLocalEJB
    AsyncSenderLocal <|.. AsyncSenderLocalEJB
    UniqueIdGeneratorLocal <|.. UniqueIdGeneratorLocalEJB

    ShoppingClientFacadeLocalEJB --> ShoppingControllerLocal
    ShoppingClientFacadeLocalEJB --> ShoppingCartLocal
    ShoppingControllerLocalEJB --> AsyncSenderLocal
    ShoppingControllerLocalEJB --> UniqueIdGeneratorLocal
```

---

## 3. FastLane Reader Catalog DAO Pattern Class Diagram

The **Fast Lane Reader** pattern allows presentation-tier helpers to execute high-speed relational queries without EJB container overhead.

```mermaid
classDiagram
    class CatalogDAO {
        <<interface>>
        +getCategory(String, Locale) Category
        +getCategories(int, int, Locale) Page
        +getProduct(String, Locale) Product
        +getProducts(String, int, int, Locale) Page
        +getItem(String, Locale) Item
        +getItems(String, int, int, Locale) Page
        +searchItems(String, int, int, Locale) Page
    }

    class CatalogDAOFactory {
        +getDAO() CatalogDAO$
    }

    class CloudscapeCatalogDAO {
        +GET_CATEGORY_STATEMENT$ String
        +GET_CATEGORIES_STATEMENT$ String
        +GET_PRODUCT_STATEMENT$ String
        +GET_PRODUCTS_STATEMENT$ String
        +GET_ITEM_STATEMENT$ String
        +GET_ITEMS_STATEMENT$ String
        #getDataSource()$ DataSource
        +getCategory(String, Locale) Category
        +getProducts(String, int, int, Locale) Page
        +getItem(String, Locale) Item
        +getItems(String, int, int, Locale) Page
    }

    class GenericCatalogDAO {
        -URL sqlXmlUrl
        +getProducts(String, int, int, Locale) Page
        +getItems(String, int, int, Locale) Page
    }

    class Page {
        -List list
        -int start
        -boolean hasNext
        -boolean hasPrevious
        +getList() List
        +getStart() int
        +isNextPageAvailable() boolean
        +isPreviousPageAvailable() boolean
    }

    class Category {
        -String id
        -String name
        -String description
        -String image
        +getId() String
        +getName() String
    }

    class Product {
        -String id
        -String categoryId
        -String name
        -String description
        -String image
        +getId() String
        +getName() String
    }

    class Item {
        -String id
        -String productId
        -double listPrice
        -double unitCost
        -String image
        -String attribute1
        +getId() String
        +getListPrice() double
    }

    CatalogDAO <|.. CloudscapeCatalogDAO
    CatalogDAO <|.. GenericCatalogDAO
    CatalogDAOFactory ..> CatalogDAO
    CloudscapeCatalogDAO --> Page
    Page --> Product
    Page --> Item
    CatalogHelper --> CatalogDAOFactory
```

---

## 4. CMP 2.0 Entity Bean Tier Class Diagram

Container-Managed Persistence (CMP 2.0) defines abstract getter/setter methods managed by the EJB container.

```mermaid
classDiagram
    class UserLocal {
        <<interface>>
        +getUserName() String
        +getPassword() String
        +setPassword(String)
    }

    class UserEJB {
        <<abstract>>
        +getUserName()* String
        +setUserName(String)*
        +getPassword()* String
        +setPassword(String)*
        +ejbCreate(String, String) String
    }

    class CustomerLocal {
        <<interface>>
        +getUserId() String
        +getAccount() AccountLocal
        +getProfile() ProfileLocal
    }

    class CustomerEJB {
        <<abstract>>
        +getUserId()* String
        +getAccount()* AccountLocal
        +setAccount(AccountLocal)*
        +getProfile()* ProfileLocal
        +setProfile(ProfileLocal)*
    }

    class AccountLocal {
        <<interface>>
        +getContactInfo() ContactInfoLocal
        +getCreditCard() CreditCardLocal
        +getStatus() String
    }

    class AccountEJB {
        <<abstract>>
        +getContactInfo()* ContactInfoLocal
        +getCreditCard()* CreditCardLocal
        +getStatus()* String
    }

    class ProfileLocal {
        <<interface>>
        +getPreferredLanguage() String
        +getFavoriteCategory() String
        +getMyListPreference() boolean
        +getBannerPreference() boolean
    }

    class CreditCardLocal {
        <<interface>>
        +getCardNumber() String
        +getCardType() String
        +getExpiryDate() String
    }

    class AddressLocal {
        <<interface>>
        +getStreetName1() String
        +getStreetName2() String
        +getCity() String
        +getState() String
        +getZipCode() String
        +getCountry() String
    }

    class ContactInfoLocal {
        <<interface>>
        +getFamilyName() String
        +getGivenName() String
        +getEmail() String
        +getTelephone() String
        +getAddress() AddressLocal
    }

    UserLocal <|.. UserEJB
    CustomerLocal <|.. CustomerEJB
    AccountLocal <|.. AccountEJB
    CustomerEJB "1" --> "1" AccountLocal : 1-to-1 CMP
    CustomerEJB "1" --> "1" ProfileLocal : 1-to-1 CMP
    AccountEJB "1" --> "1" ContactInfoLocal : 1-to-1 CMP
    AccountEJB "1" --> "1" CreditCardLocal : 1-to-1 CMP
    ContactInfoLocal "1" --> "1" AddressLocal : 1-to-1 CMP
```
