package com.petstore.catalog.dto;

import com.petstore.catalog.document.CategoryDocument;
import java.io.Serializable;
import java.util.Map;

/**
 * REST response representation of a Pet Store category with localized text resolution.
 *
 * @param id unique category identifier (e.g., "BIRDS", "FISH")
 * @param name localized category name based on requested locale
 * @param description localized category description
 * @param image category hero icon filename
 * @param names map of all available localized names
 * @param descriptions map of all available localized descriptions
 */
public record CategoryResponse(
    String id,
    String name,
    String description,
    String image,
    Map<String, String> names,
    Map<String, String> descriptions
) implements Serializable {

  /**
   * Factory method resolving localized text for the specified locale.
   *
   * @param document the underlying MongoDB CategoryDocument
   * @param locale requested locale string (e.g., "en_US", "ja_JP", "zh_CN")
   * @return localized CategoryResponse
   */
  public static CategoryResponse of(CategoryDocument document, String locale) {
    if (document == null) {
      return null;
    }
    return new CategoryResponse(
        document.getId(),
        document.resolveName(locale),
        document.resolveDescription(locale),
        document.getImage(),
        document.getNames(),
        document.getDescriptions()
    );
  }
}
