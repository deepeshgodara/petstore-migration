package com.petstore.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LocaleString}.
 */
class LocaleStringTest {

  @Test
  @DisplayName("Should resolve exact locale when present")
  void shouldResolveExactLocaleWhenPresent() {
    Map<String, String> translations = Map.of(
        "en_US", "Angelfish",
        "ja_JP", "エンゼルフィッシュ",
        "zh_CN", "神仙鱼"
    );
    LocaleString localeString = new LocaleString(translations);

    assertThat(localeString.resolve("ja_JP")).isEqualTo("エンゼルフィッシュ");
    assertThat(localeString.resolve("zh_CN")).isEqualTo("神仙鱼");
    assertThat(localeString.resolve("en_US")).isEqualTo("Angelfish");
  }

  @Test
  @DisplayName("Should fall back to English when requested locale is missing")
  void shouldFallbackToEnglishWhenMissing() {
    Map<String, String> translations = Map.of("en_US", "Goldfish");
    LocaleString localeString = new LocaleString(translations);

    assertThat(localeString.resolve("fr_FR")).isEqualTo("Goldfish");
    assertThat(localeString.resolve(null)).isEqualTo("Goldfish");
  }

  @Test
  @DisplayName("Should return empty string when translations map is null or empty")
  void shouldReturnEmptyStringWhenEmpty() {
    LocaleString emptyLocaleString = new LocaleString(null);

    assertThat(emptyLocaleString.resolve("en_US")).isEmpty();
    assertThat(emptyLocaleString.resolve("de_DE")).isEmpty();
  }

  @Test
  @DisplayName("Should verify equals and hashCode contracts")
  void shouldVerifyEqualsAndHashCode() {
    Map<String, String> map1 = Map.of("en_US", "Dog");
    Map<String, String> map2 = Map.of("en_US", "Dog");
    Map<String, String> map3 = Map.of("en_US", "Cat");

    LocaleString ls1 = new LocaleString(map1);
    LocaleString ls2 = new LocaleString(map2);
    LocaleString ls3 = new LocaleString(map3);

    assertThat(ls1).isEqualTo(ls2);
    assertThat(ls1.hashCode()).isEqualTo(ls2.hashCode());
    assertThat(ls1).isNotEqualTo(ls3);
  }
}
