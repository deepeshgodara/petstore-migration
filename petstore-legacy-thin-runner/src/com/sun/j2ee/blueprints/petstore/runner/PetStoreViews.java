package com.sun.j2ee.blueprints.petstore.runner;

import com.sun.j2ee.blueprints.petstore.runner.PetStoreModels.*;
import java.text.NumberFormat;
import java.util.*;

public class PetStoreViews {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    public static String renderTemplate(String title, String bodyContent, UserSession session, String activeCategoryId) {
        String locale = session.locale;
        PetStoreDatabase db = PetStoreDatabase.getInstance();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>").append(escape(title)).append(" - Java Pet Store Demo</title>\n");
        html.append("  <style type=\"text/css\">\n");
        html.append("    body { font-family: Helvetica, Arial, sans-serif; margin: 0; padding: 10px; background-color: #FFFFFF; color: #000000; }\n");
        html.append("    .petstore { font-family: Helvetica, Arial, sans-serif; font-size: 13px; }\n");
        html.append("    .petstore_title { font-family: Helvetica, Arial, sans-serif; font-size: 16px; font-weight: bold; margin-bottom: 10px; color: #000033; }\n");
        html.append("    .petstore_footer { font-family: Helvetica, Arial, sans-serif; font-size: 11px; color: #555555; }\n");
        html.append("    .petstore_listing { font-family: Helvetica, Arial, sans-serif; font-size: 12px; }\n");
        html.append("    .petstore_form { font-family: Helvetica, Arial, sans-serif; font-size: 12px; }\n");
        html.append("    a { color: #003399; text-decoration: underline; }\n");
        html.append("    a:hover { color: #CC0000; }\n");
        html.append("    table.petstore_table { width: 100%; border-collapse: collapse; margin-top: 5px; margin-bottom: 10px; }\n");
        html.append("    table.petstore_table th { background-color: #CCCCFF; padding: 6px; font-size: 12px; text-align: left; border: 1px solid #9999CC; }\n");
        html.append("    table.petstore_table td { padding: 6px; font-size: 12px; border: 1px solid #E0E0E0; }\n");
        html.append("    .cart_badge { background: #CC0000; color: white; border-radius: 10px; padding: 2px 6px; font-size: 10px; font-weight: bold; }\n");
        html.append("    .admin_badge { background: #008800; color: white; padding: 2px 5px; font-size: 11px; border-radius: 3px; }\n");
        html.append("    .btn { background-color: #EEEEEE; border: 1px solid #777777; padding: 3px 8px; font-size: 12px; cursor: pointer; }\n");
        html.append("    .btn:hover { background-color: #DDDDDD; }\n");
        html.append("    .btn-primary { background-color: #003399; color: white; border: 1px solid #002266; font-weight: bold; }\n");
        html.append("    .btn-primary:hover { background-color: #002266; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body bgcolor=\"#FFFFFF\">\n");

        // Top Banner Table
        html.append("<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n");
        html.append("  <tr>\n");
        html.append("    <td align=\"left\" valign=\"middle\">\n");
        html.append("      <a href=\"/petstore/main.screen\"><img src=\"/petstore/images/banner_logo.gif\" alt=\"Java Pet Store Demo logo\" border=\"0\"></a>\n");
        html.append("    </td>\n");
        html.append("    <td class=\"petstore\" align=\"right\" valign=\"middle\">\n");
        html.append("      <form action=\"/petstore/search.screen\" method=\"GET\" style=\"display:inline; margin:0;\">\n");
        html.append("        <input class=\"petstore_listing\" type=\"text\" name=\"keywords\" size=\"15\" placeholder=\"Search pets...\">\n");
        html.append("        <input class=\"btn\" type=\"submit\" value=\"Search\">\n");
        html.append("      </form>\n");
        html.append("      <br>\n");
        html.append("      <a href=\"/petstore/customer.screen\">Account</a> | \n");
        html.append("      <a href=\"/petstore/cart.screen\">Cart (<b>").append(session.cart.getTotalCount()).append("</b>)</a> | \n");
        if (session.isLoggedIn()) {
            html.append("      <span>Welcome, <b>").append(escape(session.userId)).append("</b></span> | \n");
            html.append("      <a href=\"/petstore/signoff.do\">Sign out</a>\n");
        } else {
            html.append("      <a href=\"/petstore/signon.screen\">Sign in</a>\n");
        }
        html.append("      | <a href=\"/petstore/admin\">Admin</a>\n");
        html.append("      | <a href=\"/petstore/supplier\">Supplier</a>\n");
        html.append("    </td>\n");
        html.append("  </tr>\n");

