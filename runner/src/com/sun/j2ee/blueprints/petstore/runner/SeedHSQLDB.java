package com.sun.j2ee.blueprints.petstore.runner;

import com.sun.j2ee.blueprints.petstore.runner.PetStoreModels.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SeedHSQLDB {
    public static void main(String[] args) throws Exception {
        File populateXml = new File("src/apps/petstore/src/docroot/populate/Populate-UTF8.xml");
        PetStoreDatabase db = PetStoreDatabase.getInstance();
        db.initialize(populateXml);

        List<String> sqls = new ArrayList<>();
        sqls.add("SET SCHEMA PUBLIC;");

        // Categories
        for (Category cat : db.categories.values()) {
            sqls.add("INSERT INTO PUBLIC.CATEGORY VALUES('" + cat.id + "');");
            for (Map.Entry<String, String> entry : cat.names.entrySet()) {
                String loc = entry.getKey();
                String name = entry.getValue().replace("'", "''");
                String desc = cat.descriptions.getOrDefault(loc, "").replace("'", "''");
                String img = "images/category_" + cat.id.toLowerCase() + ".gif";
                sqls.add("INSERT INTO PUBLIC.CATEGORY_DETAILS VALUES('" + cat.id + "','" + name + "','" + img + "','" + desc + "','" + loc + "');");
            }
        }

        // Products
        for (Product prod : db.products.values()) {
            sqls.add("INSERT INTO PUBLIC.PRODUCT VALUES('" + prod.id + "','" + prod.categoryId + "');");
            for (Map.Entry<String, String> entry : prod.names.entrySet()) {
                String loc = entry.getKey();
                String name = entry.getValue().replace("'", "''");
                String desc = prod.descriptions.getOrDefault(loc, "").replace("'", "''");
                String img = (prod.image != null ? prod.image : "").replace("'", "''");
                sqls.add("INSERT INTO PUBLIC.PRODUCT_DETAILS VALUES('" + prod.id + "','" + loc + "','" + name + "','" + img + "','" + desc + "');");
            }
        }

        // Items
        for (Item item : db.items.values()) {
            sqls.add("INSERT INTO PUBLIC.ITEM VALUES('" + item.id + "','" + item.productId + "');");
            String img = (item.image != null ? item.image : "").replace("'", "''");
            String desc = (item.attribute1 != null ? item.attribute1 : item.id).replace("'", "''");
            String attr1 = (item.attribute1 != null ? item.attribute1 : "").replace("'", "''");
            sqls.add(String.format(Locale.US, "INSERT INTO PUBLIC.ITEM_DETAILS VALUES('%s',%.2f,%.2f,'en_US','%s','%s','%s',null,null,null,null);",
                    item.id, item.listPrice, item.unitCost, img, desc, attr1));
        }

        File scriptFile = new File("legacy_container/tomee/data/petstoredb.script");
        List<String> lines = Files.readAllLines(scriptFile.toPath());
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith("INSERT INTO PUBLIC.CATEGORY") &&
                !line.startsWith("INSERT INTO PUBLIC.PRODUCT") &&
                !line.startsWith("INSERT INTO PUBLIC.ITEM") &&
                !line.equals("SET SCHEMA PUBLIC;")) {
                cleaned.add(line);
            }
        }
        cleaned.addAll(sqls);
        Files.write(scriptFile.toPath(), cleaned);
        System.out.println("Injected " + sqls.size() + " rows with SET SCHEMA PUBLIC into " + scriptFile.getAbsolutePath());
    }
}
