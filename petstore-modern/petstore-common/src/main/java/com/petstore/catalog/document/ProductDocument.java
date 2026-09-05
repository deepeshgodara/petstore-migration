package com.petstore.catalog.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a Pet Store product and its inventory items.
 * Maps to the {@code petstore_products} collection.
 */
@Document(collection = "petstore_products")
public class ProductDocument implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_LOCALE = "en_US";

  @Id
  private String id;

  @Indexed
  private String categoryId;

  private Map<String, String> names = new HashMap<>();
  private Map<String, String> descriptions = new HashMap<>();
  private String image;
  private List<ItemDocument> items = new ArrayList<>();

  public ProductDocument() {}

  public ProductDocument(
      String id,
      String categoryId,
      Map<String, String> names,
      Map<String, String> descriptions,
      String image,
      List<ItemDocument> items) {
    this.id = id;
    this.categoryId = categoryId;
    if (names != null) {
      this.names = new HashMap<>(names);
    }
    if (descriptions != null) {
      this.descriptions = new HashMap<>(descriptions);
    }
    this.image = image;
    if (items != null) {
      this.items = new ArrayList<>(items);
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public Map<String, String> getNames() {
    return Collections.unmodifiableMap(names);
  }

  public void setNames(Map<String, String> names) {
    this.names = names != null ? new HashMap<>(names) : new HashMap<>();
  }

  public Map<String, String> getDescriptions() {
    return Collections.unmodifiableMap(descriptions);
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions != null ? new HashMap<>(descriptions) : new HashMap<>();
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public List<ItemDocument> getItems() {
    return Collections.unmodifiableList(items);
  }

  public void setItems(List<ItemDocument> items) {
    this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
  }

  /**
   * Adds an item to this product aggregate.
   *
   * @param item the inventory item
   */
  public void addItem(ItemDocument item) {
    if (item != null) {
      this.items.add(item);
    }
  }

  /**
   * Resolves the localized product name with fallback to English.
   *
   * @param locale the target locale
   * @return localized product name
   */
  public String resolveName(String locale) {
    if (locale != null && names.containsKey(locale)) {
      return names.get(locale);
    }
    return names.getOrDefault(DEFAULT_LOCALE, id);
  }

  /**
   * Resolves the localized product description with fallback to English.
   *
   * @param locale the target locale
   * @return localized product description
   */
  public String resolveDescription(String locale) {
    if (locale != null && descriptions.containsKey(locale)) {
      return descriptions.get(locale);
    }
    return descriptions.getOrDefault(DEFAULT_LOCALE, "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProductDocument that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ProductDocument{"
        + "id='" + id + '\''
        + ", categoryId='" + categoryId + '\''
        + ", names=" + names
        + ", itemCount=" + items.size()
        + '}';
  }
}
