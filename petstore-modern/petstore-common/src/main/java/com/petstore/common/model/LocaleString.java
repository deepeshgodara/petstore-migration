package com.petstore.common.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a multilingual localized string.
 */
public final class LocaleString implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_LOCALE = "en_US";

  private final Map<String, String> translations;

  public LocaleString(Map<String, String> translations) {
    this.translations = translations != null ? Map.copyOf(translations) : Map.of();
  }

  /**
   * Resolves the localized text for the requested locale, falling back to English.
   *
   * @param locale the target locale (e.g., "en_US", "ja_JP", "zh_CN")
   * @return the localized string or empty string if not found
   */
  public String resolve(String locale) {
    if (locale != null && translations.containsKey(locale)) {
      return translations.get(locale);
    }
    return translations.getOrDefault(DEFAULT_LOCALE, "");
  }

  public Map<String, String> getTranslations() {
    return translations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocaleString that)) {
      return false;
    }
    return Objects.equals(translations, that.translations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(translations);
  }

  @Override
  public String toString() {
    return "LocaleString{" + translations + "}";
  }
}
