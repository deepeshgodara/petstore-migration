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
    Map<String, String> attributes
) implements Serializable {

  /**
   * Factory method resolving localized text for the specified locale.
   *
   * @param item the item document
   * @param parentProduct the parent product document
   * @param locale requested locale string
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
        item.getListPrice(),
        item.getUnitCost(),
        item.resolveAttribute(locale),
        resolvedImage,
        item.getInventoryQuantity(),
        item.getAttributes()
    );
  }
}
