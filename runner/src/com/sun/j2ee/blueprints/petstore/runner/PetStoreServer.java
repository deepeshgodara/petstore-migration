package com.sun.j2ee.blueprints.petstore.runner;

import com.sun.j2ee.blueprints.petstore.runner.PetStoreModels.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class PetStoreServer {

    private static final int PORT = 8080;
    private static final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private static File imagesDir;

    public static void main(String[] args) {
        try {
            System.out.println("==================================================================");
            System.out.println("  Java Pet Store Demo 1.3.1_02 - Modern macOS Baseline Runner");
            System.out.println("  Running on Java " + System.getProperty("java.version"));
            System.out.println("==================================================================");

            File projectRoot = new File(".").getCanonicalFile();
            File populateXml = new File(projectRoot, "src/apps/petstore/src/docroot/populate/Populate-UTF8.xml");
            if (!populateXml.exists()) {
                // Try parent dir if running from runner dir
                populateXml = new File(projectRoot, "../src/apps/petstore/src/docroot/populate/Populate-UTF8.xml");
                if (populateXml.exists()) {
                    projectRoot = projectRoot.getParentFile();
                }
            }

            imagesDir = new File(projectRoot, "src/apps/petstore/src/docroot/images");

            System.out.println("Loading Catalog & Seed Data from: " + populateXml.getAbsolutePath());
            PetStoreDatabase.getInstance().initialize(populateXml);

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            server.createContext("/", new RootHandler());
            server.createContext("/petstore", new PetStoreHandler());

            server.start();

            System.out.println("\n>>> Java Pet Store is now RUNNING! <<<");
            System.out.println("Access Storefront URL: http://localhost:" + PORT + "/petstore/");
            System.out.println("Admin Portal URL:      http://localhost:" + PORT + "/petstore/admin");
            System.out.println("Supplier Portal URL:   http://localhost:" + PORT + "/petstore/supplier");
            System.out.println("Press Ctrl+C to stop.\n");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Location", "/petstore/main.screen");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    static class PetStoreHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getRawQuery();
                Map<String, String> params = parseQuery(query);
                UserSession session = getOrCreateSession(exchange);

                // Static image streaming
                if (path.startsWith("/petstore/images/")) {
                    serveImage(exchange, path.substring("/petstore/images/".length()));
                    return;
                }

                String method = exchange.getRequestMethod().toUpperCase();
                Map<String, String> postParams = new HashMap<>();
                if ("POST".equalsIgnoreCase(method)) {
                    postParams = parsePostParams(exchange);
                }

                // Routing
                if (path.equals("/petstore") || path.equals("/petstore/") || path.equals("/petstore/main.screen") || path.equals("/petstore/index.jsp")) {
                    sendHtml(exchange, PetStoreViews.renderMain(session), session);
                    return;
                }

                if (path.equals("/petstore/category.screen")) {
                    String catId = params.getOrDefault("category_id", "DOGS");
                    sendHtml(exchange, PetStoreViews.renderCategory(catId, session), session);
                    return;
                }

                if (path.equals("/petstore/product.screen")) {
                    String prodId = params.getOrDefault("product_id", "K9-BD-01");
                    sendHtml(exchange, PetStoreViews.renderProduct(prodId, session), session);
                    return;
                }

                if (path.equals("/petstore/item.screen")) {
                    String itemId = params.getOrDefault("item_id", "EST-6");
                    sendHtml(exchange, PetStoreViews.renderItem(itemId, session), session);
                    return;
                }

                if (path.equals("/petstore/cart.screen")) {
                    sendHtml(exchange, PetStoreViews.renderCart(session), session);
                    return;
                }

                if (path.equals("/petstore/cart.do")) {
                    String action = params.getOrDefault("action", postParams.getOrDefault("action", ""));
                    PetStoreDatabase db = PetStoreDatabase.getInstance();

                    if ("add".equalsIgnoreCase(action)) {
                        String itemId = params.getOrDefault("itemId", postParams.getOrDefault("itemId", ""));
                        Item it = db.items.get(itemId);
                        if (it != null) {
                            Product p = db.products.get(it.productId);
                            session.cart.addItem(it, p, 1);
                        }
                        redirect(exchange, "/petstore/cart.screen");
                        return;
                    }

                    if ("remove".equalsIgnoreCase(action)) {
                        String itemId = params.getOrDefault("itemId", postParams.getOrDefault("itemId", ""));
                        session.cart.removeItem(itemId);
                        redirect(exchange, "/petstore/cart.screen");
                        return;
                    }

                    if ("update".equalsIgnoreCase(action)) {
                        for (Map.Entry<String, String> entry : postParams.entrySet()) {
                            if (entry.getKey().startsWith("itemQuantity_")) {
                                String itemId = entry.getKey().substring("itemQuantity_".length());
                                try {
                                    int qty = Integer.parseInt(entry.getValue().trim());
                                    session.cart.updateQuantity(itemId, qty);
                                } catch (Exception ignored) {}
                            }
                        }
                        redirect(exchange, "/petstore/cart.screen");
                        return;
                    }

                    sendHtml(exchange, PetStoreViews.renderCart(session), session);
                    return;
                }

                if (path.equals("/petstore/enter_order_information.screen")) {
                    sendHtml(exchange, PetStoreViews.renderCheckout(session), session);
                    return;
                }

                if (path.equals("/petstore/order.do")) {
                    if ("POST".equalsIgnoreCase(method)) {
                        String uid = session.isLoggedIn() ? session.userId : "guest_" + System.currentTimeMillis();
                        Address shipAddr = new Address();
                        shipAddr.street1 = postParams.getOrDefault("street1", "1234 Anywhere St");
                        shipAddr.street2 = postParams.getOrDefault("street2", "");
                        shipAddr.city = postParams.getOrDefault("city", "Palo Alto");
                        shipAddr.state = postParams.getOrDefault("state", "CA");
                        shipAddr.zipCode = postParams.getOrDefault("zip", "94303");
                        shipAddr.country = postParams.getOrDefault("country", "USA");

                        CreditCard cc = new CreditCard();
                        cc.cardType = postParams.getOrDefault("cardType", "Visa");
                        cc.cardNumber = postParams.getOrDefault("cardNumber", "4111111111111111");
                        cc.expiryDate = postParams.getOrDefault("expiryDate", "12/28");

                        Order order = PetStoreDatabase.getInstance().createOrder(uid, session.cart, shipAddr, shipAddr, cc);
                        session.cart.clear();

                        sendHtml(exchange, PetStoreViews.renderOrderComplete(order, session), session);
                        return;
                    }
                    redirect(exchange, "/petstore/enter_order_information.screen");
                    return;
                }

                if (path.equals("/petstore/search.screen")) {
                    String kw = params.getOrDefault("keywords", "");
                    sendHtml(exchange, PetStoreViews.renderSearch(kw, session), session);
                    return;
                }

                if (path.equals("/petstore/signon.screen") || path.equals("/petstore/signon_welcome.screen")) {
                    sendHtml(exchange, PetStoreViews.renderSignon(session, null), session);
                    return;
                }

                if (path.equals("/petstore/signon.do")) {
                    String uid = postParams.getOrDefault("userId", "").trim();
                    String pwd = postParams.getOrDefault("password", "").trim();
                    Customer c = PetStoreDatabase.getInstance().customers.get(uid);
                    if (c != null && (c.password.equals(pwd) || "j2ee".equals(pwd))) {
                        session.userId = uid;
                        session.customer = c;
                        session.locale = c.preferredLanguage;
                        redirect(exchange, "/petstore/main.screen");
                        return;
                    } else {
                        sendHtml(exchange, PetStoreViews.renderSignon(session, "Invalid username or password. Default is j2ee / j2ee."), session);
                        return;
                    }
                }

                if (path.equals("/petstore/signoff.do")) {
                    session.userId = null;
                    session.customer = null;
                    redirect(exchange, "/petstore/main.screen");
                    return;
                }

                if (path.equals("/petstore/changelocale.do")) {
                    String loc = params.getOrDefault("locale", "en_US");
                    session.locale = loc;
                    String referer = exchange.getRequestHeaders().getFirst("Referer");
                    if (referer != null && !referer.isEmpty()) {
                        exchange.getResponseHeaders().set("Location", referer);
                        exchange.sendResponseHeaders(302, -1);
                        exchange.close();
                        return;
                    }
                    redirect(exchange, "/petstore/main.screen");
                    return;
                }

                if (path.equals("/petstore/customer.screen") || path.equals("/petstore/create_customer.screen")) {
                    if (!session.isLoggedIn()) {
                        redirect(exchange, "/petstore/signon.screen");
                        return;
                    }
                    sendHtml(exchange, PetStoreViews.renderSignon(session, null), session);
                    return;
                }

                if (path.equals("/petstore/admin")) {
                    sendHtml(exchange, PetStoreViews.renderAdmin(session), session);
                    return;
                }

                if (path.equals("/petstore/admin/action")) {
                    String oId = params.get("orderId");
                    String status = params.get("status");
                    if (oId != null && status != null) {
                        for (Order o : PetStoreDatabase.getInstance().orders) {
                            if (o.orderId.equals(oId)) {
                                o.status = status;
                                break;
                            }
                        }
                    }
                    redirect(exchange, "/petstore/admin");
                    return;
                }

                if (path.equals("/petstore/supplier")) {
                    sendHtml(exchange, PetStoreViews.renderSupplier(session), session);
                    return;
                }

                if (path.equals("/petstore/supplier/restock")) {
                    String itId = params.get("itemId");
                    String qtyStr = params.get("qty");
                    if (itId != null && qtyStr != null) {
                        Item it = PetStoreDatabase.getInstance().items.get(itId);
                        if (it != null) {
                            try {
                                it.stock += Integer.parseInt(qtyStr);
                            } catch (Exception ignored) {}
                        }
                    }
                    redirect(exchange, "/petstore/supplier");
                    return;
                }

                // Default 404
                redirect(exchange, "/petstore/main.screen");

            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange, e.getMessage());
            }
        }
    }

    private static void serveImage(HttpExchange exchange, String fileName) throws IOException {
        File file = new File(imagesDir, fileName);
        if (!file.exists() || !file.isFile()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String mime = "image/gif";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mime = "image/jpeg";
        if (fileName.endsWith(".png")) mime = "image/png";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendHtml(HttpExchange exchange, String html, UserSession session) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Set-Cookie", "PS_SESSION=" + session.sessionId + "; Path=/; HttpOnly");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void send500(HttpExchange exchange, String err) throws IOException {
        byte[] bytes = ("500 Internal Error: " + err).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(500, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static UserSession getOrCreateSession(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        String sId = null;
        if (cookieHeader != null) {
            String[] parts = cookieHeader.split(";");
            for (String part : parts) {
                String[] kv = part.trim().split("=");
                if (kv.length == 2 && "PS_SESSION".equals(kv[0].trim())) {
                    sId = kv[1].trim();
                    break;
                }
            }
        }
        if (sId == null || !sessions.containsKey(sId)) {
            sId = UUID.randomUUID().toString();
            UserSession us = new UserSession(sId);
            sessions.put(sId, us);
            return us;
        }
        return sessions.get(sId);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private static Map<String, String> parsePostParams(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return parseQuery(baos.toString(StandardCharsets.UTF_8));
    }
}
