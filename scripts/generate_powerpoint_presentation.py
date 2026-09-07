import os
import pptx
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

# 16:9 widescreen presentation
prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

blank_layout = prs.slide_layouts[6]

# Color Palette
BG_DARK = RGBColor(15, 23, 42)       # Slate 900
CARD_BG = RGBColor(30, 41, 59)       # Slate 800
CARD_BORDER = RGBColor(51, 65, 85)   # Slate 700
TEXT_MAIN = RGBColor(248, 250, 252)  # Slate 50
TEXT_MUTED = RGBColor(148, 163, 184) # Slate 400
ACCENT_BLUE = RGBColor(56, 189, 248) # Sky 400
ACCENT_GREEN = RGBColor(52, 211, 153)# Emerald 400
ACCENT_PURPLE = RGBColor(168, 85, 247)# Purple 500
ACCENT_ORANGE = RGBColor(251, 146, 60)# Orange 400

SCREENSHOT_DIR = "/Users/deepeshgodara/Documents/petstore1.3.1_02/docs/demo_screenshots"
OUTPUT_DIR = "/Users/deepeshgodara/Documents/petstore1.3.1_02/docs/presentation"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def apply_background(slide):
    bg_shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5))
    bg_shape.fill.solid()
    bg_shape.fill.fore_color.rgb = BG_DARK
    bg_shape.line.fill.background()
    return bg_shape

def add_header(slide, title, category="ENTERPRISE STRANGLER FIG ARCHITECTURE"):
    # Category pill
    pill = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(0.4), Inches(4.5), Inches(0.35))
    pill.fill.solid()
    pill.fill.fore_color.rgb = RGBColor(30, 58, 138)
    pill.line.fill.background()
    p_tf = pill.text_frame
    p_tf.margin_left = p_tf.margin_right = p_tf.margin_top = p_tf.margin_bottom = 0
    p = p_tf.paragraphs[0]
    p.text = category
    p.font.size = Pt(10)
    p.font.bold = True
    p.font.color.rgb = ACCENT_BLUE
    p.alignment = PP_ALIGN.CENTER

    # Main Title
    t_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.85), Inches(11.7), Inches(0.7))
    tf = t_box.text_frame
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    p2 = tf.paragraphs[0]
    p2.text = title
    p2.font.size = Pt(24)
    p2.font.bold = True
    p2.font.color.rgb = TEXT_MAIN

def add_card(slide, left, top, width, height, title, content_bullets, border_color=CARD_BORDER, title_color=ACCENT_BLUE):
    card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(left), Inches(top), Inches(width), Inches(height))
    card.fill.solid()
    card.fill.fore_color.rgb = CARD_BG
    card.line.color.rgb = border_color
    card.line.width = Pt(1.5)

    tf = card.text_frame
    tf.margin_left = tf.margin_right = Inches(0.3)
    tf.margin_top = Inches(0.25)
    tf.word_wrap = True

    p_title = tf.paragraphs[0]
    p_title.text = title
    p_title.font.size = Pt(16)
    p_title.font.bold = True
    p_title.font.color.rgb = title_color
    p_title.space_after = Pt(12)

    for bullet in content_bullets:
        p = tf.add_paragraph()
        p.text = "•  " + bullet
        p.font.size = Pt(12)
        p.font.color.rgb = TEXT_MAIN
        p.space_after = Pt(8)

# ==============================================================================
# SLIDE 1: Title Slide
# ==============================================================================
slide1 = prs.slides.add_slide(blank_layout)
apply_background(slide1)

t_box = slide1.shapes.add_textbox(Inches(1.0), Inches(2.0), Inches(11.333), Inches(3.5))
tf = t_box.text_frame
tf.word_wrap = True

p_sub = tf.paragraphs[0]
p_sub.text = "SUN MICROSYSTEMS JAVA PET STORE 1.3.1_02"
p_sub.font.size = Pt(14)
p_sub.font.bold = True
p_sub.font.color.rgb = ACCENT_BLUE
p_sub.space_after = Pt(14)

p_main = tf.add_paragraph()
p_main.text = "Cloud-Native Modernization & Strangler Fig Migration"
p_main.font.size = Pt(36)
p_main.font.bold = True
p_main.font.color.rgb = TEXT_MAIN
p_main.space_after = Pt(14)

