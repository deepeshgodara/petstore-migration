package com.petstore.catalog.document;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a Pet Store category with multi-language support.
 * Maps to the {@code petstore_categories} collection.
 */
@Document(collection = "petstore_categories")
public class CategoryDocument implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_LOCALE = "en_US";

  @Id
  private String id;

  private Map<String, String> names = new HashMap<>();
  private Map<String, String> descriptions = new HashMap<>();
  private String image;

  public CategoryDocument() {}

  public CategoryDocument(
      String id,
      Map<String, String> names,
      Map<String, String> descriptions,
      String image) {
    this.id = id;
    if (names != null) {
      this.names = new HashMap<>(names);
    }
    if (descriptions != null) {
      this.descriptions = new HashMap<>(descriptions);
    }
    this.image = image;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, String> getNames() {
    return Collections.unmodifiableMap(names);
  }

  public void setNames(Map<String, String> names) {
    this.names = names != null ? new HashMap<>(names) : new HashMap<>();
  }

  public Map<String, String> getDescriptions() {
    return Collections.unmodifiableMap(descriptions);
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions != null ? new HashMap<>(descriptions) : new HashMap<>();
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  /**
   * Resolves the localized category name with fallback to English.
   *
   * @param locale the requested locale string (e.g., "en_US", "ja_JP", "zh_CN")
   * @return the localized name or fallback
   */
  public String resolveName(String locale) {
    if (locale != null && names.containsKey(locale)) {
      return names.get(locale);
    }
    return names.getOrDefault(DEFAULT_LOCALE, id);
  }

  /**
   * Resolves the localized category description with fallback to English.
   *
   * @param locale the requested locale string
   * @return the localized description or fallback
   */
  public String resolveDescription(String locale) {
    if (locale != null && descriptions.containsKey(locale)) {
      return descriptions.get(locale);
    }
    return descriptions.getOrDefault(DEFAULT_LOCALE, "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CategoryDocument that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "CategoryDocument{"
        + "id='" + id + '\''
        + ", names=" + names
        + ", image='" + image + '\''
        + '}';
  }
}
