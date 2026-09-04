# Java Pet Store 1.3.1_02 - Complete Legacy UI & HTTP Reference Catalog

This directory contains the authentic UI screen snapshots, HTTP requests, and HTTP responses captured from the running 2002 J2EE baseline container environment (`http://localhost:8000/`).

This catalog serves as the **functional and interface contract reference** for the **Phase 2 Spring Boot 3.3.x + MongoDB + Apache Kafka** modernization.

---

## Directory Structure

```text
legacy_reference/
├── storefront/              # Storefront screens, catalog, cart, languages, and checkout
│   ├── 01_welcome/          # Splash screen (GET /petstore/)
│   ├── 02_main_catalog/     # Main catalog home (GET /petstore/main.screen)
│   ├── 03_category_*/       # Category listings (Fish, Dogs, Cats, Birds, Reptiles)
│   ├── 04_product_*/        # Product pages (Angelfish, Bulldog, Persian, etc.)
│   ├── 05_item_*/           # Individual SKU detail pages
│   ├── 06_language_*/       # Multi-language locale switching (EN, JA, ZH)
│   ├── 07_cart_*/           # Shopping cart add, update, and delete actions
│   ├── 08_auth_*/           # Customer authentication and user profile screens
│   └── 09_checkout_*/       # Order placement and checkout commitment
├── supplier/                # Supplier inventory management portal
│   ├── 01_login_landing/    # Supplier form login
│   ├── 02_auth_submit/      # Form auth POST (j_security_check)
│   ├── 03_inventory_display/# Current stock level table
│   ├── 04_inventory_update/ # Restocking submission (RcvrRequestProcessor)
│   └── 05_logout/           # Supplier session termination
├── admin/                   # Administration portal & XML Web Services
│   ├── 01_login_landing/    # Admin form login
│   ├── 02_auth_submit/      # Form auth POST (j_security_check)
│   ├── 03_jnlp_descriptor/  # Java Web Start JNLP descriptor
│   ├── 04_api_get_orders_*/ # XML APIs: Query Pending, Approved, Completed orders
│   ├── 07_api_revenue_stats/# XML API: Revenue analytics by date range
│   └── 08_api_order_counts/ # XML API: Order counts by date range
└── static_assets/           # Extracted 2002 images, flags, category banners, icons
```

---

## Summary of Captured Endpoints & Interactions

