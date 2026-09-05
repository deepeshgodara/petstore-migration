package com.petstore.migration.model;

import java.io.Serializable;

/**
 * Record representing a joined row from legacy PRODUCT and PRODUCT_DETAILS tables.
 */
public record LegacyProductRow(
    String productId,
    String categoryId,
    String locale,
    String name,
    String image,
    String description
) implements Serializable {

  public LegacyProductRow {
    productId = productId != null ? productId.trim() : "";
    categoryId = categoryId != null ? categoryId.trim() : "";
    locale = locale != null ? locale.trim() : "en_US";
    name = name != null ? name.trim() : "";
    image = image != null ? image.trim() : "";
    description = description != null ? description.trim() : "";
  }
}
