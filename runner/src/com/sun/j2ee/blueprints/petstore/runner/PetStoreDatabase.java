package com.sun.j2ee.blueprints.petstore.runner;

import com.sun.j2ee.blueprints.petstore.runner.PetStoreModels.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PetStoreDatabase {

    private static final PetStoreDatabase INSTANCE = new PetStoreDatabase();

    public final Map<String, Category> categories = new LinkedHashMap<>();
    public final Map<String, Product> products = new LinkedHashMap<>();
    public final Map<String, Item> items = new LinkedHashMap<>();
    public final Map<String, Customer> customers = new ConcurrentHashMap<>();
    public final List<Order> orders = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger orderSequence = new AtomicInteger(1000);

    public static PetStoreDatabase getInstance() {
        return INSTANCE;
    }

    private PetStoreDatabase() {
    }

    public void initialize(File populateXmlFile) throws Exception {
        categories.clear();
        products.clear();
        items.clear();
        customers.clear();

        // Read and strip DTD
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(populateXmlFile), "UTF-8"))) {
            String line;
            boolean inDtd = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<!DOCTYPE")) {
                    inDtd = true;
                }
                if (inDtd) {
                    if (line.contains("]>")) {
                        inDtd = false;
                    }
                    continue;
                }
                sb.append(line).append("\n");
            }
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(sb.toString().getBytes("UTF-8")));

        // Parse Users & Customers
        NodeList userNodes = doc.getElementsByTagName("User");
        for (int i = 0; i < userNodes.getLength(); i++) {
            Element uElem = (Element) userNodes.item(i);
            String uId = uElem.getAttribute("id");
            String pwd = getElementText(uElem, "Password");
            Customer c = new Customer(uId, pwd);
            customers.put(uId, c);
        }

        NodeList custNodes = doc.getElementsByTagName("Customer");
        for (int i = 0; i < custNodes.getLength(); i++) {
            Element cElem = (Element) custNodes.item(i);
            String uId = cElem.getAttribute("id");
            Customer c = customers.get(uId);
            if (c == null) {
                c = new Customer(uId, "j2ee");
                customers.put(uId, c);
            }
            Element acc = getFirstChild(cElem, "Account");
            if (acc != null) {
                Element contact = getFirstChild(acc, "ContactInfo");
                if (contact != null) {
                    c.familyName = getElementText(contact, "FamilyName");
                    c.givenName = getElementText(contact, "GivenName");
                    c.email = getElementText(contact, "Email");
                    c.phone = getElementText(contact, "Phone");
                    Element addr = getFirstChild(contact, "Address");
                    if (addr != null) {
                        NodeList stList = addr.getElementsByTagName("StreetName");
                        if (stList.getLength() > 0) c.address.street1 = stList.item(0).getTextContent().trim();
                        if (stList.getLength() > 1) c.address.street2 = stList.item(1).getTextContent().trim();
                        c.address.city = getElementText(addr, "City");
                        c.address.state = getElementText(addr, "State");
                        c.address.zipCode = getElementText(addr, "ZipCode");
                        c.address.country = getElementText(addr, "Country");
                    }
                }
                Element cc = getFirstChild(acc, "CreditCard");
                if (cc != null) {
                    c.creditCard.cardNumber = getElementText(cc, "CardNumber");
                    c.creditCard.cardType = getElementText(cc, "CardType");
                    c.creditCard.expiryDate = getElementText(cc, "ExpiryDate");
                }
            }
            Element prof = getFirstChild(cElem, "Profile");
            if (prof != null) {
                c.preferredLanguage = getElementText(prof, "PreferredLanguage");
                c.favoriteCategory = getElementText(prof, "FavoriteCategory");
                c.myListPreference = "true".equalsIgnoreCase(getElementText(prof, "MyListPreference"));
                c.bannerPreference = "true".equalsIgnoreCase(getElementText(prof, "BannerPreference"));
            }
        }

        // Add default admin & supplier accounts
        if (!customers.containsKey("jps_admin")) {
            customers.put("jps_admin", new Customer("jps_admin", "admin"));
        }
        if (!customers.containsKey("supplier")) {
            customers.put("supplier", new Customer("supplier", "supplier"));
        }

        // Parse Categories
        NodeList catNodes = doc.getElementsByTagName("Category");
        for (int i = 0; i < catNodes.getLength(); i++) {
            Element catElem = (Element) catNodes.item(i);
            String catId = catElem.getAttribute("id");
            Category cat = new Category(catId);

            NodeList details = catElem.getElementsByTagName("CategoryDetails");
            for (int j = 0; j < details.getLength(); j++) {
                Element d = (Element) details.item(j);
                String loc = d.getAttribute("locale");
                if (loc == null || loc.isEmpty()) {
                    loc = (j == 0 ? "en_US" : (j == 1 ? "ja_JP" : "zh_CN"));
                }
                cat.names.put(loc, getElementText(d, "Name"));
                cat.descriptions.put(loc, getElementText(d, "Description"));
            }
            categories.put(catId, cat);
        }

        // Parse Products
        NodeList prodNodes = doc.getElementsByTagName("Product");
        for (int i = 0; i < prodNodes.getLength(); i++) {
            Element pElem = (Element) prodNodes.item(i);
            String pId = pElem.getAttribute("id");
            String catId = pElem.getAttribute("category");
            Product p = new Product(pId, catId);

            NodeList details = pElem.getElementsByTagName("ProductDetails");
            for (int j = 0; j < details.getLength(); j++) {
                Element d = (Element) details.item(j);
                String loc = d.getAttribute("locale");
                if (loc == null || loc.isEmpty()) {
                    loc = (j == 0 ? "en_US" : (j == 1 ? "ja_JP" : "zh_CN"));
                }
                p.names.put(loc, getElementText(d, "Name"));
                p.descriptions.put(loc, getElementText(d, "Description"));
                if (p.image == null) {
                    p.image = getElementText(d, "Image");
                }
            }
            products.put(pId, p);
            Category cat = categories.get(catId);
            if (cat != null) {
                cat.products.add(p);
            }
        }

        // Parse Items
        NodeList itemNodes = doc.getElementsByTagName("Item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element itElem = (Element) itemNodes.item(i);
            String itId = itElem.getAttribute("id");
            String pId = itElem.getAttribute("product");
            Item it = new Item(itId, pId);

            NodeList details = itElem.getElementsByTagName("ItemDetails");
            for (int j = 0; j < details.getLength(); j++) {
                Element d = (Element) details.item(j);
                String loc = d.getAttribute("locale");
                if (loc == null || loc.isEmpty()) {
                    loc = (j == 0 ? "en_US" : (j == 1 ? "ja_JP" : "zh_CN"));
                }
                it.names.put(loc, getElementText(d, "Name"));
                if (j == 0) {
                    try { it.listPrice = Double.parseDouble(getElementText(d, "ListPrice")); } catch (Exception ignored) {}
                    try { it.unitCost = Double.parseDouble(getElementText(d, "UnitCost")); } catch (Exception ignored) {}
                    it.supplierId = getElementText(d, "SupplierId");
                    it.attribute1 = getElementText(d, "Attribute1");
                    it.image = getElementText(d, "Image");
                }
            }
            items.put(itId, it);
            Product p = products.get(pId);
            if (p != null) {
                p.items.add(it);
            }
        }

        System.out.println("Initialized PetStoreDatabase: " + categories.size() + " categories, "
                + products.size() + " products, " + items.size() + " items, " + customers.size() + " users.");
    }

    public Order createOrder(String userId, Cart cart, Address shipAddr, Address billAddr, CreditCard cc) {
        String oId = "PS-" + orderSequence.incrementAndGet();
        Order order = new Order(oId, userId, cart.getSubTotal());
        order.shippingAddress = shipAddr;
        order.billingAddress = billAddr;
        order.creditCard = cc;
        order.items.addAll(cart.items.values());

        // Deduct inventory
        for (CartItem ci : cart.items.values()) {
            Item it = items.get(ci.item.id);
            if (it != null) {
                it.stock = Math.max(0, it.stock - ci.quantity);
            }
        }

        orders.add(0, order);
        return order;
    }

    public List<Product> searchProducts(String keywords, String locale) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String kw = keywords.toLowerCase().trim();
        List<Product> results = new ArrayList<>();
        for (Product p : products.values()) {
            String name = p.getName(locale).toLowerCase();
            String desc = p.getDescription(locale).toLowerCase();
            if (name.contains(kw) || desc.contains(kw) || p.id.toLowerCase().contains(kw) || p.categoryId.toLowerCase().contains(kw)) {
                results.add(p);
            }
        }
        return results;
    }

    private static String getElementText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return "";
    }

    private static Element getFirstChild(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return (Element) nl.item(0);
        }
        return null;
    }
}
