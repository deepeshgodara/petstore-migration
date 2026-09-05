package com.petstore.migration.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Record representing a joined row from legacy ITEM, ITEM_DETAILS, and INVENTORY tables.
 */
public record LegacyItemRow(
    String itemId,
    String productId,
    BigDecimal listPrice,
    BigDecimal unitCost,
    String locale,
    String image,
    String description,
    String attribute1,
    int inventoryQuantity
) implements Serializable {

  public LegacyItemRow {
    itemId = itemId != null ? itemId.trim() : "";
    productId = productId != null ? productId.trim() : "";
    listPrice = listPrice != null ? listPrice : BigDecimal.ZERO;
    unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
    locale = locale != null ? locale.trim() : "en_US";
    image = image != null ? image.trim() : "";
    description = description != null ? description.trim() : "";
    attribute1 = attribute1 != null ? attribute1.trim() : "";
  }
}
