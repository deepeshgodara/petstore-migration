package com.petstore.migration.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Record representing a legacy LINEITEM row associated with a PURCHASEORDER.
 */
public record LegacyLineItemRow(
    String orderId,
    int lineNumber,
    String itemId,
    String productId,
    String categoryId,
    int quantity,
    BigDecimal unitPrice
) implements Serializable {

  public LegacyLineItemRow {
    orderId = orderId != null ? orderId.trim() : "";
    itemId = itemId != null ? itemId.trim() : "";
    productId = productId != null ? productId.trim() : "";
    categoryId = categoryId != null ? categoryId.trim() : "";
    unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
  }
}
