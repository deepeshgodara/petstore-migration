import os
import json
import urllib.request
import urllib.parse
import http.cookiejar
import re

BASE_URL = "http://localhost:8000"
OUTPUT_DIR = "/Users/deepeshgodara/Documents/petstore1.3.1_02/legacy_reference"

class SmartRedirectHandler(urllib.request.HTTPRedirectHandler):
    def http_error_302(self, req, fp, code, msg, headers):
        newurl = headers.get("Location") or headers.get("location")
        if newurl:
            if not newurl.startswith("http"):
                newurl = BASE_URL + (newurl if newurl.startswith("/") else "/" + newurl)
            return self.parent.open(newurl)
        return self.parent.open(BASE_URL + "/petstore/userprofile.screen")
    http_error_301 = http_error_302
    http_error_303 = http_error_302
    http_error_307 = http_error_302

class HTTPRecorder:
    def __init__(self):
        self.cj = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cj), SmartRedirectHandler())
        self.records = []

    def record(self, category, name, method, path, headers=None, data=None, description=""):
        url = BASE_URL + path
        target_dir = os.path.join(OUTPUT_DIR, category, name)
        os.makedirs(target_dir, exist_ok=True)

        req_headers = {
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) PetStore-Migration-Recorder",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }
        if headers:
            req_headers.update(headers)

        encoded_data = None
        if data:
            if isinstance(data, dict):
                encoded_data = urllib.parse.urlencode(data).encode("utf-8")
                if "Content-Type" not in req_headers:
                    req_headers["Content-Type"] = "application/x-www-form-urlencoded"
            elif isinstance(data, str):
                encoded_data = data.encode("utf-8")

        req = urllib.request.Request(url, data=encoded_data, headers=req_headers, method=method)

        try:
            resp = self.opener.open(req)
            status_code = resp.status
            resp_headers = dict(resp.headers)
            body_bytes = resp.read()
            try:
                body_str = body_bytes.decode("utf-8")
            except:
                body_str = body_bytes.decode("iso-8859-1", errors="replace")
        except urllib.error.HTTPError as e:
            status_code = e.code
            resp_headers = dict(e.headers)
            body_bytes = e.read()
            try:
                body_str = body_bytes.decode("utf-8")
            except:
                body_str = body_bytes.decode("iso-8859-1", errors="replace")
        except Exception as e:
            status_code = 500
            resp_headers = {}
            body_str = f"Error capturing response: {str(e)}"

        ct = resp_headers.get("Content-Type", "")
        if "xml" in ct or (body_str.strip().startswith("<?xml") or body_str.strip().startswith("<Response>")):
            resp_file_name = "response.xml"
        elif "html" in ct or "<html" in body_str.lower():
            resp_file_name = "response.html"
        else:
            resp_file_name = "response.txt"

        with open(os.path.join(target_dir, resp_file_name), "w", encoding="utf-8") as f:
            f.write(body_str)

        record_entry = {
            "category": category,
            "name": name,
            "description": description,
            "request": {
                "method": method,
                "url": url,
                "path": path,
                "headers": req_headers,
                "body": data if isinstance(data, (dict, str)) else None
            },
            "response": {
                "status_code": status_code,
                "headers": resp_headers,
                "file": resp_file_name,
                "content_length": len(body_str)
            }
        }

        with open(os.path.join(target_dir, "request_response.json"), "w", encoding="utf-8") as f:
            json.dump(record_entry, f, indent=2)

        self.records.append(record_entry)
        print(f"[{status_code}] {method} {path} -> {category}/{name}")
        return body_str

