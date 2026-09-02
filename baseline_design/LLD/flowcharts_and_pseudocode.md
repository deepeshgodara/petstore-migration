# Low-Level Design (LLD): Flowcharts and Pseudocode

This document provides exact algorithmic flowcharts and pseudocode for the three most complex control structures in the 2002 Java Pet Store:
1. **WAF Front Controller (`MainServlet.process`) Pipeline**
2. **FastLane vs EJB Routing Decision Algorithm in `CatalogHelper`**
3. **Block Allocation Sequence Generator Algorithm in `UniqueIdGeneratorEJB`**

---

## 1. WAF Front Controller Request Processing Pipeline

### 1.1 Flowchart

```mermaid
flowchart TD
    Start([Incoming HTTP Request]) --> ExtractURI[Extract Request URI & Session]
    ExtractURI --> CheckLocale{Language Param in URL?}
    
    CheckLocale -- Yes --> UpdateLocale[Update Locale in HttpSession]
    CheckLocale -- No --> CheckMapping[Lookup URI in URLMappings]
    UpdateLocale --> CheckMapping

    CheckMapping --> IsScreenURL{Is URI a *.screen?}
    
    IsScreenURL -- Yes --> LookupScreen[Fetch Screen Definition XML]
    LookupScreen --> LoadTemplate[Load Template JSP template.jsp]
    LoadTemplate --> InsertTags[Insert Taglib components: banner, sidebar, body, footer]
    InsertTags --> SendHTML([Render HTML to Client 200 OK])

    IsScreenURL -- No --> IsDoAction{Is URI a *.do Action?}
    
    IsDoAction -- Yes --> InstantiateAction[Instantiate Action Class via Reflection]
    InstantiateAction --> ExecAction[action.perform request]
    ExecAction --> ActionSuccess{Action Successful?}
    
    ActionSuccess -- Yes --> GetNextScreen[Determine Next Screen from FlowHandler]
    GetNextScreen --> RedirectScreen[HTTP 302 Redirect to Next Screen]
    RedirectScreen --> SendHTML
    
    ActionSuccess -- No --> HandleException[Capture HTMLActionException]
    HandleException --> RenderError[Forward to Error Screen with Message]
    RenderError --> SendHTML

    IsDoAction -- No --> Fallback404[Forward to default screen / main.screen]
    Fallback404 --> SendHTML
```

### 1.2 Pseudocode for `MainServlet.process()`

```java
public void process(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    String path = request.getPathInfo();
    
    // 1. Locale synchronization
    String lang = request.getParameter("locale");
    if (lang != null && !lang.isEmpty()) {
        session.setAttribute("com.sun.j2ee.blueprints.waf.locale", new Locale(lang));
    }
    
    // 2. Screen routing vs Action execution
    if (path.endsWith(".screen")) {
        String screenName = path.substring(1, path.indexOf(".screen"));
        Screen screen = screenDefinitions.getScreen(screenName);
        request.setAttribute("CURRENT_SCREEN", screen);
        RequestDispatcher rd = request.getRequestDispatcher(screen.getTemplateName());
        rd.forward(request, response);
        return;
    }
    
    if (path.endsWith(".do")) {
        ActionMapping mapping = urlMappings.getMapping(path);
        try {
            Action action = (Action) Class.forName(mapping.getActionClass()).newInstance();
            EventResponse eventResponse = action.perform(request);
            
            FlowHandler flowHandler = getFlowHandler(mapping);
            String nextScreen = flowHandler.getNextScreen(request, eventResponse);
            response.sendRedirect(nextScreen + ".screen");
        } catch (HTMLActionException e) {
            request.setAttribute("javax.servlet.jsp.jspException", e);
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
```

---

## 2. FastLane Reader Routing Algorithm (`CatalogHelper`)

### 2.1 Flowchart

```mermaid
flowchart TD
    Start([CatalogHelper.getProducts categoryId, start, count]) --> CheckFlag{useFastLane == true?}
    
    CheckFlag -- Yes --> CheckDAOCache{Is dao instance cached?}
    CheckDAOCache -- No --> LookupDAO[CatalogDAOFactory.getDAO Lookup via JNDI]
    LookupDAO --> CacheDAO[Cache CatalogDAO instance]
    CheckDAOCache -- Yes --> ExecDAO[dao.getProducts categoryId, start, count, locale]
    CacheDAO --> ExecDAO

    ExecDAO --> AcquireConn[Acquire direct JDBC Connection from DataSource]
    AcquireConn --> RunSQL[Execute Scrollable Prepared SQL Query]
    RunSQL --> ScrollOffset[rs.absolute start + 1]
    
    ScrollOffset --> BuildList[Iterate count times and populate List Product]
    BuildList --> CheckMore[Check rs.next for hasNext boolean]
    CheckMore --> CloseJDBC[Close ResultSet, PreparedStatement, Connection]
    CloseJDBC --> ReturnPage([Return Page Object with List, start, hasNext])

    CheckFlag -- No --> CallEJB[Invoke CatalogLocalEJB.getProducts via JNDI]
    CallEJB --> EJBContainer[EJB Container Transaction & Security Checks]
    EJBContainer --> ReturnPage
```

