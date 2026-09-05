package com.petstore.migration.processor;

import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Domain transformation processor that aggregates multi-row relational 3NF
 * catalog records across multiple locales into denormalized MongoDB document aggregates.
 */
@Component
public class CatalogTransformationProcessor {

  /**
   * Transforms multi-row localized category records into unified CategoryDocuments.
   *
   * @param categoryRows relational rows from CATEGORY and CATEGORY_DETAILS
   * @return list of denormalized CategoryDocuments
   */
  public List<CategoryDocument> transformCategories(List<LegacyCategoryRow> categoryRows) {
    if (categoryRows == null || categoryRows.isEmpty()) {
      return List.of();
    }

    Map<String, CategoryBuilder> builders = new LinkedHashMap<>();

    for (LegacyCategoryRow row : categoryRows) {
      builders.computeIfAbsent(row.catId(), id -> new CategoryBuilder(id))
          .addTranslation(row.locale(), row.name(), row.description(), row.image());
    }

    List<CategoryDocument> documents = new ArrayList<>();
    for (CategoryBuilder builder : builders.values()) {
      documents.add(builder.build());
    }
    return documents;
  }

  /**
   * Transforms multi-row localized product and item records into unified ProductDocuments.
   *
   * @param productRows relational rows from PRODUCT and PRODUCT_DETAILS
   * @param itemRows relational rows from ITEM, ITEM_DETAILS, and INVENTORY
   * @return list of denormalized ProductDocuments with embedded ItemDocuments
   */
  public List<ProductDocument> transformProducts(
      List<LegacyProductRow> productRows,
      List<LegacyItemRow> itemRows) {
    if (productRows == null || productRows.isEmpty()) {
      return List.of();
    }

    // 1. Group items by itemId to aggregate multi-lingual attributes
    Map<String, ItemBuilder> itemBuilders = new LinkedHashMap<>();
    if (itemRows != null) {
      for (LegacyItemRow itemRow : itemRows) {
        itemBuilders.computeIfAbsent(itemRow.itemId(), id -> new ItemBuilder(id, itemRow.productId()))
            .addTranslation(itemRow.locale(), itemRow.attribute1(), itemRow.image(), itemRow.description())
            .setPricingAndStock(itemRow.listPrice(), itemRow.unitCost(), itemRow.inventoryQuantity());
      }
    }

    // 2. Group items by their parent productId
    Map<String, List<ItemDocument>> itemsByProduct = new LinkedHashMap<>();
    for (ItemBuilder itemBuilder : itemBuilders.values()) {
      ItemDocument itemDoc = itemBuilder.build();
      itemsByProduct.computeIfAbsent(itemBuilder.productId, k -> new ArrayList<>()).add(itemDoc);
    }

    // 3. Group products by productId and attach embedded items
    Map<String, ProductBuilder> productBuilders = new LinkedHashMap<>();
    for (LegacyProductRow productRow : productRows) {
      productBuilders.computeIfAbsent(
          productRow.productId(),
          id -> new ProductBuilder(id, productRow.categoryId())
      ).addTranslation(productRow.locale(), productRow.name(), productRow.description(), productRow.image());
    }

    List<ProductDocument> documents = new ArrayList<>();
    for (ProductBuilder builder : productBuilders.values()) {
      List<ItemDocument> attachedItems = itemsByProduct.getOrDefault(builder.productId, List.of());
      documents.add(builder.build(attachedItems));
    }
    return documents;
  }

  private static class CategoryBuilder {
    private final String catId;
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private String image;

    CategoryBuilder(String catId) {
      this.catId = catId;
    }

    CategoryBuilder addTranslation(String locale, String name, String description, String image) {
      if (name != null && !name.isBlank()) {
        names.put(locale, name);
      }
      if (description != null && !description.isBlank()) {
        descriptions.put(locale, description);
      }
      if (this.image == null && image != null && !image.isBlank()) {
        this.image = image;
      }
      return this;
    }

    CategoryDocument build() {
      return new CategoryDocument(catId, names, descriptions, image);
    }
  }

  private static class ProductBuilder {
    private final String productId;
    private final String categoryId;
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private String image;

    ProductBuilder(String productId, String categoryId) {
      this.productId = productId;
      this.categoryId = categoryId;
    }

    ProductBuilder addTranslation(String locale, String name, String description, String image) {
      if (name != null && !name.isBlank()) {
        names.put(locale, name);
      }
      if (description != null && !description.isBlank()) {
        descriptions.put(locale, description);
      }
      if (this.image == null && image != null && !image.isBlank()) {
        this.image = image;
      }
      return this;
    }

    ProductDocument build(List<ItemDocument> items) {
      return new ProductDocument(productId, categoryId, names, descriptions, image, items);
    }
  }

  private static class ItemBuilder {
    private final String itemId;
    private final String productId;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private java.math.BigDecimal listPrice = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal unitCost = java.math.BigDecimal.ZERO;
    private String image;
    private int inventoryQuantity;

    ItemBuilder(String itemId, String productId) {
      this.itemId = itemId;
      this.productId = productId;
    }

    ItemBuilder addTranslation(String locale, String attribute, String image, String description) {
      if (attribute != null && !attribute.isBlank()) {
        attributes.put(locale, attribute);
      }
      if (this.image == null && image != null && !image.isBlank()) {
        this.image = image;
      }
      return this;
    }

    ItemBuilder setPricingAndStock(java.math.BigDecimal listPrice, java.math.BigDecimal unitCost, int stock) {
      this.listPrice = listPrice;
      this.unitCost = unitCost;
      this.inventoryQuantity = stock;
      return this;
    }

    ItemDocument build() {
      return new ItemDocument(itemId, listPrice, unitCost, attributes, image, inventoryQuantity);
    }
  }
}
