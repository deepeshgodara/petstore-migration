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
  private Map<String, BigDecimal> listPrices = new HashMap<>();
  private Map<String, BigDecimal> unitCosts = new HashMap<>();
  private Map<String, String> descriptions = new HashMap<>();

  public ItemDocument() {}

  public ItemDocument(
      String itemId,
      BigDecimal listPrice,
      BigDecimal unitCost,
      Map<String, String> attributes,
      String image,
      int inventoryQuantity) {
    this(itemId, listPrice, unitCost, attributes, image, inventoryQuantity, null, null, null);
  }

  public ItemDocument(
      String itemId,
      BigDecimal listPrice,
      BigDecimal unitCost,
      Map<String, String> attributes,
      String image,
      int inventoryQuantity,
      Map<String, BigDecimal> listPrices,
      Map<String, BigDecimal> unitCosts,
      Map<String, String> descriptions) {
    this.itemId = itemId;
    this.listPrice = listPrice;
    this.unitCost = unitCost;
    if (attributes != null) {
      this.attributes = new HashMap<>(attributes);
    }
    this.image = image;
    this.inventoryQuantity = inventoryQuantity;
    if (listPrices != null) {
      this.listPrices = new HashMap<>(listPrices);
    }
    if (unitCosts != null) {
      this.unitCosts = new HashMap<>(unitCosts);
    }
    if (descriptions != null) {
      this.descriptions = new HashMap<>(descriptions);
    }
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

  public Map<String, BigDecimal> getListPrices() {
    return Collections.unmodifiableMap(listPrices);
  }

  public void setListPrices(Map<String, BigDecimal> listPrices) {
    this.listPrices = listPrices != null ? new HashMap<>(listPrices) : new HashMap<>();
  }

  public Map<String, BigDecimal> getUnitCosts() {
    return Collections.unmodifiableMap(unitCosts);
  }

  public void setUnitCosts(Map<String, BigDecimal> unitCosts) {
    this.unitCosts = unitCosts != null ? new HashMap<>(unitCosts) : new HashMap<>();
  }

  public Map<String, String> getDescriptions() {
    return Collections.unmodifiableMap(descriptions);
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions != null ? new HashMap<>(descriptions) : new HashMap<>();
  }

  /**
   * Resolves the localized retail list price in the currency of the requested locale.
   *
   * @param locale the target locale (e.g., "en_US", "ja_JP", "zh_CN")
   * @return localized price or fallback to USD canonical price
   */
  public BigDecimal resolveListPrice(String locale) {
    if (locale != null && listPrices.containsKey(locale)) {
      return listPrices.get(locale);
    }
    return listPrices.getOrDefault(DEFAULT_LOCALE, listPrice != null ? listPrice : BigDecimal.ZERO);
  }

  /**
   * Resolves the localized wholesale unit cost in the currency of the requested locale.
   *
   * @param locale the target locale
   * @return localized unit cost or fallback to USD canonical cost
   */
  public BigDecimal resolveUnitCost(String locale) {
    if (locale != null && unitCosts.containsKey(locale)) {
      return unitCosts.get(locale);
    }
    return unitCosts.getOrDefault(DEFAULT_LOCALE, unitCost != null ? unitCost : BigDecimal.ZERO);
  }

  /**
   * Resolves the localized item description for the requested locale.
   *
   * @param locale the target locale
   * @return localized description or fallback
   */
  public String resolveDescription(String locale) {
    if (locale != null && descriptions.containsKey(locale)) {
      return descriptions.get(locale);
    }
    return descriptions.getOrDefault(DEFAULT_LOCALE, "");
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
