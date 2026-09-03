# Running the Legacy Java Pet Store 1.3.1_02 Baseline (Docker Guide)

This guide documents how to run and test the authentic **Java Pet Store 1.3.1_02 (J2EE 1.3 / EJB 2.0)** baseline application in a containerized environment.

---

## 1. Prerequisites

- **Docker Desktop** (macOS, Linux, or Windows) installed and running.
- No local Java installation or J2EE app server required on the host system (all runtimes are self-contained inside the Docker image).

---

## 2. Quick Start

Run the automated build and launch script from the project root:

```bash
./docker/run_docker.sh
```

This single command will:
1. Build the lightweight Docker image `petstore-authentic-2002` (Ubuntu + OpenJDK 8 + Apache TomEE 1.7.5 Plus J2EE runtime).
2. Deploy the pristine baseline EAR archives (`petstore.ear`, `opc.ear`, `petstoreadmin.ear`, `supplier.ear`).
3. Initialize the embedded SQL database with the authentic 2002 seed data.
4. Expose the HTTP and JMS ports on `localhost:8000` (and `8088`).

To check container logs:
```bash
docker logs -f petstore-baseline
```

To stop the container:
```bash
docker stop petstore-baseline
```

To restart the container:
```bash
docker start petstore-baseline
```

---

## 3. Application URLs & Default Credentials

| Portal / Module | URL | Default Username | Default Password | Role / Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| **PetStore Storefront** | [http://localhost:8000/petstore/](http://localhost:8000/petstore/) | `j2ee`<br>`shopper`<br>`j2ee-ja`<br>`j2ee-zh` | `j2ee`<br>`j2ee`<br>`j2ee`<br>`j2ee` | Browse pet catalog, change languages (EN/JA/ZH), add items to cart, checkout, profile |
| **Admin Module (Web)** | [http://localhost:8000/admin/AdminRequestProcessor](http://localhost:8000/admin/AdminRequestProcessor) | `jps_admin`<br>`admin` | `admin`<br>`admin` | Admin web landing page / Web Start JNLP descriptor endpoint |
| **Admin Rich Client (GUI)** | `./run_admin_client.sh` | Authenticates automatically | Authenticates automatically | Java Swing desktop client: order approval, revenue reports, sales statistics |
| **Supplier Portal** | [http://localhost:8000/supplier/RcvrRequestProcessor](http://localhost:8000/supplier/RcvrRequestProcessor) | `supplier` | `supplier` | Purchase order receiver, inventory restocking & back-order fulfillment |

---

## 4. End-to-End Test Journey

### A. Catalog Browsing, Languages & Cart Management
1. **Catalog Home**: Navigate to [http://localhost:8000/petstore/main.screen](http://localhost:8000/petstore/main.screen).
2. **Language Switching**: Click the Japanese (`ja_JP`), Chinese (`zh_CN`), or US (`en_US`) flag in the top banner. Notice product titles and categories dynamically render localized descriptions and prices.
3. **Category Listing**: Click **Fish** -> [http://localhost:8000/petstore/category.screen?category_id=FISH](http://localhost:8000/petstore/category.screen?category_id=FISH).
4. **Product Details**: Click **Angelfish** -> [http://localhost:8000/petstore/product.screen?product_id=FI-SW-01](http://localhost:8000/petstore/product.screen?product_id=FI-SW-01).
5. **Add to Cart**: Click **Add to Cart** on item `EST-1` (Large Angelfish, $16.50).
6. **Manage Cart**: Update item quantities or remove items directly in the shopping cart table.

### B. Sign-In & Checkout
1. Click **Check Out** or navigate to [http://localhost:8000/petstore/signon_welcome.screen](http://localhost:8000/petstore/signon_welcome.screen).
2. Sign in with customer credentials: **`j2ee`** / **`j2ee`**.
3. The **Order Information** page displays pre-populated customer address and credit card details from the entity bean profile.
4. Submit the order to generate a new purchase order.

### C. Admin & Supplier Order Processing
1. **Launch Admin Desktop GUI**: Run `./run_admin_client.sh` in your terminal. This automatically logs into the container as `jps_admin` and launches the authentic Java Swing administration dashboard window.
2. **Supplier Restocking**: Open Supplier portal at [http://localhost:8000/supplier/RcvrRequestProcessor](http://localhost:8000/supplier/RcvrRequestProcessor).
3. Log in with **`supplier`** / **`supplier`**, click **Display Inventory**, adjust any item quantities (e.g. `EST-1` to `12000`), check the box, and click **Submit**. Inventory is updated and back orders are automatically fulfilled.

---

## 5. Architecture Summary

- **Web Tier**: Java Servlets (WAF framework), JSP 1.2 with custom taglibs (`waf:`, `c:`, `fmt:`).
- **EJB Tier**: Stateful Session Beans (Shopping Controller, Shopping Cart, Shopping Client Facade), Stateless Session Beans (Catalog, SignOn, OPC Order Processor), CMP Entity Beans (Customer, Account, Profile, Inventory).
- **FastLane Reader Pattern**: Direct JDBC SQL queries for catalog browsing using `CloudscapeCatalogDAO`.
- **Messaging (JMS)**: ActiveMQ resource adapter with async queues (`OrderQueue`, `OrderApprovalQueue`, `PurchaseOrderQueue`, `InvoiceTopic`).
- **Persistence**: Embedded HSQLDB relational database mapped to J2EE standard DataSources (`jdbc/CatalogDB`, `jdbc/petstore/PetStoreDB`, `jdbc/opc/OPCDB`, `jdbc/supplier/SupplierDB`).
