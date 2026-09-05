package com.petstore.catalog.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProductDocument} and embedded {@link ItemDocument}.
 */
class ProductDocumentTest {

  @Test
  @DisplayName("Should populate product with localized attributes and items")
  void shouldPopulateProductAndItems() {
    ItemDocument item1 = new ItemDocument(
        "EST-1",
        new BigDecimal("16.50"),
        new BigDecimal("10.00"),
        Map.of("en_US", "Large", "ja_JP", "大"),
        "images/fish1.gif",
        100
    );

    ItemDocument item2 = new ItemDocument(
        "EST-2",
        new BigDecimal("5.50"),
        new BigDecimal("3.00"),
        Map.of("en_US", "Small", "ja_JP", "小"),
        "images/fish1.gif",
        50
    );

    ProductDocument product = new ProductDocument(
        "FI-SW-01",
        "FISH",
        Map.of("en_US", "Angelfish", "ja_JP", "エンゼルフィッシュ"),
        Map.of("en_US", "Saltwater fish"),
        "images/fish1.gif",
        List.of(item1, item2)
    );

    assertThat(product.getId()).isEqualTo("FI-SW-01");
    assertThat(product.getCategoryId()).isEqualTo("FISH");
    assertThat(product.resolveName("ja_JP")).isEqualTo("エンゼルフィッシュ");
    assertThat(product.resolveName("en_US")).isEqualTo("Angelfish");
    assertThat(product.resolveName("fr_FR")).isEqualTo("Angelfish");
    assertThat(product.getItems()).hasSize(2);

    ItemDocument retrievedItem = product.getItems().get(0);
    assertThat(retrievedItem.getItemId()).isEqualTo("EST-1");
    assertThat(retrievedItem.resolveAttribute("ja_JP")).isEqualTo("大");
    assertThat(retrievedItem.resolveAttribute("en_US")).isEqualTo("Large");
    assertThat(retrievedItem.getListPrice()).isEqualTo(new BigDecimal("16.50"));
  }

  @Test
  @DisplayName("Should verify equals and hashCode by ID")
  void shouldVerifyEqualsAndHashCode() {
    ProductDocument prod1 = new ProductDocument("FI-SW-01", "FISH", null, null, null, null);
    ProductDocument prod2 = new ProductDocument("FI-SW-01", "FISH", null, null, null, null);
    ProductDocument prod3 = new ProductDocument("K9-BD-01", "DOGS", null, null, null, null);

    assertThat(prod1).isEqualTo(prod2);
    assertThat(prod1.hashCode()).isEqualTo(prod2.hashCode());
    assertThat(prod1).isNotEqualTo(prod3);
  }
}