        // Language Selector Row
        html.append("  <tr>\n");
        html.append("    <td align=\"right\" colspan=\"2\" style=\"padding-top:4px;\">\n");
        html.append("      <a href=\"/petstore/changelocale.do?locale=en_US\"><img src=\"/petstore/images/us_flag.gif\" alt=\"English\" border=\"0\" title=\"English\"></a>&nbsp;\n");
        html.append("      <a href=\"/petstore/changelocale.do?locale=ja_JP\"><img src=\"/petstore/images/ja_flag.gif\" alt=\"Japanese\" border=\"0\" title=\"Japanese\"></a>&nbsp;\n");
        html.append("      <a href=\"/petstore/changelocale.do?locale=zh_CN\"><img src=\"/petstore/images/zh_flag.gif\" alt=\"Chinese\" border=\"0\" title=\"Chinese\"></a>\n");
        html.append("    </td>\n");
        html.append("  </tr>\n");
        html.append("  <tr><td colspan=\"2\"><hr noshade size=\"1\" color=\"#CCCCCC\" style=\"margin:6px 0 10px 0;\"></td></tr>\n");
        html.append("</table>\n");

        // Main Layout: Sidebar, Body, MyList
        html.append("<table width=\"100%\" border=\"0\" cellpadding=\"5\" cellspacing=\"0\">\n");
        html.append("  <tr valign=\"top\">\n");

        // Sidebar
        html.append("    <td width=\"180\" valign=\"top\" style=\"border-right: 1px solid #EEEEEE; padding-right:15px;\">\n");
        html.append(renderSidebar(locale, activeCategoryId));
        html.append("    </td>\n");

        // Body
        html.append("    <td valign=\"top\" style=\"padding-left:15px; min-height:400px;\">\n");
        html.append(bodyContent);
        html.append("    </td>\n");

        // MyList / Quick Cart sidebar
        html.append("    <td width=\"200\" valign=\"top\" style=\"border-left: 1px solid #EEEEEE; padding-left:15px;\">\n");
        html.append(renderMyList(session));
        html.append("    </td>\n");
        html.append("  </tr>\n");

        // Footer
        html.append("  <tr><td colspan=\"3\"><hr noshade size=\"1\" color=\"#CCCCCC\" style=\"margin:20px 0 10px 0;\"></td></tr>\n");
        html.append("  <tr>\n");
        html.append("    <td colspan=\"3\" align=\"center\" class=\"petstore_footer\">\n");
        html.append("      Java Pet Store Demo 1.3.1_02 &bull; Running on Modern macOS Engine (Java 21 LTS) &bull; ");
        html.append("      <a href=\"https://github.com/deepeshgodara/petstore-migration\" target=\"_blank\">GitHub Repository</a>\n");
        html.append("    </td>\n");
        html.append("  </tr>\n");
        html.append("</table>\n");

        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private static String renderSidebar(String locale, String activeCategoryId) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"2\" cellspacing=\"0\" border=\"0\">\n");
        sb.append("  <tr><td class=\"petstore_title\" style=\"font-size:13px;\">Pet Categories</td></tr>\n");

        String[][] catIcons = {
            {"FISH", "fish_icon.gif", "Fish"},
            {"DOGS", "dogs_icon.gif", "Dogs"},
            {"REPTILES", "reptiles_icon.gif", "Reptiles"},
            {"CATS", "cats_icon.gif", "Cats"},
            {"BIRDS", "birds_icon.gif", "Birds"}
        };

        for (String[] catInfo : catIcons) {
            String cid = catInfo[0];
            String icon = catInfo[1];
            Category cat = db.categories.get(cid);
            String name = (cat != null) ? cat.getName(locale) : catInfo[2];
            boolean isActive = cid.equalsIgnoreCase(activeCategoryId);

            sb.append("  <tr").append(isActive ? " bgcolor=\"#EEEEFF\"" : "").append(">\n");
            sb.append("    <td class=\"petstore_listing\" style=\"padding:4px 0;\">\n");
            sb.append("      <a href=\"/petstore/category.screen?category_id=").append(cid).append("\" style=\"text-decoration:none;\">\n");
            sb.append("        <img src=\"/petstore/images/").append(icon).append("\" border=\"0\" align=\"middle\" style=\"margin-right:6px;\">\n");
            sb.append("        <span style=\"font-weight:").append(isActive ? "bold" : "normal").append(";\">").append(escape(name)).append("</span>\n");
            sb.append("      </a>\n");
            sb.append("    </td>\n");
            sb.append("  </tr>\n");
        }