def main():
    print("==========================================================")
    print(" Archiving Legacy Java Pet Store UI, Requests & Responses")
    print("==========================================================")

    # 1. STOREFRONT FLOWS
    recorder = HTTPRecorder()

    # Welcome & Catalog
    recorder.record("storefront", "01_welcome", "GET", "/petstore/", description="Storefront entry splash screen")
    recorder.record("storefront", "02_main_catalog", "GET", "/petstore/main.screen", description="Main catalog screen with featured categories")

    # Categories
    categories = ["FISH", "DOGS", "CATS", "BIRDS", "REPTILES"]
    for cat in categories:
        recorder.record("storefront", f"03_category_{cat.lower()}", "GET", f"/petstore/category.screen?category_id={cat}", description=f"Category listing page for {cat}")

    # Products
    products = [
        ("FI-SW-01", "angelfish"),
        ("FI-SW-02", "tiger_shark"),
        ("FI-FW-01", "koi"),
        ("FI-FW-02", "goldfish"),
        ("K9-BD-01", "bulldog"),
        ("K9-PO-02", "poodle"),
        ("K9-DL-01", "dalmatian"),
        ("K9-RT-01", "golden_retriever"),
        ("K9-RT-02", "labrador_retriever"),
        ("FL-DSH-01", "manx"),
        ("FL-DLH-02", "persian"),
        ("AV-CB-01", "amazon_parrot"),
        ("AV-SB-02", "finch"),
        ("RP-SN-01", "rattlesnake"),
        ("RP-LI-02", "iguana")
    ]
    for pid, pname in products:
        recorder.record("storefront", f"04_product_{pname}", "GET", f"/petstore/product.screen?product_id={pid}", description=f"Product details page for {pname} ({pid})")

    # Items
    items = [
        ("EST-1", "large_angelfish"),
        ("EST-2", "small_angelfish"),
        ("EST-3", "toothless_tiger_shark"),
        ("EST-4", "spotted_koi"),
        ("EST-5", "spotless_koi"),
        ("EST-6", "male_adult_bulldog"),
        ("EST-7", "female_puppy_bulldog"),
        ("EST-8", "male_puppy_poodle"),
        ("EST-9", "spotless_male_puppy_dalmatian"),
        ("EST-10", "spotted_adult_female_dalmatian"),
        ("EST-11", "tailless_manx"),
        ("EST-12", "with_tail_manx"),
        ("EST-13", "adult_female_persian"),
        ("EST-14", "adult_male_amazon_parrot"),
        ("EST-15", "adult_male_finch"),
        ("EST-16", "rattleless_rattlesnake"),
        ("EST-17", "rattle_rattlesnake"),
        ("EST-18", "green_adult_iguana")
    ]
    for iid, iname in items:
        recorder.record("storefront", f"05_item_{iname}", "GET", f"/petstore/item.screen?item_id={iid}", description=f"Item details page for {iname} ({iid})")

    # Multi-language switching
    recorder.record("storefront", "06_language_ja_JP", "POST", "/petstore/changelocale.do", 
                    data={"locale": "ja_JP", "referring_URL": "main.screen", "referring_screen": "main.screen", "cacheId": "petstore"},
                    description="Switch UI language to Japanese (ja_JP)")
    recorder.record("storefront", "06_language_ja_JP_catalog", "GET", "/petstore/main.screen", description="Catalog screen in Japanese")
    recorder.record("storefront", "06_language_zh_CN", "POST", "/petstore/changelocale.do", 
                    data={"locale": "zh_CN", "referring_URL": "main.screen", "referring_screen": "main.screen", "cacheId": "petstore"},
                    description="Switch UI language to Chinese (zh_CN)")
    recorder.record("storefront", "06_language_zh_CN_catalog", "GET", "/petstore/main.screen", description="Catalog screen in Chinese")
    recorder.record("storefront", "06_language_en_US", "POST", "/petstore/changelocale.do", 
                    data={"locale": "en_US", "referring_URL": "main.screen", "referring_screen": "main.screen", "cacheId": "petstore"},
                    description="Switch UI language back to English (en_US)")

    # Shopping Cart Flow
    cart_recorder = HTTPRecorder()
    cart_recorder.record("storefront", "07_cart_view_empty", "GET", "/petstore/cart.screen", description="Empty shopping cart view")
    cart_recorder.record("storefront", "07_cart_add_item_EST-1", "POST", "/petstore/cart.do?action=purchase&itemId=EST-1", 
                         data={"action": "purchase", "itemId": "EST-1"}, description="Add Large Angelfish (EST-1) to cart")
    cart_recorder.record("storefront", "07_cart_add_item_EST-6", "POST", "/petstore/cart.do?action=purchase&itemId=EST-6", 
                         data={"action": "purchase", "itemId": "EST-6"}, description="Add Male Adult Bulldog (EST-6) to cart")
    cart_recorder.record("storefront", "07_cart_update_quantity", "POST", "/petstore/cart.do?action=update&itemId=EST-1&quantity=3", 
                         data={"action": "update", "itemId": "EST-1", "quantity": "3"}, description="Update quantity of EST-1 to 3")
    cart_recorder.record("storefront", "07_cart_delete_item", "POST", "/petstore/cart.do?action=delete&itemId=EST-6", 
                         data={"action": "delete", "itemId": "EST-6"}, description="Remove EST-6 from cart")

    # Customer Signin & Checkout Flow
    auth_recorder = HTTPRecorder()
    auth_recorder.record("storefront", "08_auth_signin_welcome", "GET", "/petstore/signon_welcome.screen", description="Sign-in welcome landing page")
    auth_recorder.record("storefront", "08_auth_signin_form", "GET", "/petstore/signon.screen", description="Customer login form")
    auth_recorder.record("storefront", "08_auth_login_submit", "POST", "/petstore/j_signon_check", 
                         data={"j_username": "j2ee", "j_password": "j2ee"}, description="Customer authentication POST (j2ee/j2ee)")
    auth_recorder.record("storefront", "08_auth_user_profile", "GET", "/petstore/userprofile.screen", description="Customer account profile view")
    
    # Add item and checkout
    auth_recorder.record("storefront", "09_checkout_add_cart", "POST", "/petstore/cart.do?action=purchase&itemId=EST-1", 
                         data={"action": "purchase", "itemId": "EST-1"}, description="Add item to cart before checkout")
    auth_recorder.record("storefront", "09_checkout_order_form", "GET", "/petstore/order.screen", description="Order information screen with billing and shipping address")
    
    order_data = {
        "family_name_a": "Duke",
        "given_name_a": "Java",
        "address_1_a": "100 Sun Way",
        "address_2_a": "",
        "city_a": "Santa Clara",
        "state_or_province_a": "CA",
        "postal_code_a": "95054",
        "country_a": "USA",
        "telephone_number_a": "4085551212",
        "email_a": "duke@sun.com",
        "family_name_b": "Duke",
        "given_name_b": "Java",
        "address_1_b": "100 Sun Way",
        "address_2_b": "",
        "city_b": "Santa Clara",
        "state_or_province_b": "CA",
        "postal_code_b": "95054",
        "country_b": "USA",
        "telephone_number_b": "4085551212",
        "email_b": "duke@sun.com",
        "credit_card_number": "4111111111111111",
        "card_type": "Visa",
        "card_expiry": "12/05"
    }
    auth_recorder.record("storefront", "09_checkout_order_submit", "POST", "/petstore/order.do", 
                         data=order_data, description="Submit order form with shipping, billing, and credit card details")

    # 2. SUPPLIER PORTAL FLOWS
    sup_recorder = HTTPRecorder()
    sup_recorder.record("supplier", "01_login_landing", "GET", "/supplier/RcvrRequestProcessor", description="Supplier login form landing page")
    sup_recorder.record("supplier", "02_auth_submit", "POST", "/supplier/j_security_check", 
                        data={"j_username": "supplier", "j_password": "supplier"}, description="Supplier authentication POST (supplier/supplier)")
    sup_recorder.record("supplier", "03_inventory_display", "POST", "/supplier/RcvrRequestProcessor", 
                        data={"currentScreen": "displayinventory"}, description="Supplier display inventory list")
    sup_recorder.record("supplier", "04_inventory_update_stock", "POST", "/supplier/RcvrRequestProcessor", 
                        data={"currentScreen": "updateinventory", "item_EST-1": "on", "qty_EST-1": "15000"}, description="Supplier restock item EST-1 to 15000 units")
    sup_recorder.record("supplier", "05_logout", "POST", "/supplier/RcvrRequestProcessor", 
                        data={"currentScreen": "logout"}, description="Supplier logout action")

    # 3. ADMIN PORTAL & XML API FLOWS
    adm_recorder = HTTPRecorder()
    adm_recorder.record("admin", "01_login_landing", "GET", "/admin/AdminRequestProcessor", description="Admin login landing page")
    adm_recorder.record("admin", "02_auth_submit", "POST", "/admin/j_security_check", 
                        data={"j_username": "jps_admin", "j_password": "admin"}, description="Admin authentication POST (jps_admin/admin)")
    adm_recorder.record("admin", "03_jnlp_descriptor", "GET", "/admin/AdminRequestProcessor?currentScreen=manageorders", description="Java Web Start (JNLP) XML launch descriptor")
    
    # XML Backend APIs used by Swing Rich Client
    adm_recorder.record("admin", "04_api_get_orders_pending", "POST", "/admin/ApplRequestProcessor", 
                        headers={"Content-Type": "application/x-www-form-urlencoded"},
                        data="<PetStoreRequest><Type>GETORDERS</Type><Status>P</Status></PetStoreRequest>", 
                        description="XML API: Query Pending Orders (Status=P)")
    adm_recorder.record("admin", "05_api_get_orders_approved", "POST", "/admin/ApplRequestProcessor", 
                        headers={"Content-Type": "application/x-www-form-urlencoded"},
                        data="<PetStoreRequest><Type>GETORDERS</Type><Status>A</Status></PetStoreRequest>", 
                        description="XML API: Query Approved Orders (Status=A)")
    adm_recorder.record("admin", "06_api_get_orders_completed", "POST", "/admin/ApplRequestProcessor", 
                        headers={"Content-Type": "application/x-www-form-urlencoded"},
                        data="<PetStoreRequest><Type>GETORDERS</Type><Status>C</Status></PetStoreRequest>", 
                        description="XML API: Query Completed Orders (Status=C)")
    adm_recorder.record("admin", "07_api_revenue_stats", "POST", "/admin/ApplRequestProcessor", 
                        headers={"Content-Type": "application/x-www-form-urlencoded"},
                        data="<PetStoreRequest><Type>REVENUE</Type><Start>01/01/2002</Start><End>12/31/2003</End><ReqCategory>FISH</ReqCategory></PetStoreRequest>", 
                        description="XML API: Query Revenue Analytics by Category and Date Range")
    adm_recorder.record("admin", "08_api_order_counts", "POST", "/admin/ApplRequestProcessor", 
                        headers={"Content-Type": "application/x-www-form-urlencoded"},
                        data="<PetStoreRequest><Type>ORDERS</Type><Start>01/01/2002</Start><End>12/31/2003</End><ReqCategory>FISH</ReqCategory></PetStoreRequest>", 
                        description="XML API: Query Order Volume by Category and Date Range")

    # 4. COPY ALL AUTHENTIC STATIC ASSETS
    print("\nCopying static image assets...")
    assets_dir = os.path.join(OUTPUT_DIR, "static_assets")
    os.makedirs(assets_dir, exist_ok=True)
    os.system(f"cp -r src/apps/petstore/src/docroot/images/* {assets_dir}/")

    # 5. GENERATE COMPREHENSIVE README.md IN legacy_reference/
    print("\nGenerating legacy_reference/README.md...")
    all_records = (recorder.records + cart_recorder.records + auth_recorder.records + 
                   sup_recorder.records + adm_recorder.records)

    readme_content = """# Java Pet Store 1.3.1_02 - Complete Legacy UI & HTTP Reference Catalog

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
"""

    for r in all_records:
        req = r["request"]
        res = r["response"]
        method = req["method"]
        path = req["path"]
        name = r["name"]
        cat = r["category"]
        desc = r["description"]
        body_summary = "`None`"
        if req["body"]:
            if isinstance(req["body"], dict):
                body_summary = "`" + "&".join([f"{k}={v}" for k, v in req["body"].items()][:2]) + "...`"
            elif isinstance(req["body"], str):
                body_summary = f"`{req['body'][:30]}...`"
        resp_fmt = f"[{res['file']}]({cat}/{name}/{res['file']}) ({res['status_code']})"
        readme_content += f"| **{cat.capitalize()}** | `{name}`<br>{desc} | `{method}` | `{path}` | {body_summary} | {resp_fmt} |\n"

    readme_content += """
---

## How to Use This Reference in Phase 2 Migration

1. **REST API Contract Design**:
   - Compare new Spring Boot Controller request/response JSON schemas against the legacy URL parameters and XML payloads documented in `request_response.json`.
2. **Modern UI Component Parity**:
   - Inspect the captured `response.html` files in each folder alongside the original GIF assets in `static_assets/` to ensure 100% visual and functional parity in modern React / Thymeleaf templates.
3. **Automated Migration Testing**:
   - Use the recorded input forms and expected outputs in `request_response.json` as the baseline test fixtures for Phase 2 End-to-End integration tests.
"""

    with open(os.path.join(OUTPUT_DIR, "README.md"), "w", encoding="utf-8") as f:
        f.write(readme_content)

    print(f"\nSuccessfully archived {len(all_records)} interactions and assets in {OUTPUT_DIR}")

if __name__ == "__main__":
    main()
