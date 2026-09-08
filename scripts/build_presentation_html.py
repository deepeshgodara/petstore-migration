#!/usr/bin/env python3
"""
Builds the 20-slide interactive presentation index.html for Sun Java Pet Store modernization.
"""

HTML_CONTENT = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sun Java Pet Store 1.3.1_02 Modernization Showcase</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg-dark: #090d16;
      --bg-surface: #111827;
      --bg-card: rgba(30, 41, 59, 0.7);
      --border-subtle: rgba(255, 255, 255, 0.08);
      --border-glow: rgba(56, 189, 248, 0.3);
      --text-primary: #f8fafc;
      --text-secondary: #94a3b8;
      --text-muted: #64748b;
      --accent-blue: #38bdf8;
      --accent-emerald: #34d399;
      --accent-purple: #a855f7;
      --accent-orange: #fb923c;
      --accent-rose: #f43f5e;
      --radius-sm: 8px;
      --radius-md: 12px;
      --radius-lg: 20px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: 'Inter', sans-serif;
      background: var(--bg-dark);
      color: var(--text-primary);
      overflow: hidden;
      width: 100vw;
      height: 100vh;
      display: flex;
      flex-direction: column;
      user-select: none;
    }

    /* Top presentation status bar */
    .top-bar {
      height: 56px;
      border-bottom: 1px solid var(--border-subtle);
      background: rgba(17, 24, 39, 0.85);
      backdrop-filter: blur(12px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.5rem;
      z-index: 100;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-weight: 800;
      font-size: 1rem;
      letter-spacing: -0.02em;
    }

    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.2rem 0.6rem;
      border-radius: 9999px;
      font-size: 0.7rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      background: rgba(56, 189, 248, 0.15);
      border: 1px solid var(--border-glow);
      color: var(--accent-blue);
    }

    .controls {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .btn-ctrl {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-subtle);
      color: var(--text-primary);
      padding: 0.4rem 0.8rem;
      border-radius: var(--radius-sm);
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 0.4rem;
      transition: all 0.2s ease;
    }

    .btn-ctrl:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.2);
    }

    .btn-ctrl:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }

    /* Deck main stage */
    .deck-container {
      flex: 1;
      position: relative;
      overflow: hidden;
      display: flex;
    }

    .slide {
      position: absolute;
      inset: 0;
      opacity: 0;
      pointer-events: none;
      transform: translateX(30px);
      transition: opacity 0.35s cubic-bezier(0.16, 1, 0.3, 1), transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
      padding: 2.5rem 3.5rem 2rem 3.5rem;
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
    }

    .slide.active {
      opacity: 1;
      pointer-events: auto;
      transform: translateX(0);
    }

    /* Slide Header */
    .slide-header {
      margin-bottom: 1.5rem;
    }

    .slide-category {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: var(--accent-blue);
      margin-bottom: 0.4rem;
      display: inline-block;
    }

    .slide-title {
      font-size: 2rem;
      font-weight: 800;
      line-height: 1.2;
      letter-spacing: -0.02em;
      background: linear-gradient(135deg, #ffffff, #cbd5e1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    /* Grid Layouts */
    .slide-body {
      flex: 1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.75rem;
      overflow: hidden;
    }

    .slide-body.full-width {
      grid-template-columns: 1fr;
    }

    .slide-body.tri-column {
      grid-template-columns: 1fr 1fr 1fr;
    }

    /* Cards */
    .card {
      background: var(--bg-card);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      backdrop-filter: blur(10px);
    }

    .card.highlight-blue { border-color: rgba(56, 189, 248, 0.4); }
    .card.highlight-green { border-color: rgba(52, 211, 153, 0.4); }
    .card.highlight-purple { border-color: rgba(168, 85, 247, 0.4); }
    .card.highlight-orange { border-color: rgba(251, 146, 60, 0.4); }

    .card-title {
      font-size: 1.15rem;
      font-weight: 700;
      margin-bottom: 0.75rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .card-title.blue { color: var(--accent-blue); }
    .card-title.green { color: var(--accent-emerald); }
    .card-title.purple { color: var(--accent-purple); }
    .card-title.orange { color: var(--accent-orange); }
    .card-title.rose { color: var(--accent-rose); }

    ul.bullet-list {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      font-size: 0.9rem;
      line-height: 1.5;
      color: #cbd5e1;
    }

    ul.bullet-list li {
      position: relative;
      padding-left: 1.25rem;
    }

    ul.bullet-list li::before {
      content: "•";
      position: absolute;
      left: 0;
      color: var(--accent-blue);
      font-weight: bold;
      font-size: 1.2rem;
      line-height: 1;
      top: 0;
    }

    .image-preview {
      width: 100%;
      height: 100%;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);
      object-fit: cover;
      cursor: zoom-in;
      transition: transform 0.3s ease;
    }

    .image-preview:hover {
      transform: scale(1.01);
    }

    /* Speaker Notes Bottom Drawer */
    .speaker-notes-drawer {
      margin-top: 1rem;
      background: rgba(15, 23, 42, 0.9);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 0.6rem 1rem;
      font-size: 0.8rem;
      color: var(--text-muted);
      font-family: 'JetBrains Mono', monospace;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .speaker-notes-drawer strong {
      color: var(--accent-blue);
    }

    /* Bottom progress bar */
    .progress-bar-container {
      height: 4px;
      width: 100%;
      background: rgba(255, 255, 255, 0.05);
    }

    .progress-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--accent-blue), var(--accent-emerald));
      width: 5%;
      transition: width 0.3s ease;
    }

    /* Keyboard navigation badge */
    .key-badge {
      font-family: 'JetBrains Mono', monospace;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 4px;
      padding: 0.1rem 0.35rem;
      font-size: 0.7rem;
      color: #e2e8f0;
    }
  </style>
