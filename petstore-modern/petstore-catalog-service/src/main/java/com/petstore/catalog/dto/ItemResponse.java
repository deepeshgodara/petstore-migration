package com.petstore.catalog.dto;

import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * REST response representation of a specific inventory item (SKU).
 *
 * @param itemId unique item SKU identifier (e.g., "EST-1")
 * @param productId parent product identifier
 * @param productName parent product localized name
 * @param listPrice retail list price
 * @param unitCost wholesale unit cost
 * @param attribute localized attribute description (e.g., "Large", "Adult Male")
 * @param image product item image filename
 * @param inventoryQuantity available inventory quantity in stock
 * @param attributes map of all multilingual attribute descriptions
 */
public record ItemResponse(
    String itemId,
    String productId,
    String productName,
    BigDecimal listPrice,
    BigDecimal unitCost,
    String attribute,
    String image,
    int inventoryQuantity,
    Map<String, String> attributes,
    Map<String, BigDecimal> listPrices,
    Map<String, BigDecimal> unitCosts,
    Map<String, String> descriptions,
    String description
) implements Serializable {

  /**
   * Backward-compatible constructor for existing tests and consumers.
   */
  public ItemResponse(
      String itemId,
      String productId,
      String productName,
      BigDecimal listPrice,
      BigDecimal unitCost,
      String attribute,
      String image,
      int inventoryQuantity,
      Map<String, String> attributes) {
    this(
        itemId,
        productId,
        productName,
        listPrice,
        unitCost,
        attribute,
        image,
        inventoryQuantity,
        attributes,
        Map.of(),
        Map.of(),
        Map.of(),
        ""
    );
  }

  /**
   * Factory method resolving localized text and pricing for the specified locale.
   *
   * @param item the item document
   * @param parentProduct the parent product document
   * @param locale requested locale string (e.g. "en_US", "ja_JP", "zh_CN")
   * @return localized ItemResponse
   */
  public static ItemResponse of(ItemDocument item, ProductDocument parentProduct, String locale) {
    if (item == null) {
      return null;
    }
    String prodId = parentProduct != null ? parentProduct.getId() : "";
    String prodName = parentProduct != null ? parentProduct.resolveName(locale) : "";
    String resolvedImage = (item.getImage() != null && !item.getImage().isBlank())
        ? item.getImage()
        : (parentProduct != null ? parentProduct.getImage() : "");

    return new ItemResponse(
        item.getItemId(),
        prodId,
        prodName,
        item.resolveListPrice(locale),
        item.resolveUnitCost(locale),
        item.resolveAttribute(locale),
        resolvedImage,
        item.getInventoryQuantity(),
        item.getAttributes(),
        item.getListPrices(),
        item.getUnitCosts(),
        item.getDescriptions(),
        item.resolveDescription(locale)
    );
  }
}