### 2.2 Pseudocode for `CatalogHelper.getProducts()`

```java
public Page getProducts(String categoryId, int start, int count, Locale locale) 
    throws CatalogException {
    
    if (this.useFastLane) {
        try {
            if (this.dao == null) {
                // JNDI Lookup of DAO implementation class name
                this.dao = CatalogDAOFactory.getDAO();
            }
            // Direct JDBC FastLane execution
            return this.dao.getProducts(categoryId, start, count, locale);
        } catch (CatalogDAOSysException se) {
            throw new CatalogException("FastLane Query Failed: " + se.getMessage());
        }
    } else {
        // Fallback to full EJB component invocation
        CatalogLocal ejb = getCatalogEJB();
        return ejb.getProducts(categoryId, start, count, locale);
    }
}
```

---

## 3. Block Allocation Unique ID Generation Algorithm (`UniqueIdGeneratorEJB`)

The 2002 Pet Store uses a **Transactional High-Low Block Reservation Algorithm** to avoid database bottlenecks when generating unique IDs for orders and accounts.

### 3.1 Flowchart

```mermaid
flowchart TD
    Start(["getNextId(sequenceName)"]) --> CheckCache{"currentId < maxIdInBlock?"}
    
    CheckCache -- "Yes (In-Memory Block Available)" --> IncrementLocal["currentId++"]
    IncrementLocal --> ReturnID(["Return currentId"])

    CheckCache -- "No (Block Exhausted)" --> StartTx["Start Database Transaction"]
    StartTx --> LockCounter["SELECT counter FROM CounterEJBTable WHERE name=? FOR UPDATE"]
    LockCounter --> ReadDB["Read currentCounter from DB"]
    ReadDB --> ComputeNewMax["newMax = currentCounter + blockSize"]
    ComputeNewMax --> UpdateDB["UPDATE CounterEJBTable SET counter = newMax WHERE name=?"]
    UpdateDB --> CommitTx["Commit Transaction & Release Lock"]
    
    CommitTx --> SetMemoryBounds["currentId = currentCounter + 1; maxIdInBlock = newMax"]
    SetMemoryBounds --> ReturnID
```

### 3.2 Pseudocode for `UniqueIdGeneratorEJB.getNextId()`

```java
public synchronized int getNextId(String sequenceName) throws CreateException {
    // 1. If we still have pre-allocated IDs in our in-memory block, return next
    if (this.currentId < this.maxId) {
        this.currentId++;
        return this.currentId;
    }
    
    // 2. Block exhausted: allocate a new block from the database atomically
    Connection conn = null;
    PreparedStatement selectStmt = null;
    PreparedStatement updateStmt = null;
    try {
        conn = getDataSource().getConnection();
        conn.setAutoCommit(false);
        
        // Lock row with FOR UPDATE or single-row transaction
        selectStmt = conn.prepareStatement("SELECT counter FROM Counter WHERE name = ?");
        selectStmt.setString(1, sequenceName);
        ResultSet rs = selectStmt.executeQuery();
        
        int dbCounter = 0;
        if (rs.next()) {
            dbCounter = rs.getInt(1);
        }
        rs.close();
        
        int newDbCounter = dbCounter + this.blockSize; // Default blockSize = 10
        updateStmt = conn.prepareStatement("UPDATE Counter SET counter = ? WHERE name = ?");
        updateStmt.setInt(1, newDbCounter);
        updateStmt.setString(2, sequenceName);
        updateStmt.executeUpdate();
        
        conn.commit();
        
        // 3. Update memory bounds
        this.currentId = dbCounter + 1;
        this.maxId = newDbCounter;
        
        return this.currentId;
    } catch (SQLException e) {
        if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
        throw new CreateException("Failed to allocate ID block: " + e.getMessage());
    } finally {
        close(updateStmt);
        close(selectStmt);
        close(conn);
    }
}
```