| Category | Screen / Interaction | HTTP Method | Path / Endpoint | Request Payload / Params | Response Format |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Storefront** | `01_welcome`<br>Storefront entry splash screen | `GET` | `/petstore/` | `None` | [response.html](storefront/01_welcome/response.html) (200) |
| **Storefront** | `02_main_catalog`<br>Main catalog screen with featured categories | `GET` | `/petstore/main.screen` | `None` | [response.html](storefront/02_main_catalog/response.html) (200) |
| **Storefront** | `03_category_fish`<br>Category listing page for FISH | `GET` | `/petstore/category.screen?category_id=FISH` | `None` | [response.html](storefront/03_category_fish/response.html) (200) |
| **Storefront** | `03_category_dogs`<br>Category listing page for DOGS | `GET` | `/petstore/category.screen?category_id=DOGS` | `None` | [response.html](storefront/03_category_dogs/response.html) (200) |
| **Storefront** | `03_category_cats`<br>Category listing page for CATS | `GET` | `/petstore/category.screen?category_id=CATS` | `None` | [response.html](storefront/03_category_cats/response.html) (200) |
| **Storefront** | `03_category_birds`<br>Category listing page for BIRDS | `GET` | `/petstore/category.screen?category_id=BIRDS` | `None` | [response.html](storefront/03_category_birds/response.html) (200) |
| **Storefront** | `03_category_reptiles`<br>Category listing page for REPTILES | `GET` | `/petstore/category.screen?category_id=REPTILES` | `None` | [response.html](storefront/03_category_reptiles/response.html) (200) |
| **Storefront** | `04_product_angelfish`<br>Product details page for angelfish (FI-SW-01) | `GET` | `/petstore/product.screen?product_id=FI-SW-01` | `None` | [response.html](storefront/04_product_angelfish/response.html) (200) |
| **Storefront** | `04_product_tiger_shark`<br>Product details page for tiger_shark (FI-SW-02) | `GET` | `/petstore/product.screen?product_id=FI-SW-02` | `None` | [response.html](storefront/04_product_tiger_shark/response.html) (200) |
| **Storefront** | `04_product_koi`<br>Product details page for koi (FI-FW-01) | `GET` | `/petstore/product.screen?product_id=FI-FW-01` | `None` | [response.html](storefront/04_product_koi/response.html) (200) |
| **Storefront** | `04_product_goldfish`<br>Product details page for goldfish (FI-FW-02) | `GET` | `/petstore/product.screen?product_id=FI-FW-02` | `None` | [response.html](storefront/04_product_goldfish/response.html) (200) |
| **Storefront** | `04_product_bulldog`<br>Product details page for bulldog (K9-BD-01) | `GET` | `/petstore/product.screen?product_id=K9-BD-01` | `None` | [response.html](storefront/04_product_bulldog/response.html) (200) |
| **Storefront** | `04_product_poodle`<br>Product details page for poodle (K9-PO-02) | `GET` | `/petstore/product.screen?product_id=K9-PO-02` | `None` | [response.html](storefront/04_product_poodle/response.html) (200) |
| **Storefront** | `04_product_dalmatian`<br>Product details page for dalmatian (K9-DL-01) | `GET` | `/petstore/product.screen?product_id=K9-DL-01` | `None` | [response.html](storefront/04_product_dalmatian/response.html) (200) |
| **Storefront** | `04_product_golden_retriever`<br>Product details page for golden_retriever (K9-RT-01) | `GET` | `/petstore/product.screen?product_id=K9-RT-01` | `None` | [response.html](storefront/04_product_golden_retriever/response.html) (200) |
| **Storefront** | `04_product_labrador_retriever`<br>Product details page for labrador_retriever (K9-RT-02) | `GET` | `/petstore/product.screen?product_id=K9-RT-02` | `None` | [response.html](storefront/04_product_labrador_retriever/response.html) (200) |
| **Storefront** | `04_product_manx`<br>Product details page for manx (FL-DSH-01) | `GET` | `/petstore/product.screen?product_id=FL-DSH-01` | `None` | [response.html](storefront/04_product_manx/response.html) (200) |
| **Storefront** | `04_product_persian`<br>Product details page for persian (FL-DLH-02) | `GET` | `/petstore/product.screen?product_id=FL-DLH-02` | `None` | [response.html](storefront/04_product_persian/response.html) (200) |
| **Storefront** | `04_product_amazon_parrot`<br>Product details page for amazon_parrot (AV-CB-01) | `GET` | `/petstore/product.screen?product_id=AV-CB-01` | `None` | [response.html](storefront/04_product_amazon_parrot/response.html) (200) |
| **Storefront** | `04_product_finch`<br>Product details page for finch (AV-SB-02) | `GET` | `/petstore/product.screen?product_id=AV-SB-02` | `None` | [response.html](storefront/04_product_finch/response.html) (200) |
| **Storefront** | `04_product_rattlesnake`<br>Product details page for rattlesnake (RP-SN-01) | `GET` | `/petstore/product.screen?product_id=RP-SN-01` | `None` | [response.html](storefront/04_product_rattlesnake/response.html) (200) |
| **Storefront** | `04_product_iguana`<br>Product details page for iguana (RP-LI-02) | `GET` | `/petstore/product.screen?product_id=RP-LI-02` | `None` | [response.html](storefront/04_product_iguana/response.html) (200) |
| **Storefront** | `05_item_large_angelfish`<br>Item details page for large_angelfish (EST-1) | `GET` | `/petstore/item.screen?item_id=EST-1` | `None` | [response.html](storefront/05_item_large_angelfish/response.html) (200) |
| **Storefront** | `05_item_small_angelfish`<br>Item details page for small_angelfish (EST-2) | `GET` | `/petstore/item.screen?item_id=EST-2` | `None` | [response.html](storefront/05_item_small_angelfish/response.html) (200) |
| **Storefront** | `05_item_toothless_tiger_shark`<br>Item details page for toothless_tiger_shark (EST-3) | `GET` | `/petstore/item.screen?item_id=EST-3` | `None` | [response.html](storefront/05_item_toothless_tiger_shark/response.html) (200) |
| **Storefront** | `05_item_spotted_koi`<br>Item details page for spotted_koi (EST-4) | `GET` | `/petstore/item.screen?item_id=EST-4` | `None` | [response.html](storefront/05_item_spotted_koi/response.html) (200) |
| **Storefront** | `05_item_spotless_koi`<br>Item details page for spotless_koi (EST-5) | `GET` | `/petstore/item.screen?item_id=EST-5` | `None` | [response.html](storefront/05_item_spotless_koi/response.html) (200) |
| **Storefront** | `05_item_male_adult_bulldog`<br>Item details page for male_adult_bulldog (EST-6) | `GET` | `/petstore/item.screen?item_id=EST-6` | `None` | [response.html](storefront/05_item_male_adult_bulldog/response.html) (200) |
| **Storefront** | `05_item_female_puppy_bulldog`<br>Item details page for female_puppy_bulldog (EST-7) | `GET` | `/petstore/item.screen?item_id=EST-7` | `None` | [response.html](storefront/05_item_female_puppy_bulldog/response.html) (200) |
| **Storefront** | `05_item_male_puppy_poodle`<br>Item details page for male_puppy_poodle (EST-8) | `GET` | `/petstore/item.screen?item_id=EST-8` | `None` | [response.html](storefront/05_item_male_puppy_poodle/response.html) (200) |
| **Storefront** | `05_item_spotless_male_puppy_dalmatian`<br>Item details page for spotless_male_puppy_dalmatian (EST-9) | `GET` | `/petstore/item.screen?item_id=EST-9` | `None` | [response.html](storefront/05_item_spotless_male_puppy_dalmatian/response.html) (200) |
| **Storefront** | `05_item_spotted_adult_female_dalmatian`<br>Item details page for spotted_adult_female_dalmatian (EST-10) | `GET` | `/petstore/item.screen?item_id=EST-10` | `None` | [response.html](storefront/05_item_spotted_adult_female_dalmatian/response.html) (200) |
| **Storefront** | `05_item_tailless_manx`<br>Item details page for tailless_manx (EST-11) | `GET` | `/petstore/item.screen?item_id=EST-11` | `None` | [response.html](storefront/05_item_tailless_manx/response.html) (200) |
| **Storefront** | `05_item_with_tail_manx`<br>Item details page for with_tail_manx (EST-12) | `GET` | `/petstore/item.screen?item_id=EST-12` | `None` | [response.html](storefront/05_item_with_tail_manx/response.html) (200) |
| **Storefront** | `05_item_adult_female_persian`<br>Item details page for adult_female_persian (EST-13) | `GET` | `/petstore/item.screen?item_id=EST-13` | `None` | [response.html](storefront/05_item_adult_female_persian/response.html) (200) |
| **Storefront** | `05_item_adult_male_amazon_parrot`<br>Item details page for adult_male_amazon_parrot (EST-14) | `GET` | `/petstore/item.screen?item_id=EST-14` | `None` | [response.html](storefront/05_item_adult_male_amazon_parrot/response.html) (200) |
| **Storefront** | `05_item_adult_male_finch`<br>Item details page for adult_male_finch (EST-15) | `GET` | `/petstore/item.screen?item_id=EST-15` | `None` | [response.html](storefront/05_item_adult_male_finch/response.html) (200) |
| **Storefront** | `05_item_rattleless_rattlesnake`<br>Item details page for rattleless_rattlesnake (EST-16) | `GET` | `/petstore/item.screen?item_id=EST-16` | `None` | [response.html](storefront/05_item_rattleless_rattlesnake/response.html) (200) |
| **Storefront** | `05_item_rattle_rattlesnake`<br>Item details page for rattle_rattlesnake (EST-17) | `GET` | `/petstore/item.screen?item_id=EST-17` | `None` | [response.html](storefront/05_item_rattle_rattlesnake/response.html) (200) |
| **Storefront** | `05_item_green_adult_iguana`<br>Item details page for green_adult_iguana (EST-18) | `GET` | `/petstore/item.screen?item_id=EST-18` | `None` | [response.html](storefront/05_item_green_adult_iguana/response.html) (200) |
| **Storefront** | `06_language_ja_JP`<br>Switch UI language to Japanese (ja_JP) | `POST` | `/petstore/changelocale.do` | `locale=ja_JP&referring_URL=main.screen...` | [response.html](storefront/06_language_ja_JP/response.html) (200) |
| **Storefront** | `06_language_ja_JP_catalog`<br>Catalog screen in Japanese | `GET` | `/petstore/main.screen` | `None` | [response.html](storefront/06_language_ja_JP_catalog/response.html) (200) |
| **Storefront** | `06_language_zh_CN`<br>Switch UI language to Chinese (zh_CN) | `POST` | `/petstore/changelocale.do` | `locale=zh_CN&referring_URL=main.screen...` | [response.html](storefront/06_language_zh_CN/response.html) (200) |
| **Storefront** | `06_language_zh_CN_catalog`<br>Catalog screen in Chinese | `GET` | `/petstore/main.screen` | `None` | [response.html](storefront/06_language_zh_CN_catalog/response.html) (200) |
| **Storefront** | `06_language_en_US`<br>Switch UI language back to English (en_US) | `POST` | `/petstore/changelocale.do` | `locale=en_US&referring_URL=main.screen...` | [response.html](storefront/06_language_en_US/response.html) (200) |
| **Storefront** | `07_cart_view_empty`<br>Empty shopping cart view | `GET` | `/petstore/cart.screen` | `None` | [response.html](storefront/07_cart_view_empty/response.html) (200) |
| **Storefront** | `07_cart_add_item_EST-1`<br>Add Large Angelfish (EST-1) to cart | `POST` | `/petstore/cart.do?action=purchase&itemId=EST-1` | `action=purchase&itemId=EST-1...` | [response.html](storefront/07_cart_add_item_EST-1/response.html) (200) |
| **Storefront** | `07_cart_add_item_EST-6`<br>Add Male Adult Bulldog (EST-6) to cart | `POST` | `/petstore/cart.do?action=purchase&itemId=EST-6` | `action=purchase&itemId=EST-6...` | [response.html](storefront/07_cart_add_item_EST-6/response.html) (200) |
| **Storefront** | `07_cart_update_quantity`<br>Update quantity of EST-1 to 3 | `POST` | `/petstore/cart.do?action=update&itemId=EST-1&quantity=3` | `action=update&itemId=EST-1...` | [response.html](storefront/07_cart_update_quantity/response.html) (200) |
| **Storefront** | `07_cart_delete_item`<br>Remove EST-6 from cart | `POST` | `/petstore/cart.do?action=delete&itemId=EST-6` | `action=delete&itemId=EST-6...` | [response.html](storefront/07_cart_delete_item/response.html) (200) |
| **Storefront** | `08_auth_signin_welcome`<br>Sign-in welcome landing page | `GET` | `/petstore/signon_welcome.screen` | `None` | [response.html](storefront/08_auth_signin_welcome/response.html) (200) |
| **Storefront** | `08_auth_signin_form`<br>Customer login form | `GET` | `/petstore/signon.screen` | `None` | [response.html](storefront/08_auth_signin_form/response.html) (200) |
| **Storefront** | `08_auth_login_submit`<br>Customer authentication POST (j2ee/j2ee) | `POST` | `/petstore/j_signon_check` | `j_username=j2ee&j_password=j2ee...` | [response.txt](storefront/08_auth_login_submit/response.txt) (404) |
| **Storefront** | `08_auth_user_profile`<br>Customer account profile view | `GET` | `/petstore/userprofile.screen` | `None` | [response.html](storefront/08_auth_user_profile/response.html) (200) |
| **Storefront** | `09_checkout_add_cart`<br>Add item to cart before checkout | `POST` | `/petstore/cart.do?action=purchase&itemId=EST-1` | `action=purchase&itemId=EST-1...` | [response.html](storefront/09_checkout_add_cart/response.html) (200) |
| **Storefront** | `09_checkout_order_form`<br>Order information screen with billing and shipping address | `GET` | `/petstore/order.screen` | `None` | [response.html](storefront/09_checkout_order_form/response.html) (200) |
| **Storefront** | `09_checkout_order_submit`<br>Submit order form with shipping, billing, and credit card details | `POST` | `/petstore/order.do` | `family_name_a=Duke&given_name_a=Java...` | [response.html](storefront/09_checkout_order_submit/response.html) (200) |
| **Supplier** | `01_login_landing`<br>Supplier login form landing page | `GET` | `/supplier/RcvrRequestProcessor` | `None` | [response.html](supplier/01_login_landing/response.html) (200) |
| **Supplier** | `02_auth_submit`<br>Supplier authentication POST (supplier/supplier) | `POST` | `/supplier/j_security_check` | `j_username=supplier&j_password=supplier...` | [response.html](supplier/02_auth_submit/response.html) (200) |
| **Supplier** | `03_inventory_display`<br>Supplier display inventory list | `POST` | `/supplier/RcvrRequestProcessor` | `currentScreen=displayinventory...` | [response.html](supplier/03_inventory_display/response.html) (200) |
| **Supplier** | `04_inventory_update_stock`<br>Supplier restock item EST-1 to 15000 units | `POST` | `/supplier/RcvrRequestProcessor` | `currentScreen=updateinventory&item_EST-1=on...` | [response.html](supplier/04_inventory_update_stock/response.html) (200) |
| **Supplier** | `05_logout`<br>Supplier logout action | `POST` | `/supplier/RcvrRequestProcessor` | `currentScreen=logout...` | [response.html](supplier/05_logout/response.html) (200) |
| **Admin** | `01_login_landing`<br>Admin login landing page | `GET` | `/admin/AdminRequestProcessor` | `None` | [response.html](admin/01_login_landing/response.html) (200) |
| **Admin** | `02_auth_submit`<br>Admin authentication POST (jps_admin/admin) | `POST` | `/admin/j_security_check` | `j_username=jps_admin&j_password=admin...` | [response.html](admin/02_auth_submit/response.html) (200) |
| **Admin** | `03_jnlp_descriptor`<br>Java Web Start (JNLP) XML launch descriptor | `GET` | `/admin/AdminRequestProcessor?currentScreen=manageorders` | `None` | [response.html](admin/03_jnlp_descriptor/response.html) (200) |
| **Admin** | `04_api_get_orders_pending`<br>XML API: Query Pending Orders (Status=P) | `POST` | `/admin/ApplRequestProcessor` | `<PetStoreRequest><Type>GETORDE...` | [response.xml](admin/04_api_get_orders_pending/response.xml) (200) |
| **Admin** | `05_api_get_orders_approved`<br>XML API: Query Approved Orders (Status=A) | `POST` | `/admin/ApplRequestProcessor` | `<PetStoreRequest><Type>GETORDE...` | [response.xml](admin/05_api_get_orders_approved/response.xml) (200) |
| **Admin** | `06_api_get_orders_completed`<br>XML API: Query Completed Orders (Status=C) | `POST` | `/admin/ApplRequestProcessor` | `<PetStoreRequest><Type>GETORDE...` | [response.xml](admin/06_api_get_orders_completed/response.xml) (200) |
| **Admin** | `07_api_revenue_stats`<br>XML API: Query Revenue Analytics by Category and Date Range | `POST` | `/admin/ApplRequestProcessor` | `<PetStoreRequest><Type>REVENUE...` | [response.xml](admin/07_api_revenue_stats/response.xml) (200) |
| **Admin** | `08_api_order_counts`<br>XML API: Query Order Volume by Category and Date Range | `POST` | `/admin/ApplRequestProcessor` | `<PetStoreRequest><Type>ORDERS<...` | [response.xml](admin/08_api_order_counts/response.xml) (200) |

---

## How to Use This Reference in Phase 2 Migration

1. **REST API Contract Design**:
   - Compare new Spring Boot Controller request/response JSON schemas against the legacy URL parameters and XML payloads documented in `request_response.json`.
2. **Modern UI Component Parity**:
   - Inspect the captured `response.html` files in each folder alongside the original GIF assets in `static_assets/` to ensure 100% visual and functional parity in modern React / Thymeleaf templates.
3. **Automated Migration Testing**:
   - Use the recorded input forms and expected outputs in `request_response.json` as the baseline test fixtures for Phase 2 End-to-End integration tests.
