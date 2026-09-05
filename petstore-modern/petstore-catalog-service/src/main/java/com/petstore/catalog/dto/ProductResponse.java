package com.petstore.catalog.dto;

import com.petstore.catalog.document.ProductDocument;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST response representation of a Pet Store product aggregate with embedded items.
 *
 * @param id unique product identifier (e.g., "FI-SW-01")
 * @param categoryId parent category identifier (e.g., "FISH")
 * @param name localized product name based on requested locale
 * @param description localized product description
 * @param image product hero image filename
 * @param items list of child inventory items (SKUs)
 * @param names map of all available localized names
 * @param descriptions map of all available localized descriptions
 */
public record ProductResponse(
    String id,
    String categoryId,
    String name,
    String description,
    String image,
    List<ItemResponse> items,
    Map<String, String> names,
    Map<String, String> descriptions
) implements Serializable {

  /**
   * Factory method resolving localized text for the specified locale.
   *
   * @param document the underlying MongoDB ProductDocument
   * @param locale requested locale string
   * @return localized ProductResponse
   */
  public static ProductResponse of(ProductDocument document, String locale) {
    if (document == null) {
      return null;
    }

    List<ItemResponse> itemResponses = document.getItems() != null
        ? document.getItems().stream()
            .map(item -> ItemResponse.of(item, document, locale))
            .toList()
        : Collections.emptyList();

    return new ProductResponse(
        document.getId(),
        document.getCategoryId(),
        document.resolveName(locale),
        document.resolveDescription(locale),
        document.getImage(),
        itemResponses,
        document.getNames(),
        document.getDescriptions()
    );
  }
}