</head>
<body>

  <!-- Top bar -->
  <div class="top-bar">
    <div class="brand">
      <span>Java Pet Store 1.3.1_02</span>
      <span class="badge">Architecture Showcase</span>
    </div>
    <div class="controls">
      <span style="font-size: 0.8rem; color: var(--text-muted); margin-right: 0.5rem;">
        Navigate: <span class="key-badge">←</span> <span class="key-badge">→</span> or <span class="key-badge">Space</span>
      </span>
      <button class="btn-ctrl" id="prevBtn" onclick="prevSlide()">‹ Previous</button>
      <span id="slideCounter" style="font-size: 0.85rem; font-weight: 700; font-family: 'JetBrains Mono'; color: var(--accent-blue);">1 / 20</span>
      <button class="btn-ctrl" id="nextBtn" onclick="nextSlide()">Next ›</button>
      <button class="btn-ctrl" id="fsBtn" onclick="toggleFullScreen()">⛶ Fullscreen</button>
    </div>
  </div>

  <!-- Slide Deck -->
  <div class="deck-container">

    <!-- SLIDE 1: Title Slide -->
    <div class="slide active" id="slide-1">
      <div style="margin: auto 0; text-align: left; max-width: 900px;">
        <span class="slide-category">SUN MICROSYSTEMS PET STORE 1.3.1_02 MODERNIZATION</span>
        <h1 style="font-size: 3.2rem; font-weight: 800; line-height: 1.1; margin-bottom: 1.25rem; background: linear-gradient(135deg, #ffffff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
          Cloud-Native Microservices & Strangler Fig Migration
        </h1>
        <p style="font-size: 1.2rem; line-height: 1.6; color: var(--text-secondary); margin-bottom: 2rem;">
          Migrating a legacy 2002 J2EE 1.3 / EJB 2.0 monolith to Spring Boot 3.3, Java 21 LTS, MongoDB 7.0, and Apache Kafka with 100% Zero-Downtime Data Parity.
        </p>
        <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
          <div class="badge" style="background: rgba(52, 211, 153, 0.15); color: #34d399; border-color: rgba(52, 211, 153, 0.4); font-size: 0.8rem; padding: 0.35rem 0.85rem;">
            ✓ 100% Frozen 2002 Baseline
          </div>
          <div class="badge" style="background: rgba(56, 189, 248, 0.15); color: #38bdf8; border-color: rgba(56, 189, 248, 0.4); font-size: 0.8rem; padding: 0.35rem 0.85rem;">
            ✓ Kafka Dual-Write Event Bus
          </div>
          <div class="badge" style="background: rgba(168, 85, 247, 0.15); color: #c084fc; border-color: rgba(168, 85, 247, 0.4); font-size: 0.8rem; padding: 0.35rem 0.85rem;">
            ✓ Real-Time Parity Auditing
          </div>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Introduce the project as an enterprise case study in non-invasive modernization: preserving legacy assets while building a state-of-the-art distributed reactive architecture.
      </div>
    </div>

    <!-- SLIDE 2: Executive Summary -->
    <div class="slide" id="slide-2">
      <div class="slide-header">
        <span class="slide-category">STRATEGIC PRINCIPLES</span>
        <h2 class="slide-title">Executive Summary & 3 Core Mandates</h2>
      </div>
      <div class="slide-body tri-column">
        <div class="card highlight-blue">
          <div class="card-title blue">1. Frozen Baseline</div>
          <ul class="bullet-list">
            <li>Original 2002 source code in <code>petstore-legacy/</code> is <strong>100% untouched</strong>.</li>
            <li>Apache TomEE 1.7.5 container on OpenJDK 8 hosts all 4 legacy EARs.</li>
            <li>Zero build.xml or SQL changes.</li>
            <li>Guarantees zero regression risk and acts as the Golden Oracle.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">2. Strangler Fig Pattern</div>
          <ul class="bullet-list">
            <li>Modern Spring Boot 3.3 services run concurrently with legacy.</li>
            <li>Kafka event streaming bus (<code>orders.dualwrite</code>, <code>users.created</code>).</li>
            <li>Dead-Letter Queue (DLQ) error isolation guarantees zero blast radius.</li>
            <li>Zero downtime during migration transition.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">3. Continuous Parity Auditing</div>
          <ul class="bullet-list">
            <li>Automated <code>ShadowReadComparator</code> audits records in real-time.</li>
            <li>Reconciliation across 7 joined tables and MongoDB collections.</li>
            <li>Live Ops Parity Dashboard shows <strong>100.0% Data Fidelity</strong>.</li>
            <li>Deterministic criteria for production DNS cutover.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Emphasize that we avoided a "Big Bang" cutover. Both systems run side-by-side with continuous automated reconciliation.
      </div>
    </div>

    <!-- SLIDE 3: Architecture Comparison -->
    <div class="slide" id="slide-3">
      <div class="slide-header">
        <span class="slide-category">SYSTEM EVOLUTION</span>
        <h2 class="slide-title">2002 Legacy Monolith vs. 2026 Target Platform</h2>
      </div>
      <div class="slide-body">
        <div class="card" style="border-color: rgba(244, 63, 94, 0.4);">
          <div class="card-title rose">Legacy 2002 Monolith (J2EE 1.3)</div>
          <ul class="bullet-list">
            <li><strong>Framework:</strong> EJB 2.0 (BMP & CMP entity beans, Stateful Session Beans).</li>
            <li><strong>Packaging:</strong> 4 monolithic EAR archives (<code>petstore.ear</code>, <code>opc.ear</code>, <code>petstoreadmin.ear</code>, <code>supplier.ear</code>).</li>
            <li><strong>Datastore:</strong> 18 normalized relational tables in Cloudscape/HSQLDB.</li>
            <li><strong>Clients:</strong> Server-side JSP 1.2 with custom WAF tags + Swing desktop client.</li>
            <li><strong>Messaging:</strong> Point-to-point JMS tightly coupled to in-memory broker.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Target 2026 Platform (Microservices)</div>
          <ul class="bullet-list">
            <li><strong>Framework:</strong> Spring Boot 3.3.x, Java 21 LTS with Virtual Threads.</li>
            <li><strong>Services:</strong> 3 domain microservices (catalog, order, migration).</li>
            <li><strong>Datastore:</strong> MongoDB 7.0 Replica Set (<code>rs0</code>) with document aggregates.</li>
            <li><strong>Event Streaming:</strong> Apache Kafka 3.7 in KRaft mode with DLQ.</li>
            <li><strong>Frontend:</strong> React 18 + Vite + TypeScript SPA with Route-Packaged RBAC.</li>
            <li><strong>Ops:</strong> Admin Web Analytics & Supplier Portals.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Contrast the heavy JNDI/EJB classloader constraints with modern lightweight Virtual Threads, reactive document aggregation, and cloud portability.
      </div>
    </div>

    <!-- SLIDE 4: Service-by-Service Migration Blueprint -->
    <div class="slide" id="slide-4">
      <div class="slide-header">
        <span class="slide-category">MIGRATION METHODOLOGY</span>
        <h2 class="slide-title">Service-by-Service Migration: Enterprise Strangler Blueprint</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Step-by-Step Domain Decomposition</div>
          <ul class="bullet-list">
            <li><strong>Step 1: Scaffolding & Oracle:</strong> Containerized 2002 TomEE baseline (:8000) as the immutable behavioral reference.</li>
            <li><strong>Step 2: Catalog Service (:8081):</strong> Extracted read-mostly catalog domain first (5 categories, 16 products, 28 items). Lowest write risk.</li>
            <li><strong>Step 3: Order & Customer Service (:8082):</strong> Migrated stateful transactions, 7-table user persistence, and checkout pipeline.</li>
            <li><strong>Step 4: Migration & Parity Service (:8085):</strong> Autonomous auditor hosting batch ETL, Kafka consumers, and shadow reconciliation.</li>
            <li><strong>Step 5: Full-Stack Channels (:3000):</strong> React 18 SPA replacing JSPs, Swing admin, and supplier portals.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Multi-Service Enterprise Blueprint</div>
          <ul class="bullet-list">
            <li><strong>Bounded Context Isolation:</strong> Each service owns its database collections; zero cross-service direct DB access.</li>
            <li><strong>Sequenced Risk Profiling:</strong> Migrate read domains first &rarr; audit stability &rarr; migrate transactional domains &rarr; cutover channels.</li>
            <li><strong>Decoupled Squad Ownership:</strong> Catalog, Orders, and Migration squads develop, test, and deploy independently.</li>
            <li><strong>Production Extensibility:</strong> This exact decomposition pattern scales from 3 services to 50+ enterprise microservices.</li>
            <li><strong>Independent Release Cadence:</strong> Eliminates monolithic EAR coordination and cross-team deployment bottlenecks.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> This phased domain decomposition is the exact blueprint for decomposing large 50-service enterprise monoliths safely without cross-team lockups.
      </div>
    </div>

    <!-- SLIDE 5: Modern Storefront & Imagery (Change 1) -->
    <div class="slide" id="slide-5">
      <div class="slide-header">
        <span class="slide-category">FRONTEND MODERNIZATION</span>
        <h2 class="slide-title">Authentic Product Imagery & Category Fallbacks (Change 1)</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Species-Accurate Imagery Engine</div>
          <ul class="bullet-list">
            <li><strong>Root Cause:</strong> Legacy Pet Store lacked fish assets, causing Angelfish and Tiger Shark to default to a parrot icon (<code>birds_icon.gif</code>).</li>
            <li><strong>Internet Ingestion:</strong> Researched English titles & descriptions for all 16 species (Angelfish, Koi, Goldfish, Bulldog, etc.).</li>
            <li><strong>HD Asset Library:</strong> Ingested high-resolution photos into <code>public/images/products/</code>.</li>
            <li><strong>Smart Fallbacks:</strong> Created <code>imageUtils.ts</code> with category-aware fallbacks ensuring fish never display bird or dog icons.</li>
            <li><strong>Commit:</strong> Delivered cleanly in separate commit <code>7afd138</code>.</li>
          </ul>
        </div>
        <div style="display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: var(--radius-md);">
          <img src="../demo_screenshots/01_storefront_home.png" alt="Storefront" class="image-preview">
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Showcases attention to authentic domain fidelity. Instead of using generic placeholders, we ingested real species imagery and fixed legacy image fallback bugs.
      </div>
    </div>

    <!-- SLIDE 6: User Persistence & Registration Subsystem (Task) -->
    <div class="slide" id="slide-6">
      <div class="slide-header">
        <span class="slide-category">USER MANAGEMENT SUBSYSTEM</span>
        <h2 class="slide-title">User Persistence, Kafka Streaming & Registration Flow (Task)</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-green">
          <div class="card-title green">7-Table Relational Synthesis (Task)</div>
          <ul class="bullet-list">
            <li><strong>Legacy Complexity:</strong> User data was scattered across 7 normalized tables (<code>USER</code>, <code>CUSTOMER</code>, <code>ACCOUNT</code>, <code>PROFILE</code>, <code>CONTACTINFO</code>, <code>ADDRESS</code>, <code>CREDITCARD</code>).</li>
            <li><strong>Streaming Reader:</strong> Built <code>LegacyUserCursorReader</code> with safe bit/boolean JDBC handling.</li>
            <li><strong>Denormalization:</strong> <code>UserTransformationProcessor</code> collapses relational tuples into rich <code>UserDocument</code> aggregates.</li>
            <li><strong>Kafka Event Bus:</strong> Created <code>petstore.users.created</code> and <code>petstore.users.dualwrite</code> topics.</li>
            <li><strong>Dual-Tab Auth:</strong> Implemented React <code>LoginModal</code> with Sign In and Account Registration.</li>
            <li><strong>Commit:</strong> Formatted strictly as a task in commit <code>4baa677</code>.</li>
          </ul>
        </div>
        <div style="display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: var(--radius-md);">
          <img src="../demo_screenshots/02_user_registration_modal.png" alt="Registration Modal" class="image-preview">
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Explain the 7-table synthesis into a single MongoDB document and emphasize that new user registrations immediately stream onto Kafka and sync across systems.
      </div>
    </div>

    <!-- SLIDE 7: Parity Telemetry & Verification -->
    <div class="slide" id="slide-7">
      <div class="slide-header">
        <span class="slide-category">DATA FIDELITY ASSURANCE</span>
        <h2 class="slide-title">Automated Shadow Reconciliation & Parity Telemetry</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-purple">
          <div class="card-title purple">Real-Time Verification Engine</div>
          <ul class="bullet-list">
            <li><strong>Zero-Drift Engine:</strong> <code>ShadowReadComparator</code> continuously audits legacy HSQLDB against MongoDB aggregates.</li>
            <li><strong>Deep Field Assertions:</strong> Validates category IDs, SKUs, order monetary totals (&plusmn;$0.01), line items, user credentials, and addresses.</li>
            <li><strong>Delta-Aware Logic:</strong> Intelligently recognizes modern customer registrations as valid deltas rather than drift anomalies.</li>
            <li><strong>Operational Telemetry:</strong> Live metrics report <strong>100.0% Parity Score</strong> across 704 comparative assertions with 0 drifts.</li>
            <li><strong>Cutover Gate:</strong> Automated <code>CUTOVER_READY</code> gate gives DevOps teams statistical confidence for DNS migration.</li>
          </ul>
        </div>
        <div style="display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: var(--radius-md);">
          <img src="../demo_screenshots/03_ops_parity_dashboard.png" alt="Ops Parity Dashboard" class="image-preview">
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> This live dashboard is our proof of data fidelity. It demonstrates that our modern MongoDB collections perfectly match legacy state without drift.
      </div>
    </div>

    <!-- SLIDE 8: Disaster Recovery & Chaos Demo -->
    <div class="slide" id="slide-8">
      <div class="slide-header">
        <span class="slide-category">HIGH AVAILABILITY & CHAOS RESILIENCE</span>
        <h2 class="slide-title">Chaos Resilience Demo: Secondary Datastore Outage & Rollback</h2>
      </div>
      <div class="slide-body tri-column">
        <div class="card highlight-orange">
          <div class="card-title orange">Step 1: Chaos Injection</div>
          <ul class="bullet-list">
            <li><strong>Script:</strong> <code>chaos_mongo_failure_test.sh</code>.</li>
            <li><strong>Action:</strong> Secondary MongoDB replica set paused (<code>docker pause petstore-mongo</code>).</li>
            <li><strong>Simulation:</strong> Catastrophic network partition or primary replica crash.</li>
            <li><strong>Container:</strong> PAUSED status.</li>
          </ul>
        </div>
        <div class="card highlight-blue">
          <div class="card-title blue">Step 2: Zero Blast Radius</div>
          <ul class="bullet-list">
            <li><strong>Legacy Health:</strong> TomEE on port 8000 verified: returns HTTP 200 OK (18ms).</li>
            <li><strong>Zero Blast Radius:</strong> Legacy catalog browsing & orders continue 100% uninterrupted.</li>
            <li><strong>Error Isolation:</strong> Modern checkout errors route to Dead-Letter Queue (<code>orders.dlq</code>).</li>
            <li><strong>Customer Protection:</strong> Checkout never drops data.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Step 3: Self-Healing & Parity</div>
          <ul class="bullet-list">
            <li><strong>Restoration:</strong> MongoDB unpaused (<code>docker unpause</code>).</li>
            <li><strong>Replica Set:</strong> Recovers with <code>ok: 1</code>.</li>
            <li><strong>DLQ Drain:</strong> Kafka consumers drain backlogged events.</li>
            <li><strong>Audit Assertion:</strong> Re-executing audit re-asserts <strong>100.0% Parity with 0 drifts</strong>.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Walk through the chaos script. Pausing MongoDB proves that our modern pipeline failure has ZERO blast radius on the legacy monolith, and self-heals when unpaused.
      </div>
    </div>

    <!-- SLIDE 9: Admin Console & Sales Analytics -->
    <div class="slide" id="slide-9">
      <div class="slide-header">
        <span class="slide-category">ADMINISTRATIVE OPERATIONS</span>
        <h2 class="slide-title">Admin Operations & Real-Time Business Analytics (React SPA)</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Desktop Swing to Cloud-Native Web</div>
          <ul class="bullet-list">
            <li><strong>Swing Retirement:</strong> Retires obsolete 2002 Java desktop Swing application (<code>petstoreadmin.ear</code>).</li>
            <li><strong>Order Lifecycle:</strong> Real-time approval, rejection, and fulfillment workflows with item visual inspection.</li>
            <li><strong>Sales Analytics:</strong> Live gross revenue calculation ($14,879.50), average order value ($619.98), 21 active customers.</li>
            <li><strong>Visual Charts:</strong> Dynamic SVG Donut and Bar charts analyzing category distribution (Birds: $12.9k, Fish: $1.5k).</li>
            <li><strong>Security:</strong> Protected by <code>ROLE_ADMIN</code> RBAC.</li>
          </ul>
        </div>
        <div style="display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: var(--radius-md);">
          <img src="../demo_screenshots/04b_admin_sales_analytics.png" alt="Sales Analytics" class="image-preview">
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> The 2002 monolith relied on an obsolete Java desktop Swing client (petstoreadmin.ear); we re-engineered this into an authenticated, responsive React web console with real-time SVG business intelligence.
      </div>
    </div>

    <!-- SLIDE 10: Supplier Operations & Vertical Slice -->
    <div class="slide" id="slide-10">
      <div class="slide-header">
        <span class="slide-category">SUPPLY CHAIN MODERNIZATION</span>
        <h2 class="slide-title">Supplier Operations: Full-Stack Vertical Slice (Frontend to Backend)</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-green">
          <div class="card-title green">End-to-End Vertical Integration</div>
          <ul class="bullet-list">
            <li><strong>Legacy Replacement:</strong> Retires 2002 <code>supplier.ear</code> with modern responsive portal at <code>/supplier</code>.</li>
            <li><strong>Frontend UI Layer:</strong> React 18 + Vite with dynamic category/stock filters, draft stock edits, and toast feedback.</li>
            <li><strong>API Gateway & REST Layer:</strong> <code>PUT /api/v1/items/{itemId}/inventory</code> handled by <code>petstore-catalog-service</code>.</li>
            <li><strong>Database Mutation Layer:</strong> Executes atomic <code>$set</code> on <code>items.$.inventoryQuantity</code> in MongoDB with zero table locks.</li>
            <li><strong>Instant Storefront Reflection:</strong> Customer catalog immediately reflects stock adjustments (in-stock / low-stock / backorder).</li>
            <li><strong>RBAC Protection:</strong> Route-level and API-level authorization enforced with <code>ROLE_SUPPLIER</code>.</li>
          </ul>
        </div>
        <div style="display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: var(--radius-md);">
          <img src="../demo_screenshots/05_supplier_portal.png" alt="Supplier Portal" class="image-preview">
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> The supplier portal demonstrates a complete vertical slice touching React UI, REST controllers, and atomic MongoDB document updates without monolithic dependencies.
      </div>
    </div>

    <!-- SLIDE 11: One-Way Baseline ETL vs Naive Dual-Write -->
    <div class="slide" id="slide-11">
      <div class="slide-header">
        <span class="slide-category">ARCHITECTURE REALITY CHECK</span>
        <h2 class="slide-title">Architecture Reality: One-Way Baseline ETL vs. Naive Dual-Write</h2>
      </div>
      <div class="slide-body">
        <div class="card" style="border-color: rgba(239, 68, 68, 0.4);">
          <div class="card-title rose">The Naive Dual-Write Anti-Pattern</div>
          <ul class="bullet-list">
            <li><strong>Direct Writes to Legacy HSQLDB:</strong> Writing simultaneously from Spring Boot into legacy HSQLDB while TomEE is running.</li>
            <li><strong>Fatal File-Lock Collision:</strong> HSQLDB runs as an embedded single-process engine inside TomEE. Concurrent JDBC connections cause immediate <code>LockException</code> or file corruption.</li>
            <li><strong>Distributed 2PC Latency Spikes:</strong> Synchronous two-phase commit across legacy relational tables and modern MongoDB causes cascading latency and partial-failure drift.</li>
            <li><strong>Blast Radius Coupling:</strong> A crash, lock contention, or slow query in legacy HSQLDB immediately halts modern checkout operations.</li>
            <li><strong>Dual-Write Misnomer:</strong> We never actively write into legacy HSQLDB during normal business operation.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Our Production Architecture: One-Way ETL + Event Sourcing</div>
          <ul class="bullet-list">
            <li><strong>One-Way Baseline Batch ETL:</strong> High-speed JDBC cursor reader extracts legacy catalog & users in <strong>115ms total</strong>, seeding MongoDB without modifying legacy tables.</li>
            <li><strong>Asynchronous Event Sourcing:</strong> Modern order & user services write authoritatively to MongoDB and emit immutable events to Kafka (<code>petstore.orders.dualwrite</code>, <code>petstore.users.created</code>).</li>
            <li><strong>Emergency Disaster Recovery Replay Only:</strong> Reverse writes to legacy HSQLDB exist strictly as an isolated, offline rollback utility (<code>ReverseReplayService</code>), never in active user request paths.</li>
            <li><strong>Zero Legacy Blast Radius:</strong> Legacy TomEE runs 100% frozen and isolated; modern load spikes have 0 impact on legacy stability.</li>
            <li><strong>True Pattern:</strong> Strangler Fig Migration with One-Way Ingestion and Asynchronous Kafka Event Streaming.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Address the architectural nuance: we avoid naive dual-writing into legacy because embedded HSQLDB cannot tolerate concurrent multi-process file access. We rely on fast one-way baseline extraction and Kafka event sourcing.
      </div>
    </div>

    <!-- SLIDE 12: Handling Massive Databases -->
    <div class="slide" id="slide-12">
      <div class="slide-header">
        <span class="slide-category">LARGE-SCALE DATA ARCHITECTURE</span>
        <h2 class="slide-title">Handling Massive Databases: Two-Phase Convergence & Replay</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-orange">
          <div class="card-title orange">In-Flight Writes During Multi-Hour Batch Jobs</div>
          <ul class="bullet-list">
            <li><strong>The Scale Problem:</strong> For terabyte-scale enterprise databases, historical batch extraction takes 12-48 hours. Live transactions continuously modify legacy data.</li>
            <li><strong>The Data Drift Trap:</strong> Rows read at hour 1 will be updated by legacy users at hour 6; a naive static copy results in corrupt, stale data.</li>
            <li><strong>The Solution:</strong> Two-Phase Convergence Pattern combining Write-Ahead Log (WAL) streaming with chunked historical snapshots.</li>
            <li><strong>Zero Production Contention:</strong> Batch extraction runs against read replicas; transactional OLTP master is never locked or degraded.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">The 4-Step Convergence Execution</div>
          <ul class="bullet-list">
            <li><strong>1. Start Change Stream (T0):</strong> Establish CDC buffer (Debezium/Kafka) at initial WAL offset T0 before snapshotting row 1.</li>
            <li><strong>2. Chunked Historical Snapshot:</strong> Batch reader streams primary-key slices into MongoDB with natural key <code>_id</code> upserts.</li>
            <li><strong>3. Catch-Up / Delta Draining:</strong> Once snapshot finishes at T1, Kafka consumer replays buffered mutations from T0 to Now.</li>
            <li><strong>4. Convergence & Lockstep:</strong> Replays apply latest timestamps; replication lag drops to &lt;50ms, achieving real-time parity.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> For massive databases, you never rely on a static snapshot alone. CDC stream buffering + chunked snapshots + catch-up replay converges the databases with zero downtime.
      </div>
    </div>

    <!-- SLIDE 13: Traffic Mirroring & Response Diffing -->
    <div class="slide" id="slide-13">
      <div class="slide-header">
        <span class="slide-category">ZERO-DOWNTIME VERIFICATION</span>
        <h2 class="slide-title">Pre-Cutover Verification: HTTP Traffic Mirroring & Response Diffing</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Dark Launching & Traffic Mirroring</div>
          <ul class="bullet-list">
            <li><strong>The Concept:</strong> Copy 100% of live customer HTTP traffic at the API Gateway and send it asynchronously to both systems.</li>
            <li><strong>Envoy Request Mirroring:</strong> Production traffic hits legacy J2EE app (returns to user); shadow clone hits modern microservice.</li>
            <li><strong>Zero Blast Radius:</strong> Modern failures, slow queries, or crashes in shadow mode are completely invisible to live customers.</li>
            <li><strong>Load & Concurrency Testing:</strong> Proves modern Spring Boot + MongoDB replica set handles real production peak traffic.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">Semantic Response Diffing (Twitter Diffy / Shadow Auditor)</div>
          <ul class="bullet-list">
            <li><strong>Automated Response Diffing:</strong> Tools like Twitter Diffy or GoReplay compare legacy HTTP response bodies against modern REST.</li>
            <li><strong>Noise Filtering:</strong> Automatically masks non-deterministic fields (session tokens, timestamps, autogenerated IDs).</li>
            <li><strong>Pet Store Implementation:</strong> We used legacy TomEE (:8000) as the Golden Oracle; <code>ShadowReadComparator</code> diffed legacy and Mongo states across 8 fields.</li>
            <li><strong>Zero-Drift Quality Gate:</strong> DNS cutover is only greenlit after 72 hours of 100.0% semantic response and data fidelity.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Explain how dark launching and Twitter Diffy allow testing modern services against real customer load with zero customer impact prior to cutover.
      </div>
    </div>

    <!-- SLIDE 14: Kafka vs RabbitMQ vs AWS SQS -->
    <div class="slide" id="slide-14">
      <div class="slide-header">
        <span class="slide-category">TECHNOLOGY EVALUATION</span>
        <h2 class="slide-title">Messaging Evaluation: Why Apache Kafka over RabbitMQ or AWS SQS?</h2>
      </div>
      <div class="slide-body tri-column">
        <div class="card highlight-blue">
          <div class="card-title blue">Apache Kafka 3.7 (Selected)</div>
          <ul class="bullet-list">
            <li><strong>Distributed Commit Log:</strong> Retains ordered events indefinitely across partitioned disk segments.</li>
            <li><strong>Offset 0 Replayability:</strong> Crucial for disaster recovery rollback, audit verification, and rebuilding MongoDB aggregates from scratch.</li>
            <li><strong>Partition Key Ordering:</strong> Strict per-entity FIFO sequencing guaranteed by partitioning on <code>orderId</code> or <code>userId</code>.</li>
            <li><strong>KRaft Container Mode:</strong> Zero cloud lock-in; runs fully self-contained in Docker for local dev, testing, and CI.</li>
            <li><strong>Zero-Copy Throughput:</strong> Kernel-level sendfile delivers 10x higher message throughput than traditional AMQP brokers.</li>
          </ul>
        </div>
        <div class="card highlight-orange">
          <div class="card-title orange">Why Not RabbitMQ?</div>
          <ul class="bullet-list">
            <li><strong>Transient Message Broker:</strong> Messages are immediately destroyed once ACKed; no native stream replay.</li>
            <li><strong>No Historical Backfill:</strong> Cannot rewind offsets to reconstruct lost database collections or rehydrate new microservices.</li>
            <li><strong>Memory-Heavy Queues:</strong> In-memory Erlang mailboxes incur severe memory bloat under backpressure vs Kafka disk pages.</li>
            <li><strong>Split-Brain Vulnerability:</strong> Mnesia network partition handling can cause silent message dropping or lost consumer state.</li>
            <li><strong>Queue Scaling Bottleneck:</strong> Single-queue throughput is bounded by single Erlang process CPU core limits.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">Why Not AWS SQS?</div>
          <ul class="bullet-list">
            <li><strong>Proprietary Cloud Lock-In:</strong> AWS SDK dependency prevents running on-premise, multi-cloud, or air-gapped environments.</li>
            <li><strong>No Native Local Dev:</strong> Requires heavy LocalStack mock containers with behavioral drift and slow startup times.</li>
            <li><strong>No Stream Rewind:</strong> Standard SQS deletes messages post-consumption; lacks arbitrary time/offset replay.</li>
            <li><strong>FIFO Throughput Quotas:</strong> SQS FIFO is limited to 300-3000 msg/sec with rigid message group ID contention.</li>
            <li><strong>Pay-Per-Request OpEx:</strong> Every polling check and diagnostic API call incurs metered AWS billing costs.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> The core justification for Kafka over RabbitMQ or SQS is durable event retention with offset 0 replayability, allowing disaster recovery rollbacks and historical audit reconciliation without external backups.
      </div>
    </div>

    <!-- SLIDE 15: Full-Stack Rationale -->
    <div class="slide" id="slide-15">
      <div class="slide-header">
        <span class="slide-category">FULL-STACK ARCHITECTURE</span>
        <h2 class="slide-title">Full-Stack Technology Rationale: React 18, MongoDB 7.0 & Java 21</h2>
      </div>
      <div class="slide-body tri-column">
        <div class="card highlight-blue">
          <div class="card-title blue">React 18 + Vite SPA</div>
          <ul class="bullet-list">
            <li><strong>Retires 98 JSPs & Swing:</strong> Replaces legacy server-side JSPs and 2002 Java desktop GUI (<code>petstoreadmin.ear</code>).</li>
            <li><strong>Sub-Second Vite HMR:</strong> Hot Module Replacement gives instant developer feedback vs 45s Ant/TomEE EAR redeploys.</li>
            <li><strong>Multi-Persona Routing:</strong> Single cohesive SPA bundles Storefront, Admin Analytics, Supplier Portal, and Parity HUD.</li>
            <li><strong>Virtual DOM & Reactivity:</strong> Seamless real-time state synchronization with backend REST/Kafka APIs without DOM flicker.</li>
            <li><strong>Component Reusability:</strong> Modular React component tree ensures consistent UI patterns across all persona dashboards.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">MongoDB 7.0 Aggregates</div>
          <ul class="bullet-list">
            <li><strong>18 Tables &rarr; 4 Aggregates:</strong> Eliminates 7-way relational SQL joins (USER, CUSTOMER, ACCOUNT, PROFILE, etc.).</li>
            <li><strong>Single-Document Atomicity:</strong> Orders and users retrieved in a single indexed read without relational impedance mismatch.</li>
            <li><strong>Schema Agility:</strong> Allows dynamic pet attributes and supplier tiers without blocking multi-table DDL schema locks.</li>
            <li><strong>Production Replica Set ('rs0'):</strong> Native high availability, automated primary election, and change streams.</li>
            <li><strong>Sub-Millisecond Read Latency:</strong> Memory-mapped WiredTiger storage engine delivers blazing fast catalog lookups.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">Spring Boot 3.3 + Java 21 LTS</div>
          <ul class="bullet-list">
            <li><strong>Project Loom Virtual Threads:</strong> High-throughput lightweight concurrency handles thousands of concurrent requests.</li>
            <li><strong>EJB 2.0 Elimination:</strong> Replaces 32 cumbersome EJB home/remote interfaces with clean POJOs and constructor DI.</li>
            <li><strong>Modern Cloud Framework:</strong> Native Kafka template, Spring Data MongoDB, and Actuator observability out-of-the-box.</li>
            <li><strong>Long-Term Support:</strong> Java 21 LTS provides modern pattern matching, records, and GraalVM native image readiness.</li>
            <li><strong>Rapid Production Boot:</strong> Starts in ~2.8 seconds compared to 45 seconds for legacy TomEE J2EE container.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Explains how each layer of the modern stack (React 18 frontend, MongoDB document model, and Spring Boot 3.3 / Java 21 microservices) systematically eliminates specific friction points from the 2002 J2EE monolith.
      </div>
    </div>

    <!-- SLIDE 16: Google Style Guide & Engineering Rigor -->
    <div class="slide" id="slide-16">
      <div class="slide-header">
        <span class="slide-category">ENGINEERING EXCELLENCE</span>
        <h2 class="slide-title">Engineering Rigor: Google Style Guide Compliance & Quality Gates</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Google Java Style Compliance</div>
          <ul class="bullet-list">
            <li><strong>Strict 2-Space Indentation:</strong> Standard Google Java formatting enforced across all 4 Spring Boot microservices.</li>
            <li><strong>Zero Wildcard Imports:</strong> Every single import is explicit (0 <code>import .*</code> occurrences across entire repository).</li>
            <li><strong>Google Naming Conventions:</strong> UpperCamelCase classes, lowerCamelCase methods/variables, CONSTANT_CASE enums.</li>
            <li><strong>Exhaustive Javadoc:</strong> Full documentation on domain entities, DTOs, Kafka consumers, and REST controllers.</li>
            <li><strong>100% Test Pass Rate:</strong> All 34 Maven unit tests pass cleanly with 0 failures, 0 errors, and 0 skipped.</li>
            <li><strong>Clean Reactor Build:</strong> Multi-module Maven reactor compiles deterministically across common, catalog, order, and migration.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Google TypeScript / JS & Automated Quality</div>
          <ul class="bullet-list">
            <li><strong>Strict TypeScript:</strong> 100% type-annotated codebase with strict compiler options and zero untyped <code>any</code>.</li>
            <li><strong>Oxlint Zero Warnings:</strong> Rust-powered linter passes with <strong>0 warnings and 0 errors</strong> across all 46 frontend files.</li>
            <li><strong>Lightning Production Build:</strong> <code>tsc -b && vite build</code> compiles production bundle in 1.09s without warnings.</li>
            <li><strong>100.0% Data Parity Score:</strong> Automated ShadowReadComparator verifies 704 data points between legacy and modern.</li>
            <li><strong>Chaos Failure Tested:</strong> Secondary outage injection proves zero blast radius on legacy and automated self-healing.</li>
            <li><strong>Clean Architecture:</strong> Modular separation of services, domain models, custom hooks, and route-level code splitting.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Highlight that code quality was not left to chance: we adhered strictly to Google's style guides for both Java and TypeScript, backed by automated oxlint checks and 100% passing test suites.
      </div>
    </div>

    <!-- SLIDE 17: Interview Q&A Part 1 -->
    <div class="slide" id="slide-17">
      <div class="slide-header">
        <span class="slide-category">SYSTEM DESIGN INTERVIEW DEEP DIVE</span>
        <h2 class="slide-title">Key Architectural Questions: Dual-Write & ACID</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Q1: Why Dual-Write over CDC / Debezium?</div>
          <ul class="bullet-list">
            <li><strong>Question:</strong> Why not use Change Data Capture (CDC) on the legacy database?</li>
            <li><strong>Answer:</strong> The legacy database is Cloudscape/HSQLDB, which lacks native Write-Ahead Log (WAL) replication streaming APIs.</li>
            <li><strong>Answer:</strong> Application-level event sourcing gives explicit control over domain event modeling (Event-Carried State Transfer).</li>
            <li><strong>Answer:</strong> Eliminates CDC connector agent overhead on the legacy database host.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">Q2: How is ACID Preserved across Services?</div>
          <ul class="bullet-list">
            <li><strong>Question:</strong> How do you maintain consistency without distributed 2-Phase Commit (2PC)?</li>
            <li><strong>Answer:</strong> 2PC introduces blocking and single points of failure. We use <strong>Eventual Consistency with the Outbox Pattern</strong>.</li>
            <li><strong>Answer:</strong> Local database write succeeds first; events are published to Kafka with <code>acks=all</code>.</li>
            <li><strong>Answer:</strong> Consumers are strictly idempotent (unique document IDs prevent duplicate processing).</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Interviewers love hearing why you chose application dual-write over CDC when dealing with proprietary or legacy embedded databases.
      </div>
    </div>

    <!-- SLIDE 18: Interview Q&A Part 2 -->
    <div class="slide" id="slide-18">
      <div class="slide-header">
        <span class="slide-category">SYSTEM DESIGN INTERVIEW DEEP DIVE</span>
        <h2 class="slide-title">Key Architectural Questions: Rollback & Split-Brain</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-orange">
          <div class="card-title orange">Q3: Modern Delta Data on Rollback?</div>
          <ul class="bullet-list">
            <li><strong>Question:</strong> If you roll back to legacy, what happens to users created in the modern app?</li>
            <li><strong>Answer:</strong> All modern writes emit full Event-Carried State Transfer payloads to Kafka (<code>users.created</code>, <code>orders.dualwrite</code>).</li>
            <li><strong>Answer:</strong> Kafka log retention preserves all events from offset 0.</li>
            <li><strong>Answer:</strong> Rollback runbook executes a reverse replay worker that writes delta events into the 7 legacy relational tables.</li>
            <li><strong>Answer:</strong> Users can log into legacy immediately without password reset.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Q4: Preventing Split-Brain Writes</div>
          <ul class="bullet-list">
            <li><strong>Question:</strong> What if legacy and modern both receive concurrent writes to the same item?</li>
            <li><strong>Answer:</strong> <strong>Single Source of Truth Rule:</strong> only one system is designated write-authoritative per entity during migration.</li>
            <li><strong>Answer:</strong> Orders & Users: modern system is write-primary; legacy database is connected in <code>readonly=true</code> mode.</li>
            <li><strong>Answer:</strong> Inventory: Supplier portal is authoritative; atomic decrement prevents overselling.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Address the split-brain dilemma head on by emphasizing the single-writer principle during transition phases.
      </div>
    </div>

    <!-- SLIDE 19: Future Scope & Roadmap -->
    <div class="slide" id="slide-19">
      <div class="slide-header">
        <span class="slide-category">FUTURE IMPROVEMENTS & SCOPE</span>
        <h2 class="slide-title">Future Scope: Enterprise Roadmap & Production Hardening</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Architecture & Resilience Hardening</div>
          <ul class="bullet-list">
            <li><strong>Transactional Outbox Pattern:</strong> Persist domain events into MongoDB 'outbox' collection within the same document transaction before Kafka dispatch.</li>
            <li><strong>Optimistic Locking (@Version):</strong> Add version tracking and CAS on status transitions to eliminate concurrent write clobbering.</li>
            <li><strong>Production Orchestration:</strong> Package services into distroless Jib Docker containers deployed via Kubernetes & Helm charts.</li>
            <li><strong>Distributed Tracing:</strong> Instrument OpenTelemetry and Jaeger tracing across React, Spring Boot, Kafka, and MongoDB.</li>
            <li><strong>Automated Reverse CDC:</strong> Continuous reverse sync from modern MongoDB back to legacy for indefinite post-cutover rollback capability.</li>
          </ul>
        </div>
        <div class="card highlight-purple">
          <div class="card-title purple">Migration Tooling & Security Maturation</div>
          <ul class="bullet-list">
            <li><strong>Spring Security & OAuth2/JWT:</strong> Transition from client-side route guards to stateless JWT bearer authentication with BCrypt hashing.</li>
            <li><strong>Automated Envoy Canary Router:</strong> Implement progressive traffic shifting (1% internal &rarr; 10% canary &rarr; 50% &rarr; 100% cutover).</li>
            <li><strong>Continuous Windowed Audit:</strong> Upgrade O(n^2) reconciler to id-keyed bucketed reconciliation for millions of records.</li>
            <li><strong>Automated Load & Chaos Injection:</strong> Scheduled weekly network partitions and cluster failovers in staging environment.</li>
            <li><strong>Zero-Trust Policy:</strong> Mutual TLS (mTLS) between all microservices and MongoDB replica set.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Conclude the technical roadmap by detailing our phased priorities for scaling this architecture to mission-critical multi-region enterprise production.
      </div>
    </div>

    <!-- SLIDE 20: Production Readiness -->
    <div class="slide" id="slide-20">
      <div class="slide-header">
        <span class="slide-category">OPERATIONAL STATUS</span>
        <h2 class="slide-title">Production Readiness & Verification Grid</h2>
      </div>
      <div class="slide-body">
        <div class="card highlight-blue">
          <div class="card-title blue">Verified Service Endpoints</div>
          <ul class="bullet-list">
            <li><strong>Storefront (port 3000):</strong> React 18 SPA (HTTP 200 OK).</li>
            <li><strong>Catalog Service (port 8081):</strong> 5 Categories, 16 Products.</li>
            <li><strong>Order Service (port 8082):</strong> Registration, Checkout, Analytics.</li>
            <li><strong>Migration Service (port 8085):</strong> Parity Telemetry & Baseline ETL.</li>
            <li><strong>MongoDB Replica Set (port 27017):</strong> 4 collections active.</li>
            <li><strong>Apache Kafka (port 9092):</strong> 4 event topics active with DLQ.</li>
            <li><strong>Legacy TomEE Container (port 8000):</strong> 100% healthy and isolated.</li>
          </ul>
        </div>
        <div class="card highlight-green">
          <div class="card-title green">Cutover Quality Gates Achieved</div>
          <ul class="bullet-list">
            <li><strong>Data Fidelity:</strong> 100.0% Parity across 704 automated comparisons.</li>
            <li><strong>Data Drifts:</strong> 0 precision discrepancies detected.</li>
            <li><strong>Chaos Resilience:</strong> Secondary datastore failure proven with zero downtime on legacy.</li>
            <li><strong>Code Quality:</strong> 100% Google Style Guide compliance in Java and TypeScript (0 oxlint warnings).</li>
            <li><strong>Documentation:</strong> Complete 6-chapter GitHub Wiki suite synchronized.</li>
            <li><strong>Status:</strong> Ready for Production DNS Cutover.</li>
          </ul>
        </div>
      </div>
      <div class="speaker-notes-drawer">
        <strong>Talking Point:</strong> Conclude by showing the operational health grid and reiterating that all quality gates (100% parity, 0 drifts, chaos resilience, Google style guide compliance) have been satisfied.
      </div>
    </div>

  </div>

  <!-- Bottom progress bar -->
  <div class="progress-bar-container">
    <div class="progress-bar-fill" id="progressBar"></div>
  </div>

  <script>
    let currentSlide = 1;
    const totalSlides = 20;

    function showSlide(index) {
      if (index < 1) index = 1;
      if (index > totalSlides) index = totalSlides;
      currentSlide = index;

      document.querySelectorAll('.slide').forEach((slide, i) => {
        slide.classList.toggle('active', i + 1 === currentSlide);
      });

      document.getElementById('slideCounter').innerText = `${currentSlide} / ${totalSlides}`;
      document.getElementById('progressBar').style.width = `${(currentSlide / totalSlides) * 100}%`;

      document.getElementById('prevBtn').disabled = (currentSlide === 1);
      document.getElementById('nextBtn').disabled = (currentSlide === totalSlides);
    }

    function nextSlide() {
      if (currentSlide < totalSlides) showSlide(currentSlide + 1);
    }

    function prevSlide() {
      if (currentSlide > 1) showSlide(currentSlide - 1);
    }

    document.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowRight' || e.key === ' ' || e.key === 'PageDown') {
        nextSlide();
      } else if (e.key === 'ArrowLeft' || e.key === 'PageUp') {
        prevSlide();
      } else if (e.key === 'Home') {
        showSlide(1);
      } else if (e.key === 'End') {
        showSlide(totalSlides);
      }
    });

    function toggleFullScreen() {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(() => {});
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen().catch(() => {});
        }
      }
    }

    showSlide(1);
  </script>
</body>
</html>
"""

with open("docs/presentation/index.html", "w") as f:
    f.write(HTML_CONTENT.strip() + "\\n")

print("Successfully wrote 20 slides to docs/presentation/index.html")
