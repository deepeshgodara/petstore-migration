package com.petstore.migration.model;

import java.io.Serializable;

/**
 * Record representing a joined row from legacy CATEGORY and CATEGORY_DETAILS tables.
 */
public record LegacyCategoryRow(
    String catId,
    String locale,
    String name,
    String image,
    String description
) implements Serializable {

  public LegacyCategoryRow {
    catId = catId != null ? catId.trim() : "";
    locale = locale != null ? locale.trim() : "en_US";
    name = name != null ? name.trim() : "";
    image = image != null ? image.trim() : "";
    description = description != null ? description.trim() : "";
  }
}
