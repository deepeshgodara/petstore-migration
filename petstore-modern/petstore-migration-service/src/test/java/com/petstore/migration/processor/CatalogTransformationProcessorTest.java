package com.petstore.migration.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogTransformationProcessor}.
 */
class CatalogTransformationProcessorTest {

  private CatalogTransformationProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new CatalogTransformationProcessor();
  }

  @Test
  @DisplayName("Should transform multiple category locale rows into a single CategoryDocument")
  void shouldTransformCategoryRows() {
    List<LegacyCategoryRow> rows = List.of(
        new LegacyCategoryRow("BIRDS", "en_US", "Birds", "birds.gif", "Birds description"),
        new LegacyCategoryRow("BIRDS", "ja_JP", "鳥", "birds.gif", "鳥の説明"),
        new LegacyCategoryRow("BIRDS", "zh_CN", "鸟", "birds.gif", "鸟类描述")
    );

    List<CategoryDocument> categories = processor.transformCategories(rows);

    assertThat(categories).hasSize(1);
    CategoryDocument category = categories.get(0);
    assertThat(category.getId()).isEqualTo("BIRDS");
    assertThat(category.resolveName("en_US")).isEqualTo("Birds");
    assertThat(category.resolveName("ja_JP")).isEqualTo("鳥");
    assertThat(category.resolveName("zh_CN")).isEqualTo("鸟");
    assertThat(category.resolveDescription("en_US")).isEqualTo("Birds description");
    assertThat(category.getImage()).isEqualTo("birds.gif");
  }

  @Test
  @DisplayName("Should transform product rows and embed nested item documents across locales")
  void shouldTransformProductAndItemRows() {
    List<LegacyProductRow> productRows = List.of(
        new LegacyProductRow("FI-SW-01", "FISH", "en_US", "Angelfish", "fish1.gif", "Saltwater fish"),
        new LegacyProductRow("FI-SW-01", "FISH", "ja_JP", "エンゼルフィッシュ", "fish1.gif", "海水魚")
    );

    List<LegacyItemRow> itemRows = List.of(
        new LegacyItemRow("EST-1", "FI-SW-01", new BigDecimal("16.50"), new BigDecimal("10.00"), "en_US", "fish1.gif", "Saltwater fish", "Large", 15000),
        new LegacyItemRow("EST-1", "FI-SW-01", new BigDecimal("16.50"), new BigDecimal("10.00"), "ja_JP", "fish1.gif", "海水魚", "大", 15000),
        new LegacyItemRow("EST-2", "FI-SW-01", new BigDecimal("5.50"), new BigDecimal("3.00"), "en_US", "fish1.gif", "Saltwater fish", "Small", 8500)
    );

    List<ProductDocument> products = processor.transformProducts(productRows, itemRows);

    assertThat(products).hasSize(1);
    ProductDocument product = products.get(0);
    assertThat(product.getId()).isEqualTo("FI-SW-01");
    assertThat(product.getCategoryId()).isEqualTo("FISH");
    assertThat(product.resolveName("en_US")).isEqualTo("Angelfish");
    assertThat(product.resolveName("ja_JP")).isEqualTo("エンゼルフィッシュ");

    assertThat(product.getItems()).hasSize(2);
    ItemDocument est1 = product.getItems().stream().filter(i -> i.getItemId().equals("EST-1")).findFirst().orElseThrow();
    assertThat(est1.getListPrice()).isEqualTo(new BigDecimal("16.50"));
    assertThat(est1.getUnitCost()).isEqualTo(new BigDecimal("10.00"));
    assertThat(est1.resolveAttribute("en_US")).isEqualTo("Large");
    assertThat(est1.resolveAttribute("ja_JP")).isEqualTo("大");
    assertThat(est1.getInventoryQuantity()).isEqualTo(15000);

    ItemDocument est2 = product.getItems().stream().filter(i -> i.getItemId().equals("EST-2")).findFirst().orElseThrow();
    assertThat(est2.resolveAttribute("en_US")).isEqualTo("Small");
    assertThat(est2.getInventoryQuantity()).isEqualTo(8500);
  }
}