        sb.append("  <tr><td style=\"padding-top:15px;\"><hr noshade size=\"1\" color=\"#EEEEEE\"></td></tr>\n");
        sb.append("  <tr><td class=\"petstore_listing\"><b>Quick Links:</b><br>\n");
        sb.append("    &bull; <a href=\"/petstore/cart.screen\">View Cart</a><br>\n");
        sb.append("    &bull; <a href=\"/petstore/enter_order_information.screen\">Check Out</a><br>\n");
        sb.append("    &bull; <a href=\"/petstore/admin\">Order Admin</a><br>\n");
        sb.append("    &bull; <a href=\"/petstore/supplier\">Inventory Supply</a>\n");
        sb.append("  </td></tr>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    private static String renderMyList(UserSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"2\" cellspacing=\"0\" border=\"0\">\n");
        sb.append("  <tr><td class=\"petstore_title\" style=\"font-size:13px;\">Shopping Cart</td></tr>\n");

        if (session.cart.items.isEmpty()) {
            sb.append("  <tr><td class=\"petstore_listing\" style=\"color:#888888;\">Your cart is empty.</td></tr>\n");
        } else {
            sb.append("  <tr><td class=\"petstore_listing\">\n");
            sb.append("    <table width=\"100%\" cellpadding=\"2\" cellspacing=\"0\">\n");
            for (CartItem ci : session.cart.items.values()) {
                sb.append("      <tr>\n");
                sb.append("        <td class=\"petstore_listing\">").append(ci.quantity).append("x ").append(escape(ci.item.id)).append("</td>\n");
                sb.append("        <td class=\"petstore_listing\" align=\"right\">").append(CURRENCY.format(ci.getTotal())).append("</td>\n");
                sb.append("      </tr>\n");
            }
            sb.append("      <tr><td colspan=\"2\"><hr noshade size=\"1\" color=\"#DDDDDD\"></td></tr>\n");
            sb.append("      <tr>\n");
            sb.append("        <td class=\"petstore_listing\"><b>Total:</b></td>\n");
            sb.append("        <td class=\"petstore_listing\" align=\"right\"><b>").append(CURRENCY.format(session.cart.getSubTotal())).append("</b></td>\n");
            sb.append("      </tr>\n");
            sb.append("    </table>\n");
            sb.append("    <div style=\"margin-top:10px;\">\n");
            sb.append("      <a href=\"/petstore/cart.screen\" class=\"btn\">Edit Cart</a> \n");
            sb.append("      <a href=\"/petstore/enter_order_information.screen\" class=\"btn btn-primary\">Check Out</a>\n");
            sb.append("    </div>\n");
            sb.append("  </td></tr>\n");
        }

