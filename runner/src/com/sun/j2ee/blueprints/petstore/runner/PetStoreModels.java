package com.sun.j2ee.blueprints.petstore.runner;

import java.io.Serializable;
import java.util.*;

public class PetStoreModels {

    public static class Category implements Serializable {
        public String id;
        public Map<String, String> names = new HashMap<>();
        public Map<String, String> descriptions = new HashMap<>();
        public List<Product> products = new ArrayList<>();

        public Category(String id) {
            this.id = id;
        }

        public String getName(String locale) {
            return names.getOrDefault(locale, names.getOrDefault("en_US", id));
        }

        public String getDescription(String locale) {
            return descriptions.getOrDefault(locale, descriptions.getOrDefault("en_US", ""));
        }
    }

    public static class Product implements Serializable {
        public String id;
        public String categoryId;
        public Map<String, String> names = new HashMap<>();
        public Map<String, String> descriptions = new HashMap<>();
        public String image;
        public List<Item> items = new ArrayList<>();

        public Product(String id, String categoryId) {
            this.id = id;
            this.categoryId = categoryId;
        }

        public String getName(String locale) {
            return names.getOrDefault(locale, names.getOrDefault("en_US", id));
        }

        public String getDescription(String locale) {
            return descriptions.getOrDefault(locale, descriptions.getOrDefault("en_US", ""));
        }
    }

    public static class Item implements Serializable {
        public String id;
        public String productId;
        public Map<String, String> names = new HashMap<>();
        public double listPrice;
        public double unitCost;
        public String supplierId;
        public String attribute1;
        public String image;
        public int stock = 100;

        public Item(String id, String productId) {
            this.id = id;
            this.productId = productId;
        }

        public String getName(String locale) {
            return names.getOrDefault(locale, names.getOrDefault("en_US", id));
        }
    }

    public static class CartItem implements Serializable {
        public Item item;
        public Product product;
        public int quantity;

        public CartItem(Item item, Product product, int quantity) {
            this.item = item;
            this.product = product;
            this.quantity = quantity;
        }

        public double getTotal() {
            return item.listPrice * quantity;
        }
    }

    public static class Cart implements Serializable {
        public Map<String, CartItem> items = new LinkedHashMap<>();

        public void addItem(Item item, Product product, int qty) {
            CartItem ci = items.get(item.id);
            if (ci == null) {
                items.put(item.id, new CartItem(item, product, qty));
            } else {
                ci.quantity += qty;
            }
        }

        public void updateQuantity(String itemId, int qty) {
            if (qty <= 0) {
                items.remove(itemId);
            } else {
                CartItem ci = items.get(itemId);
                if (ci != null) {
                    ci.quantity = qty;
                }
            }
        }

        public void removeItem(String itemId) {
            items.remove(itemId);
        }

        public void clear() {
            items.clear();
        }

        public int getTotalCount() {
            int count = 0;
            for (CartItem ci : items.values()) {
                count += ci.quantity;
            }
            return count;
        }

        public double getSubTotal() {
            double total = 0;
            for (CartItem ci : items.values()) {
                total += ci.getTotal();
            }
            return total;
        }
    }

    public static class Address implements Serializable {
        public String street1 = "";
        public String street2 = "";
        public String city = "";
        public String state = "";
        public String zipCode = "";
        public String country = "USA";
    }

    public static class CreditCard implements Serializable {
        public String cardNumber = "";
        public String cardType = "Visa";
        public String expiryDate = "12/28";
    }

    public static class Customer implements Serializable {
        public String userId;
        public String password;
        public String givenName = "";
        public String familyName = "";
        public String email = "";
        public String phone = "";
        public Address address = new Address();
        public CreditCard creditCard = new CreditCard();
        public String preferredLanguage = "en_US";
        public String favoriteCategory = "DOGS";
        public boolean myListPreference = true;
        public boolean bannerPreference = true;

        public Customer(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }
    }

    public static class Order implements Serializable {
        public String orderId;
        public String userId;
        public Date orderDate;
        public String status = "Pending Approval"; // Pending Approval, Approved, Shipped, Denied
        public double total;
        public Address shippingAddress;
        public Address billingAddress;
        public CreditCard creditCard;
        public List<CartItem> items = new ArrayList<>();

        public Order(String orderId, String userId, double total) {
            this.orderId = orderId;
            this.userId = userId;
            this.total = total;
            this.orderDate = new Date();
        }
    }

    public static class UserSession {
        public String sessionId;
        public String userId = null;
        public Customer customer = null;
        public Cart cart = new Cart();
        public String locale = "en_US";

        public UserSession(String sessionId) {
            this.sessionId = sessionId;
        }

        public boolean isLoggedIn() {
            return userId != null && !userId.isEmpty();
        }
    }
}
