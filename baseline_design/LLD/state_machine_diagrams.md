# Low-Level Design (LLD): State Machine Diagrams

This document illustrates the state transitions for the core business entities in the 2002 Java Pet Store:
1. **Purchase Order State Machine**
2. **Stateful Session Bean (SFSB) Shopping Cart Lifecycle**
3. **User Authentication & Session State Machine**

---

## 1. Purchase Order State Machine Diagram

Captures the lifecycle of an order from initial creation in the storefront, asynchronous validation in the OPC, through supplier fulfillment and final completion.

```mermaid
stateDiagram-v2
    [*] --> PENDING : Shopper clicks "Submit Order" (ShoppingControllerLocalEJB)
    
    state PENDING {
        [*] --> OrderPlaced
        OrderPlaced --> QueuedInJMS : AsyncSenderEJB publishes to jms/opc/OrderQueue
    }

    PENDING --> VERIFYING : OrderApprovalMDB consumes message
    
    state VERIFYING {
        [*] --> ValidateCreditCard
        ValidateCreditCard --> CheckInventory
    }

    VERIFYING --> REJECTED : Credit Card Invalid or Limit Exceeded
    VERIFYING --> APPROVED : Validation Succeeded (Publishes to OrderApprovalQueue)

    state REJECTED {
        [*] --> SendRejectionEmail : MailQueue notification sent
        SendRejectionEmail --> [*]
    }

    APPROVED --> PROCESSING : PurchaseOrderMDB creates PO for Supplier
    
    state PROCESSING {
        [*] --> DispatchedToSupplier : Sent to jms/supplier/PurchaseOrderQueue
        DispatchedToSupplier --> SupplierStockAllocated : SupplierOrderMDB deducts stock
    }

    PROCESSING --> SHIPPED : Supplier generates Invoice & publishes to jms/opc/InvoiceTopic
    
    state SHIPPED {
        [*] --> InvoiceRecorded : InvoiceMDB updates OPC DB
        InvoiceRecorded --> SendShippingEmail : MailInvoiceMDB posts to MailQueue
    }

    SHIPPED --> COMPLETED : Customer receives package & payment finalized
    REJECTED --> [*]
    COMPLETED --> [*]
```

---

## 2. Stateful Session Bean (SFSB) Shopping Cart Lifecycle

Captures the container-managed lifecycle of `ShoppingCartLocalEJB` and `ShoppingClientFacadeLocalEJB` per HTTP session.

```mermaid
stateDiagram-v2
    [*] --> DOES_NOT_EXIST : User initiates HTTP request
    
    DOES_NOT_EXIST --> CREATED : ejbCreate() via ShoppingClientFacade
    
    state CREATED {
        [*] --> EMPTY_CART : items Map initialized
    }

    EMPTY_CART --> ACTIVE_CART : addItem(item, product, qty)
    
    state ACTIVE_CART {
        [*] --> ItemAdded
        ItemAdded --> QuantityUpdated : updateItemQuantity(itemId, newQty)
        QuantityUpdated --> ItemRemoved : deleteItem(itemId)
        ItemRemoved --> [*] : items.isEmpty()
    }

    ACTIVE_CART --> EMPTY_CART : All items removed or empty() called
    ACTIVE_CART --> CHECKOUT_LOCKED : createOrder() initiated

    state CHECKOUT_LOCKED {
        [*] --> OrderModelGenerated
        OrderModelGenerated --> CartEmptied : cart.empty()
    }

    CHECKOUT_LOCKED --> EMPTY_CART : Cart reset for next shopping session
    
    ACTIVE_CART --> PASSIVATED : ejbPassivate() (Container swaps bean state to disk due to idle timeout / memory pressure)
    PASSIVATED --> ACTIVE_CART : ejbActivate() (User resumes HTTP interaction)
    
    ACTIVE_CART --> DESTROYED : ejbRemove() / HTTP Session Invalidated (30 min timeout)
    EMPTY_CART --> DESTROYED : ejbRemove() / Session Timeout
    PASSIVATED --> DESTROYED : Session Expired in storage
    DESTROYED --> [*]
```

---

## 3. User Authentication & Session State Machine

```mermaid
stateDiagram-v2
    [*] --> ANONYMOUS : Guest user visits storefront

    state ANONYMOUS {
        [*] --> BrowseCatalog
        BrowseCatalog --> ManageGuestCart : Add items without login
    }

    ANONYMOUS --> AUTHENTICATING : Submits credentials on /petstore/signon.do
    
    state AUTHENTICATING {
        [*] --> QueryUserEJB
        QueryUserEJB --> VerifyPassword
    }

    AUTHENTICATING --> ANONYMOUS : Password mismatch (Error message rendered)
    AUTHENTICATING --> AUTHENTICATED : Password matches (Customer context attached)

    state AUTHENTICATED {
        [*] --> LoadCustomerProfile : Load language & banner preferences
        LoadCustomerProfile --> PopulateBillingAddress : Autofill checkout forms
        PopulateBillingAddress --> ExecuteAuthenticatedActions
    }

    AUTHENTICATED --> ANONYMOUS : User clicks "Sign Out" (/petstore/signoff.do)
    AUTHENTICATED --> SESSION_EXPIRED : Inactivity timeout (> 30 minutes)
    
    SESSION_EXPIRED --> ANONYMOUS : Redirect to signon.screen on next request
    ANONYMOUS --> [*]
```
