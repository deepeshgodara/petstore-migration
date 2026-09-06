package com.petstore.catalog.dto;

import java.io.Serializable;

/**
 * Request payload for updating inventory stock levels of a catalog item SKU.
 *
 * @param quantity new non-negative inventory stock quantity
 */
public record UpdateInventoryRequest(
    int quantity
) implements Serializable {

  public UpdateInventoryRequest {
    if (quantity < 0) {
      throw new IllegalArgumentException("Inventory quantity cannot be negative");
    }
  }
}