        sb.append("  <tr><td style=\"padding-top:20px;\"></td></tr>\n");
        sb.append("  <tr><td class=\"petstore_title\" style=\"font-size:13px;\">Recommended Pets</td></tr>\n");
        sb.append("  <tr><td class=\"petstore_listing\" style=\"color:#555555;\">\n");
        sb.append("    <a href=\"/petstore/product.screen?product_id=K9-BD-01\"><b>Bulldog</b></a> - Friendly canine companion!<br><br>\n");
        sb.append("    <a href=\"/petstore/product.screen?product_id=AV-CB-01\"><b>Amazon Parrot</b></a> - Great talker!\n");
        sb.append("  </td></tr>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    public static String renderMain(UserSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<map name=\"petmap\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=BIRDS\" alt=\"Birds\" coords=\"72,2,280,250\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=FISH\" alt=\"Fish\" coords=\"2,180,72,250\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=DOGS\" alt=\"Dogs\" coords=\"60,250,130,320\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=REPTILES\" alt=\"Reptiles\" coords=\"140,270,210,340\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=CATS\" alt=\"Cats\" coords=\"225,240,295,310\">\n");
        sb.append("  <area href=\"/petstore/category.screen?category_id=BIRDS\" alt=\"Birds\" coords=\"280,180,350,250\">\n");
        sb.append("</map>\n");
        sb.append("<div align=\"center\">\n");
        sb.append("  <p class=\"petstore_title\" style=\"font-size:18px;\">Welcome to Java Pet Store Demo</p>\n");
        sb.append("  <img src=\"/petstore/images/splash.gif\" alt=\"Pet Selection Map\" usemap=\"#petmap\" width=\"350\" height=\"355\" border=\"0\">\n");
        sb.append("  <p class=\"petstore_listing\" style=\"color:#666666; margin-top:8px;\">Click on the image above or select a category from the left sidebar.</p>\n");
        sb.append("</div>\n");
        return renderTemplate("Welcome", sb.toString(), session, null);
    }

    public static String renderCategory(String categoryId, UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        Category cat = db.categories.get(categoryId);
        if (cat == null) {
            cat = db.categories.get("DOGS");
            categoryId = "DOGS";
        }

        String locale = session.locale;
        String bannerImg = "banner_" + categoryId.toLowerCase() + ".gif";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-bottom:15px;\">\n");
        sb.append("  <img src=\"/petstore/images/").append(bannerImg).append("\" alt=\"").append(escape(cat.getName(locale))).append("\" border=\"0\">\n");
        sb.append("</div>\n");

        sb.append("<p class=\"petstore_title\">Products in ").append(escape(cat.getName(locale))).append("</p>\n");
        sb.append("<table class=\"petstore_table\">\n");
        sb.append("  <tr>\n");
        sb.append("    <th width=\"100\">Product ID</th>\n");
        sb.append("    <th>Name & Description</th>\n");
        sb.append("  </tr>\n");

        for (Product p : cat.products) {
            sb.append("  <tr>\n");
            sb.append("    <td class=\"petstore_listing\"><b><a href=\"/petstore/product.screen?product_id=").append(p.id).append("\">").append(p.id).append("</a></b></td>\n");
            sb.append("    <td class=\"petstore_listing\">\n");
            sb.append("      <a href=\"/petstore/product.screen?product_id=").append(p.id).append("\" style=\"font-weight:bold; font-size:13px;\">").append(escape(p.getName(locale))).append("</a><br>\n");
            sb.append("      <span style=\"color:#444444;\">").append(escape(p.getDescription(locale))).append("</span>\n");
            sb.append("    </td>\n");
            sb.append("  </tr>\n");
        }
        sb.append("</table>\n");

        return renderTemplate(cat.getName(locale), sb.toString(), session, categoryId);
    }

    public static String renderProduct(String productId, UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        Product prod = db.products.get(productId);
        if (prod == null) {
            return renderTemplate("Product Not Found", "<p class=\"petstore_title\">Product not found: " + escape(productId) + "</p>", session, null);
        }

        String locale = session.locale;
        StringBuilder sb = new StringBuilder();

        sb.append("<p class=\"petstore_title\">").append(escape(prod.getName(locale))).append(" (").append(prod.id).append(")</p>\n");
        sb.append("<p class=\"petstore_listing\" style=\"color:#555555;\">").append(escape(prod.getDescription(locale))).append("</p>\n");

        sb.append("<table class=\"petstore_table\">\n");
        sb.append("  <tr>\n");
        sb.append("    <th width=\"90\">Item ID</th>\n");
        sb.append("    <th>Attribute / Description</th>\n");
        sb.append("    <th width=\"80\">List Price</th>\n");
        sb.append("    <th width=\"70\">Stock</th>\n");
        sb.append("    <th width=\"100\">Action</th>\n");
        sb.append("  </tr>\n");

        for (Item it : prod.items) {
            sb.append("  <tr>\n");
            sb.append("    <td class=\"petstore_listing\"><b><a href=\"/petstore/item.screen?item_id=").append(it.id).append("\">").append(it.id).append("</a></b></td>\n");
            sb.append("    <td class=\"petstore_listing\">\n");
            sb.append("      <a href=\"/petstore/item.screen?item_id=").append(it.id).append("\">").append(escape(it.attribute1)).append(" ").append(escape(prod.getName(locale))).append("</a>\n");
            sb.append("    </td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"right\"><b>").append(CURRENCY.format(it.listPrice)).append("</b></td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"center\">").append(it.stock > 0 ? it.stock : "<span style=\"color:red;\">Out</span>").append("</td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"center\">\n");
            sb.append("      <a href=\"/petstore/cart.do?action=add&itemId=").append(it.id).append("\" class=\"btn btn-primary\">Add to Cart</a>\n");
            sb.append("    </td>\n");
            sb.append("  </tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<p class=\"petstore_listing\"><a href=\"/petstore/category.screen?category_id=").append(prod.categoryId).append("\">&laquo; Back to Category</a></p>\n");

        return renderTemplate(prod.getName(locale), sb.toString(), session, prod.categoryId);
    }

    public static String renderItem(String itemId, UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        Item it = db.items.get(itemId);
        if (it == null) {
            return renderTemplate("Item Not Found", "<p class=\"petstore_title\">Item not found: " + escape(itemId) + "</p>", session, null);
        }

        Product prod = db.products.get(it.productId);
        String locale = session.locale;
        String prodName = (prod != null) ? prod.getName(locale) : it.productId;

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">").append(escape(it.attribute1)).append(" ").append(escape(prodName)).append("</p>\n");

        sb.append("<table border=\"0\" cellpadding=\"6\" cellspacing=\"0\">\n");
        sb.append("  <tr>\n");
        if (it.image != null && !it.image.isEmpty()) {
            sb.append("    <td valign=\"top\"><img src=\"/petstore/images/").append(it.image).append("\" alt=\"").append(escape(it.id)).append("\" border=\"1\" style=\"padding:4px;\"></td>\n");
        }
        sb.append("    <td valign=\"top\" class=\"petstore_listing\">\n");
        sb.append("      <p><b>Item ID:</b> ").append(it.id).append("</p>\n");
        sb.append("      <p><b>Product:</b> <a href=\"/petstore/product.screen?product_id=").append(it.productId).append("\">").append(escape(prodName)).append(" (").append(it.productId).append(")</a></p>\n");
        sb.append("      <p><b>Attribute:</b> ").append(escape(it.attribute1)).append("</p>\n");
        sb.append("      <p><b>List Price:</b> <span style=\"font-size:15px; color:#003399; font-weight:bold;\">").append(CURRENCY.format(it.listPrice)).append("</span></p>\n");
        sb.append("      <p><b>In Stock:</b> ").append(it.stock).append(" units available</p>\n");
        sb.append("      <div style=\"margin-top:15px;\">\n");
        sb.append("        <a href=\"/petstore/cart.do?action=add&itemId=").append(it.id).append("\" class=\"btn btn-primary\" style=\"padding:6px 14px; font-size:13px;\">Add to Cart</a>\n");
        sb.append("      </div>\n");
        sb.append("    </td>\n");
        sb.append("  </tr>\n");
        sb.append("</table>\n");

        sb.append("<p class=\"petstore_listing\" style=\"margin-top:20px;\"><a href=\"/petstore/product.screen?product_id=").append(it.productId).append("\">&laquo; Back to Product</a></p>\n");

        return renderTemplate(it.id, sb.toString(), session, (prod != null ? prod.categoryId : null));
    }

    public static String renderCart(UserSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Shopping Cart</p>\n");

        if (session.cart.items.isEmpty()) {
            sb.append("<p class=\"petstore_listing\" style=\"color:#666666;\">Your shopping cart is currently empty.</p>\n");
            sb.append("<p class=\"petstore_listing\"><a href=\"/petstore/main.screen\">&laquo; Continue Shopping</a></p>\n");
            return renderTemplate("Shopping Cart", sb.toString(), session, null);
        }

        sb.append("<form action=\"/petstore/cart.do\" method=\"POST\">\n");
        sb.append("  <input type=\"hidden\" name=\"action\" value=\"update\">\n");
        sb.append("  <table class=\"petstore_table\">\n");
        sb.append("    <tr>\n");
        sb.append("      <th>Item ID</th>\n");
        sb.append("      <th>Description</th>\n");
        sb.append("      <th width=\"80\">Quantity</th>\n");
        sb.append("      <th width=\"90\">Unit Price</th>\n");
        sb.append("      <th width=\"90\">Total</th>\n");
        sb.append("      <th width=\"70\">Action</th>\n");
        sb.append("    </tr>\n");

        for (CartItem ci : session.cart.items.values()) {
            sb.append("    <tr>\n");
            sb.append("      <td class=\"petstore_listing\"><b><a href=\"/petstore/item.screen?item_id=").append(ci.item.id).append("\">").append(ci.item.id).append("</a></b></td>\n");
            sb.append("      <td class=\"petstore_listing\">").append(escape(ci.item.attribute1)).append(" ").append(escape(ci.product != null ? ci.product.getName(session.locale) : "")).append("</td>\n");
            sb.append("      <td class=\"petstore_listing\" align=\"center\"><input type=\"number\" min=\"0\" max=\"99\" name=\"itemQuantity_").append(ci.item.id).append("\" value=\"").append(ci.quantity).append("\" style=\"width:50px; text-align:center;\"></td>\n");
            sb.append("      <td class=\"petstore_listing\" align=\"right\">").append(CURRENCY.format(ci.item.listPrice)).append("</td>\n");
            sb.append("      <td class=\"petstore_listing\" align=\"right\"><b>").append(CURRENCY.format(ci.getTotal())).append("</b></td>\n");
            sb.append("      <td class=\"petstore_listing\" align=\"center\"><a href=\"/petstore/cart.do?action=remove&itemId=").append(ci.item.id).append("\" style=\"color:#CC0000;\">Remove</a></td>\n");
            sb.append("    </tr>\n");
        }

        sb.append("    <tr>\n");
        sb.append("      <td colspan=\"4\" align=\"right\" class=\"petstore_listing\"><b>Subtotal:</b></td>\n");
        sb.append("      <td align=\"right\" class=\"petstore_listing\" bgcolor=\"#CCCCFF\"><b>").append(CURRENCY.format(session.cart.getSubTotal())).append("</b></td>\n");
        sb.append("      <td></td>\n");
        sb.append("    </tr>\n");
        sb.append("  </table>\n");

        sb.append("  <div style=\"display:flex; justify-content:space-between; margin-top:15px;\">\n");
        sb.append("    <div>\n");
        sb.append("      <input type=\"submit\" class=\"btn\" value=\"Update Cart Quantities\">\n");
        sb.append("      <a href=\"/petstore/main.screen\" class=\"btn\" style=\"text-decoration:none; display:inline-block; margin-left:8px;\">Continue Shopping</a>\n");
        sb.append("    </div>\n");
        sb.append("    <div>\n");
        sb.append("      <a href=\"/petstore/enter_order_information.screen\" class=\"btn btn-primary\" style=\"font-size:13px; padding:6px 16px;\">Proceed to Checkout &raquo;</a>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</form>\n");

        return renderTemplate("Shopping Cart", sb.toString(), session, null);
    }

    public static String renderCheckout(UserSession session) {
        if (session.cart.items.isEmpty()) {
            return renderCart(session);
        }

        Customer c = session.customer;
        if (c == null) {
            c = PetStoreDatabase.getInstance().customers.get("j2ee");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Order Checkout</p>\n");
        sb.append("<form action=\"/petstore/order.do\" method=\"POST\">\n");

        sb.append("<table width=\"100%\" border=\"0\" cellpadding=\"4\" cellspacing=\"0\">\n");
        sb.append("  <tr valign=\"top\">\n");

        // Shipping Address
        sb.append("    <td width=\"50%\" style=\"padding-right:15px;\">\n");
        sb.append("      <table class=\"petstore_table\">\n");
        sb.append("        <tr><th>Shipping Address</th></tr>\n");
        sb.append("        <tr><td class=\"petstore_form\">\n");
        sb.append("          First Name: <input type=\"text\" name=\"givenName\" value=\"").append(c != null ? escape(c.givenName) : "XYZ").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Last Name: <input type=\"text\" name=\"familyName\" value=\"").append(c != null ? escape(c.familyName) : "ABC").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Address Line 1: <input type=\"text\" name=\"street1\" value=\"").append(c != null ? escape(c.address.street1) : "1234 Anywhere Street").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Address Line 2: <input type=\"text\" name=\"street2\" value=\"").append(c != null ? escape(c.address.street2) : "Unit 555").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          City: <input type=\"text\" name=\"city\" value=\"").append(c != null ? escape(c.address.city) : "Palo Alto").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          State: <input type=\"text\" name=\"state\" value=\"").append(c != null ? escape(c.address.state) : "CA").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Zip Code: <input type=\"text\" name=\"zip\" value=\"").append(c != null ? escape(c.address.zipCode) : "94303").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Country: <input type=\"text\" name=\"country\" value=\"").append(c != null ? escape(c.address.country) : "USA").append("\" style=\"width:100%;\">\n");
        sb.append("        </td></tr>\n");
        sb.append("      </table>\n");
        sb.append("    </td>\n");

        // Payment Info & Summary
        sb.append("    <td width=\"50%\">\n");
        sb.append("      <table class=\"petstore_table\">\n");
        sb.append("        <tr><th>Payment Information</th></tr>\n");
        sb.append("        <tr><td class=\"petstore_form\">\n");
        sb.append("          Card Type: <select name=\"cardType\" style=\"width:100%;\"><option value=\"Visa\">Visa</option><option value=\"MasterCard\">MasterCard</option><option value=\"Amex\">Amex</option></select><br><br>\n");
        sb.append("          Card Number: <input type=\"text\" name=\"cardNumber\" value=\"").append(c != null ? escape(c.creditCard.cardNumber) : "4111111111111111").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Expiry Date: <input type=\"text\" name=\"expiryDate\" value=\"").append(c != null ? escape(c.creditCard.expiryDate) : "12/28").append("\" style=\"width:100%;\"><br><br>\n");
        sb.append("          Email: <input type=\"email\" name=\"email\" value=\"").append(c != null ? escape(c.email) : "customer@petstore.com").append("\" style=\"width:100%;\">\n");
        sb.append("        </td></tr>\n");
        sb.append("      </table>\n");

        sb.append("      <table class=\"petstore_table\" style=\"margin-top:10px;\">\n");
        sb.append("        <tr><th colspan=\"2\">Order Summary</th></tr>\n");
        sb.append("        <tr><td class=\"petstore_listing\">Items (").append(session.cart.getTotalCount()).append("):</td><td align=\"right\" class=\"petstore_listing\">").append(CURRENCY.format(session.cart.getSubTotal())).append("</td></tr>\n");
        sb.append("        <tr><td class=\"petstore_listing\">Estimated Shipping:</td><td align=\"right\" class=\"petstore_listing\">$0.00</td></tr>\n");
        sb.append("        <tr bgcolor=\"#EEEEFF\"><td class=\"petstore_listing\"><b>Order Total:</b></td><td align=\"right\" class=\"petstore_listing\"><b>").append(CURRENCY.format(session.cart.getSubTotal())).append("</b></td></tr>\n");
        sb.append("      </table>\n");
        sb.append("    </td>\n");
        sb.append("  </tr>\n");
        sb.append("</table>\n");

        sb.append("<div align=\"center\" style=\"margin-top:20px;\">\n");
        sb.append("  <input type=\"submit\" class=\"btn btn-primary\" style=\"font-size:14px; padding:8px 24px;\" value=\"Submit Order &raquo;\">\n");
        sb.append("</div>\n");
        sb.append("</form>\n");

        return renderTemplate("Checkout", sb.toString(), session, null);
    }

    public static String renderOrderComplete(Order order, UserSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"background-color:#EEFFEE; border:1px solid #88CC88; padding:15px; margin-bottom:15px; border-radius:4px;\">\n");
        sb.append("  <p class=\"petstore_title\" style=\"color:#006600; margin:0 0 5px 0;\">&check; Order Submitted Successfully!</p>\n");
        sb.append("  <p class=\"petstore_listing\" style=\"margin:0;\">Thank you for your order. Your Order Confirmation Number is <b>").append(order.orderId).append("</b>.</p>\n");
        sb.append("</div>\n");

        sb.append("<table class=\"petstore_table\">\n");
        sb.append("  <tr><th>Order Details</th><th>Shipping Information</th></tr>\n");
        sb.append("  <tr valign=\"top\">\n");
        sb.append("    <td class=\"petstore_listing\">\n");
        sb.append("      <b>Order ID:</b> ").append(order.orderId).append("<br>\n");
        sb.append("      <b>Date:</b> ").append(order.orderDate).append("<br>\n");
        sb.append("      <b>Status:</b> <span class=\"admin_badge\">").append(order.status).append("</span><br>\n");
        sb.append("      <b>Total:</b> ").append(CURRENCY.format(order.total)).append("<br>\n");
        sb.append("    </td>\n");
        sb.append("    <td class=\"petstore_listing\">\n");
        if (order.shippingAddress != null) {
            sb.append(escape(order.shippingAddress.street1)).append("<br>\n");
            if (!order.shippingAddress.street2.isEmpty()) sb.append(escape(order.shippingAddress.street2)).append("<br>\n");
            sb.append(escape(order.shippingAddress.city)).append(", ").append(escape(order.shippingAddress.state)).append(" ").append(escape(order.shippingAddress.zipCode)).append("<br>\n");
            sb.append(escape(order.shippingAddress.country)).append("\n");
        }
        sb.append("    </td>\n");
        sb.append("  </tr>\n");
        sb.append("</table>\n");

        sb.append("<table class=\"petstore_table\">\n");
        sb.append("  <tr><th>Item</th><th>Quantity</th><th align=\"right\">Unit Price</th><th align=\"right\">Total</th></tr>\n");
        for (CartItem ci : order.items) {
            sb.append("  <tr>\n");
            sb.append("    <td class=\"petstore_listing\">").append(ci.item.id).append(" - ").append(escape(ci.item.attribute1)).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"center\">").append(ci.quantity).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"right\">").append(CURRENCY.format(ci.item.listPrice)).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"right\">").append(CURRENCY.format(ci.getTotal())).append("</td>\n");
            sb.append("  </tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<p class=\"petstore_listing\" style=\"margin-top:15px;\"><a href=\"/petstore/main.screen\">&laquo; Return to Storefront</a></p>\n");

        return renderTemplate("Order Complete", sb.toString(), session, null);
    }

    public static String renderSearch(String keywords, UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        List<Product> results = db.searchProducts(keywords, session.locale);

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Search Results for \"").append(escape(keywords != null ? keywords : "")).append("\"</p>\n");

        if (results.isEmpty()) {
            sb.append("<p class=\"petstore_listing\" style=\"color:#666666;\">No pets found matching your query. Try searching for \"dog\", \"cat\", \"fish\", \"parrot\", etc.</p>\n");
        } else {
            sb.append("<table class=\"petstore_table\">\n");
            sb.append("  <tr><th width=\"100\">Product ID</th><th>Name & Description</th><th width=\"100\">Category</th></tr>\n");
            for (Product p : results) {
                sb.append("  <tr>\n");
                sb.append("    <td class=\"petstore_listing\"><b><a href=\"/petstore/product.screen?product_id=").append(p.id).append("\">").append(p.id).append("</a></b></td>\n");
                sb.append("    <td class=\"petstore_listing\"><a href=\"/petstore/product.screen?product_id=").append(p.id).append("\"><b>").append(escape(p.getName(session.locale))).append("</b></a><br>").append(escape(p.getDescription(session.locale))).append("</td>\n");
                sb.append("    <td class=\"petstore_listing\"><a href=\"/petstore/category.screen?category_id=").append(p.categoryId).append("\">").append(p.categoryId).append("</a></td>\n");
                sb.append("  </tr>\n");
            }
            sb.append("</table>\n");
        }

        return renderTemplate("Search", sb.toString(), session, null);
    }

    public static String renderSignon(UserSession session, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Sign In</p>\n");
        if (errorMsg != null && !errorMsg.isEmpty()) {
            sb.append("<div style=\"color:red; margin-bottom:10px; font-size:12px;\">").append(escape(errorMsg)).append("</div>\n");
        }
        sb.append("<form action=\"/petstore/signon.do\" method=\"POST\">\n");
        sb.append("  <table class=\"petstore_table\" style=\"max-width:350px;\">\n");
        sb.append("    <tr><th>Account Login</th></tr>\n");
        sb.append("    <tr><td class=\"petstore_form\">\n");
        sb.append("      User ID:<br><input type=\"text\" name=\"userId\" value=\"j2ee\" style=\"width:100%;\"><br><br>\n");
        sb.append("      Password:<br><input type=\"password\" name=\"password\" value=\"j2ee\" style=\"width:100%;\"><br><br>\n");
        sb.append("      <input type=\"submit\" class=\"btn btn-primary\" value=\"Sign In\">\n");
        sb.append("    </td></tr>\n");
        sb.append("  </table>\n");
        sb.append("</form>\n");
        sb.append("<p class=\"petstore_listing\">Don't have an account? <a href=\"/petstore/create_customer.screen\">Create New Account</a></p>\n");
        return renderTemplate("Sign In", sb.toString(), session, null);
    }

    public static String renderAdmin(UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Administrator Portal - Order Processing Center (OPC)</p>\n");
        sb.append("<p class=\"petstore_listing\" style=\"color:#555555;\">Manage customer orders, review approval queues, and transition order statuses.</p>\n");

        if (db.orders.isEmpty()) {
            sb.append("<p class=\"petstore_listing\" style=\"color:#888888;\">No orders currently in system. Place an order from the storefront to see it here.</p>\n");
        } else {
            sb.append("<table class=\"petstore_table\">\n");
            sb.append("  <tr>\n");
            sb.append("    <th>Order ID</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th><th>Actions</th>\n");
            sb.append("  </tr>\n");
            for (Order o : db.orders) {
                sb.append("  <tr>\n");
                sb.append("    <td class=\"petstore_listing\"><b>").append(o.orderId).append("</b></td>\n");
                sb.append("    <td class=\"petstore_listing\">").append(escape(o.userId)).append("</td>\n");
                sb.append("    <td class=\"petstore_listing\">").append(o.orderDate).append("</td>\n");
                sb.append("    <td class=\"petstore_listing\">").append(CURRENCY.format(o.total)).append("</td>\n");
                sb.append("    <td class=\"petstore_listing\"><span class=\"admin_badge\">").append(o.status).append("</span></td>\n");
                sb.append("    <td class=\"petstore_listing\">\n");
                sb.append("      <a href=\"/petstore/admin/action?orderId=").append(o.orderId).append("&status=Approved\" class=\"btn\" style=\"color:green;\">Approve</a> \n");
                sb.append("      <a href=\"/petstore/admin/action?orderId=").append(o.orderId).append("&status=Shipped\" class=\"btn\" style=\"color:blue;\">Ship</a> \n");
                sb.append("      <a href=\"/petstore/admin/action?orderId=").append(o.orderId).append("&status=Denied\" class=\"btn\" style=\"color:red;\">Deny</a>\n");
                sb.append("    </td>\n");
                sb.append("  </tr>\n");
            }
            sb.append("</table>\n");
        }

        return renderTemplate("Admin", sb.toString(), session, null);
    }

    public static String renderSupplier(UserSession session) {
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"petstore_title\">Supplier Management Portal - Inventory & Fulfillment</p>\n");
        sb.append("<p class=\"petstore_listing\" style=\"color:#555555;\">Manage inventory levels, re-stock pets, and fulfill purchase orders.</p>\n");

        sb.append("<table class=\"petstore_table\">\n");
        sb.append("  <tr><th>Item ID</th><th>Product</th><th>Unit Cost</th><th>List Price</th><th>Current Stock</th><th>Restock</th></tr>\n");
        for (Item it : db.items.values()) {
            Product p = db.products.get(it.productId);
            sb.append("  <tr>\n");
            sb.append("    <td class=\"petstore_listing\"><b>").append(it.id).append("</b></td>\n");
            sb.append("    <td class=\"petstore_listing\">").append(escape(it.attribute1)).append(" ").append(escape(p != null ? p.getName("en_US") : "")).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\">").append(CURRENCY.format(it.unitCost)).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\">").append(CURRENCY.format(it.listPrice)).append("</td>\n");
            sb.append("    <td class=\"petstore_listing\" align=\"center\"><b>").append(it.stock).append("</b></td>\n");
            sb.append("    <td class=\"petstore_listing\">\n");
            sb.append("      <a href=\"/petstore/supplier/restock?itemId=").append(it.id).append("&qty=25\" class=\"btn\">+25 Units</a>\n");
            sb.append("    </td>\n");
            sb.append("  </tr>\n");
        }
        sb.append("</table>\n");

        return renderTemplate("Supplier", sb.toString(), session, null);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
