package com.petstore.catalog.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryDocument}.
 */
class CategoryDocumentTest {

  @Test
  @DisplayName("Should resolve localized category name with fallback to English")
  void shouldResolveLocalizedName() {
    CategoryDocument category = new CategoryDocument(
        "FISH",
        Map.of("en_US", "Fish", "ja_JP", "魚", "zh_CN", "鱼"),
        Map.of("en_US", "Freshwater and saltwater fish"),
        "images/fish_icon.gif"
    );

    assertThat(category.getId()).isEqualTo("FISH");
    assertThat(category.resolveName("ja_JP")).isEqualTo("魚");
    assertThat(category.resolveName("zh_CN")).isEqualTo("鱼");
    assertThat(category.resolveName("en_US")).isEqualTo("Fish");
    assertThat(category.resolveName("de_DE")).isEqualTo("Fish");
    assertThat(category.resolveDescription("en_US")).isEqualTo("Freshwater and saltwater fish");
  }

  @Test
  @DisplayName("Should verify equals and hashCode by ID")
  void shouldVerifyEqualsAndHashCode() {
    CategoryDocument cat1 = new CategoryDocument("BIRDS", null, null, null);
    CategoryDocument cat2 = new CategoryDocument("BIRDS", null, null, null);
    CategoryDocument cat3 = new CategoryDocument("DOGS", null, null, null);

    assertThat(cat1).isEqualTo(cat2);
    assertThat(cat1.hashCode()).isEqualTo(cat2.hashCode());
    assertThat(cat1).isNotEqualTo(cat3);
  }
}
