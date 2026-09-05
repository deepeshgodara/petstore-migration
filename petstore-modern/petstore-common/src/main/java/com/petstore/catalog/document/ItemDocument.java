package com.petstore.catalog.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Embedded document representing an inventory item/SKU within a product.
 */
public class ItemDocument implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_LOCALE = "en_US";

  private String itemId;
  private BigDecimal listPrice;
  private BigDecimal unitCost;
  private Map<String, String> attributes = new HashMap<>();
  private String image;
  private int inventoryQuantity;

  public ItemDocument() {}

  public ItemDocument(
      String itemId,
      BigDecimal listPrice,
      BigDecimal unitCost,
      Map<String, String> attributes,
      String image,
      int inventoryQuantity) {
    this.itemId = itemId;
    this.listPrice = listPrice;
    this.unitCost = unitCost;
    if (attributes != null) {
      this.attributes = new HashMap<>(attributes);
    }
    this.image = image;
    this.inventoryQuantity = inventoryQuantity;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public BigDecimal getListPrice() {
    return listPrice;
  }

  public void setListPrice(BigDecimal listPrice) {
    this.listPrice = listPrice;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public int getInventoryQuantity() {
    return inventoryQuantity;
  }

  public void setInventoryQuantity(int inventoryQuantity) {
    this.inventoryQuantity = inventoryQuantity;
  }

  /**
   * Resolves the localized attribute (e.g., "Large", "Male Adult") for the requested locale.
   *
   * @param locale the target locale
   * @return localized attribute string or empty string
   */
  public String resolveAttribute(String locale) {
    if (locale != null && attributes.containsKey(locale)) {
      return attributes.get(locale);
    }
    return attributes.getOrDefault(DEFAULT_LOCALE, "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ItemDocument that)) {
      return false;
    }
    return Objects.equals(itemId, that.itemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId);
  }

  @Override
  public String toString() {
    return "ItemDocument{"
        + "itemId='" + itemId + '\''
        + ", listPrice=" + listPrice
        + ", inventoryQuantity=" + inventoryQuantity
        + '}';
  }
}