p_desc = tf.add_paragraph()
p_desc.text = "Migrating 2002 J2EE 1.3 / EJB 2.0 to Spring Boot 3.3, Java 21 LTS, MongoDB 7.0, and Apache Kafka with 100% Zero-Downtime Data Parity"
p_desc.font.size = Pt(16)
p_desc.font.color.rgb = TEXT_MUTED
p_desc.space_after = Pt(28)

p_meta = tf.add_paragraph()
p_meta.text = "Architecture Review • Disaster Recovery Demo • System Design Deep Dive"
p_meta.font.size = Pt(13)
p_meta.font.bold = True
p_meta.font.color.rgb = ACCENT_GREEN

# ==============================================================================
# SLIDE 2: Executive Summary & Architectural Pillars
# ==============================================================================
slide2 = prs.slides.add_slide(blank_layout)
apply_background(slide2)
add_header(slide2, "Executive Summary & Core Modernization Mandates")

add_card(slide2, 0.8, 1.8, 3.6, 5.0, "1. Frozen Legacy Baseline", [
    "Original 2002 J2EE 1.3 source code in petstore-legacy/ remains 100% frozen and untouched.",
    "Containerized runtime (TomEE 1.7.5 on OpenJDK 8) hosts all 4 legacy EARs seamlessly.",
    "Zero modifications to legacy build.xml, setup.sh, or entity EJBs.",
    "Eliminates migration regression risk."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

add_card(slide2, 4.8, 1.8, 3.6, 5.0, "2. Strangler Fig Architecture", [
    "Deconstructs the 2002 monolith into decoupled Spring Boot 3.3 Java 21 microservices.",
    "Kafka dual-write event bus (petstore.orders.dualwrite, petstore.users.created).",
    "Dead-Letter Queue (DLQ) error isolation guarantees zero blast radius on legacy.",
    "Both systems operate concurrently without downtime."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

add_card(slide2, 8.8, 1.8, 3.6, 5.0, "3. Continuous Parity Auditing", [
    "ShadowReadComparator performs automated field-by-field verification in real-time.",
    "Reconciliation covers Categories, Products, SKUs, Orders, and Users across 7 joined tables.",
    "Live Ops Parity Dashboard reports 100.0% Data Fidelity Score with 0 drifts.",
    "Explicit CUTOVER_READY criteria for safe DNS cutover."
], border_color=ACCENT_PURPLE, title_color=ACCENT_PURPLE)

# ==============================================================================
# SLIDE 3: Legacy 2002 Monolith vs Modern 2026 Platform
# ==============================================================================
slide3 = prs.slides.add_slide(blank_layout)
apply_background(slide3)
add_header(slide3, "System Evolution: 2002 Monolith vs. 2026 Microservices")

add_card(slide3, 0.8, 1.8, 5.6, 5.0, "Legacy Architecture (2002)", [
    "Platform: J2EE 1.3, EJB 2.0 (BMP & CMP entity beans, stateful session beans).",
    "Packaging: 4 monolithic EAR archives (petstore.ear, opc.ear, petstoreadmin.ear, supplier.ear).",
    "Database: 18 normalized relational tables in Cloudscape/HSQLDB (USER, CUSTOMER, ACCOUNT, PROFILE, etc.).",
    "Clients: Server-side JSP 1.2 with custom WAF tags, plus Java Web Start / Swing desktop client.",
    "Messaging: Point-to-point JMS queues tightly coupled to in-memory broker."
], border_color=RGBColor(239, 68, 68), title_color=RGBColor(248, 113, 113))

add_card(slide3, 6.8, 1.8, 5.6, 5.0, "Target Modern Platform (2026)", [
    "Platform: Spring Boot 3.3.x, Java 21 LTS with Virtual Threads (Project Loom).",
    "Architecture: 3 domain microservices (petstore-catalog, petstore-order, petstore-migration).",
    "Datastore: MongoDB 7.0 Replica Set ('rs0') with high-performance document aggregates.",
    "Event Streaming: Apache Kafka 3.7 (KRaft mode) with 3 partitions and DLQ backoff.",
    "Frontend: React 18 + Vite + TypeScript Single-Page App with Route-Packaged RBAC.",
    "Management: Web-based Admin Analytics and Supplier Inventory Portals."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

# ==============================================================================
# SLIDE 4: Live Storefront & Authentic Imagery (Change 1)
# ==============================================================================
slide4 = prs.slides.add_slide(blank_layout)
apply_background(slide4)
add_header(slide4, "Modern Storefront & Authentic Species Imagery (Change 1)")

add_card(slide4, 0.8, 1.8, 4.8, 5.0, "Species-Accurate Image Sourcing", [
    "Identified legacy defect where fish products (Angelfish, Tiger Shark) rendered a parrot icon (birds_icon.gif).",
    "Researched English titles & descriptions across all 16 Pet Store catalog species.",
    "Fetched authentic, high-resolution product imagery from the internet.",
    "Created imageUtils.ts providing category-aware fallback hierarchies (fish never defaults to bird/mammal).",
    "Integrated across ProductCard, Modal, Cart Drawer, Supplier Portal, and Admin Orders.",
    "Committed separately as commit 7afd138."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

p_img1 = os.path.join(SCREENSHOT_DIR, "01_storefront_home.png")
if os.path.exists(p_img1):
    slide4.shapes.add_picture(p_img1, Inches(5.9), Inches(1.8), width=Inches(6.6))

# ==============================================================================
# SLIDE 5: User Persistence & Registration Subsystem (Change 2 - Task)
# ==============================================================================
slide5 = prs.slides.add_slide(blank_layout)
apply_background(slide5)
add_header(slide5, "User Subsystem, Kafka Eventing & Registration (Task)")

add_card(slide5, 0.8, 1.8, 5.2, 5.0, "7-Table Relational Synthesis (Task)", [
    "Legacy users spanned 7 tables: USER, CUSTOMER, ACCOUNT, PROFILE, CONTACTINFO, ADDRESS, CREDITCARD.",
    "Built LegacyUserCursorReader streaming relational tuples via JDBC with safe bit/boolean mapping.",
    "Built UserTransformationProcessor denormalizing tuples into MongoDB UserDocument aggregates.",
    "Provisioned Apache Kafka topics: petstore.users.created and petstore.users.dualwrite.",
    "Created REST registration & auth endpoints in petstore-order-service.",
    "Built dual-tab LoginModal in React with Sign In and Account Creation.",
    "Named change strictly as a task (commit 4baa677)."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

p_img2 = os.path.join(SCREENSHOT_DIR, "02_user_registration_modal.png")
if os.path.exists(p_img2):
    slide5.shapes.add_picture(p_img2, Inches(6.3), Inches(1.8), width=Inches(6.2))

# ==============================================================================
# SLIDE 6: Automated Shadow Reconciliation & Parity Dashboard
# ==============================================================================
slide6 = prs.slides.add_slide(blank_layout)
apply_background(slide6)
add_header(slide6, "Automated Shadow Reconciliation & Parity Telemetry")

add_card(slide6, 0.8, 1.8, 4.8, 5.0, "Continuous Parity Engine", [
    "Continuous real-time verification engine (ShadowReadComparator) auditing both datastores.",
    "Exhaustive field comparison: Category IDs, Product SKUs, Order totals & line items, User credentials & addresses.",
    "Delta-Aware: Recognizes modern customer registrations in MongoDB as valid deltas, not drifts.",
    "Live Dashboard Metrics: 100.0% Data Fidelity Score, 704 total comparisons, 0 drifts.",
    "Real-Time Telemetry: Exposes legacy vs. MongoDB document counts and Kafka pipeline metrics."
], border_color=ACCENT_PURPLE, title_color=ACCENT_PURPLE)

p_img3 = os.path.join(SCREENSHOT_DIR, "03_ops_parity_dashboard.png")
if os.path.exists(p_img3):
    slide6.shapes.add_picture(p_img3, Inches(5.9), Inches(1.8), width=Inches(6.6))

# ==============================================================================
# SLIDE 7: Disaster Recovery Demo: Secondary Outage & Self-Healing
# ==============================================================================
slide7 = prs.slides.add_slide(blank_layout)
apply_background(slide7)
add_header(slide7, "Chaos Resilience Demo: Secondary Datastore Outage & Rollback")

add_card(slide7, 0.8, 1.8, 3.6, 5.0, "Step 1: Chaos Injection", [
    "Script: chaos_mongo_failure_test.sh.",
    "Action: Pauses secondary MongoDB replica set (docker pause petstore-mongo).",
    "Simulates catastrophic network partition or total database crash.",
    "Container status: PAUSED."
], border_color=ACCENT_ORANGE, title_color=ACCENT_ORANGE)

add_card(slide7, 4.8, 1.8, 3.6, 5.0, "Step 2: Zero Blast Radius", [
    "Legacy Pet Store (TomEE port 8000) verified: returns HTTP 200 OK (18ms).",
    "Catalog browsing and orders continue uninterrupted on legacy.",
    "Modern checkout failures routed to Dead-Letter Queue (petstore.orders.dlq).",
    "Customer checkout never drops data."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

add_card(slide7, 8.8, 1.8, 3.6, 5.0, "Step 3: Self-Healing & Parity", [
    "MongoDB unpaused (docker unpause).",
    "Replica set responds with ok: 1.",
    "DLQ consumers drain and replay pending events.",
    "Automated audit re-asserts 100.0% Parity with 0 drifts."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

# SLIDE 8: Admin Console & Sales Analytics (React SPA)
# ==============================================================================
slide8 = prs.slides.add_slide(blank_layout)
apply_background(slide8)
add_header(slide8, "Admin Operations & Sales Analytics (React SPA)")

add_card(slide8, 0.8, 1.8, 4.8, 5.0, "Desktop to Cloud-Native Web", [
    "Completely retires the 2002 Java desktop Swing application (petstoreadmin.ear).",
    "Order Management: Approvals, completion, item thumbnails, and customer details.",
    "Visual Sales Analytics: Gross revenue ($14,879.50), Average Order Value ($619.98), 21 unique customers.",
    "Category Distribution: Dynamic SVG Donut & Bar Charts analyzing sales across Birds, Fish, Dogs, etc.",
    "Role Guard: Protected by ROLE_ADMIN RBAC."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

p_img4 = os.path.join(SCREENSHOT_DIR, "04b_admin_sales_analytics.png")
if os.path.exists(p_img4):
    slide8.shapes.add_picture(p_img4, Inches(5.9), Inches(1.8), width=Inches(6.6))

# ==============================================================================
# SLIDE 9: Supplier Inventory Management Portal
# ==============================================================================
slide9 = prs.slides.add_slide(blank_layout)
apply_background(slide9)
add_header(slide9, "Supplier Operations & Real-Time Warehouse Stock")

add_card(slide9, 0.8, 1.8, 4.8, 5.0, "Supplier Portal Replacement", [
    "Replaces legacy supplier.ear with modern responsive portal at /supplier.",
    "Inventory Visibility: Displays all 28 SKU items across all 5 pet categories.",
    "Atomic Updates: PUT /api/v1/items/{itemId}/inventory mutates stock directly in MongoDB.",
    "Instant Storefront Reflection: Customer inventory checks reflect new stock immediately.",
    "Restricted Access: Protected by ROLE_SUPPLIER RBAC."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

p_img5 = os.path.join(SCREENSHOT_DIR, "05_supplier_portal.png")
if os.path.exists(p_img5):
    slide9.shapes.add_picture(p_img5, Inches(5.9), Inches(1.8), width=Inches(6.6))

# ==============================================================================
# SLIDE 10: Top Interview Questions: Architecture & Data Integrity
# ==============================================================================
slide10 = prs.slides.add_slide(blank_layout)
apply_background(slide10)
add_header(slide10, "Architectural Interview Deep Dive: Dual-Write & ACID", category="SYSTEM DESIGN INTERVIEW PREPARATION")

add_card(slide10, 0.8, 1.8, 5.6, 5.0, "Q1: Why Dual-Write over CDC / Debezium?", [
    "Q: Why not use Change Data Capture (CDC) like Debezium on the legacy DB?",
    "A: Legacy datastore is Cloudscape/HSQLDB, which lacks native write-ahead log (WAL) replication APIs.",
    "A: Application-level dual-write gives full control over domain event modeling (Event-Carried State Transfer).",
    "A: Isolates legacy from CDC agent overhead and avoids complex schema mapping inside connector."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

add_card(slide10, 6.8, 1.8, 5.6, 5.0, "Q2: How is ACID Integrity Preserved?", [
    "Q: How do you avoid distributed 2-Phase Commit (2PC) bottlenecks?",
    "A: Microservices use Eventual Consistency via the Outbox / Dual-Write Pattern.",
    "A: Primary write to local datastore succeeds first; events are published to Kafka with acks=all.",
    "A: Idempotent consumers (upserts based on unique IDs) ensure duplicate events never corrupt data.",
    "A: Dead-Letter Queue (DLQ) isolates unrecoverable failures with backoff retry."
], border_color=ACCENT_PURPLE, title_color=ACCENT_PURPLE)

# ==============================================================================
# SLIDE 11: Top Interview Questions: Rollback & Split-Brain
# ==============================================================================
slide11 = prs.slides.add_slide(blank_layout)
apply_background(slide11)
add_header(slide11, "Architectural Interview Deep Dive: Rollback & Split-Brain", category="SYSTEM DESIGN INTERVIEW PREPARATION")

add_card(slide11, 0.8, 1.8, 5.6, 5.0, "Q3: How to Handle Modern Data on Rollback?", [
    "Q: If we roll back to legacy, what happens to users created in the modern app?",
    "A: Modern app publishes self-contained UserDomainEvent to Kafka with full credentials & address.",
    "A: Kafka commit log retains all events from offset 0.",
    "A: Rollback playbook invokes an on-demand reverse sync consumer that replays events into the 7 legacy relational tables.",
    "A: New users can log into legacy immediately without password reset."
], border_color=ACCENT_ORANGE, title_color=ACCENT_ORANGE)

add_card(slide11, 6.8, 1.8, 5.6, 5.0, "Q4: Preventing Split-Brain Scenarios", [
    "Q: What if legacy and modern both accept writes to the same order or inventory?",
    "A: Single Source of Truth Rule: During Canary/Strangler phases, only one system is designated write-authoritative per entity.",
    "A: Orders & Users: modern system is write-primary; legacy is frozen / read-only.",
    "A: Inventory: Supplier portal is authoritative; atomic decrement operations prevent overselling."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

# ==============================================================================
# SLIDE 12: Production Cutover Checklist & Final Status
# ==============================================================================
slide12 = prs.slides.add_slide(blank_layout)
apply_background(slide12)
add_header(slide12, "Production Operational Status & Cutover Readiness")

add_card(slide12, 0.8, 1.8, 5.6, 5.0, "Verified Service Grid", [
    "Modern Storefront (port 3000): HTTP 200 OK.",
    "Catalog Service (port 8081): 5 Categories, 16 Products.",
    "Order Service (port 8082): Registration, Login, Orders, Sales Analytics.",
    "Migration Service (port 8085): Parity Dashboard & Baseline ETL.",
    "MongoDB Replica Set 'rs0' (port 27017): 4 collections active.",
    "Apache Kafka (port 9092): dualwrite, users.created, dlq topics active.",
    "Legacy TomEE Container (port 8000): Verified healthy & isolated."
], border_color=ACCENT_BLUE, title_color=ACCENT_BLUE)

add_card(slide12, 6.8, 1.8, 5.6, 5.0, "Cutover Criteria Achieved", [
    "Data Fidelity: 100.0% Parity across 704 automated comparisons.",
    "Data Drifts: 0 discrepancies detected.",
    "Resilience: Chaos experiment proven with 0 downtime on legacy.",
    "Regression: 100% frozen legacy baseline in petstore-legacy/.",
    "Documentation: Full 6-page GitHub Wiki suite synchronized.",
    "Status: System is fully validated and ready for production cutover."
], border_color=ACCENT_GREEN, title_color=ACCENT_GREEN)

# Save presentation
out_path = os.path.join(OUTPUT_DIR, "petstore_modernization_showcase.pptx")
prs.save(out_path)
print(f"Presentation saved successfully to: {out_path}")
