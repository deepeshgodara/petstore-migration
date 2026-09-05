package com.petstore.migration.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Unit tests for {@link LegacyCatalogCursorReader} using embedded in-memory HSQLDB.
 */
class LegacyCatalogCursorReaderTest {

  private LegacyCatalogCursorReader reader;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    DataSource dataSource = new EmbeddedDatabaseBuilder()
        .generateUniqueName(true)
        .setType(EmbeddedDatabaseType.HSQL)
        .addScript("classpath:schema-test.sql")
        .build();

    jdbcTemplate = new JdbcTemplate(dataSource);
    reader = new LegacyCatalogCursorReader(jdbcTemplate);
  }

  @Test
  @DisplayName("Should read categories across all locales")
  void shouldReadCategories() {
    List<LegacyCategoryRow> categories = reader.readAllCategories();

    assertThat(categories).hasSize(2);
    assertThat(categories.get(0).catId()).isEqualTo("FISH");
    assertThat(categories.get(0).locale()).isEqualTo("en_US");
    assertThat(categories.get(0).name()).isEqualTo("Fish");

    assertThat(categories.get(1).catId()).isEqualTo("FISH");
    assertThat(categories.get(1).locale()).isEqualTo("ja_JP");
    assertThat(categories.get(1).name()).isEqualTo("魚");
  }

  @Test
  @DisplayName("Should read products across all locales")
  void shouldReadProducts() {
    List<LegacyProductRow> products = reader.readAllProducts();

    assertThat(products).hasSize(1);
    assertThat(products.get(0).productId()).isEqualTo("FI-SW-01");
    assertThat(products.get(0).categoryId()).isEqualTo("FISH");
    assertThat(products.get(0).name()).isEqualTo("Angelfish");
  }

  @Test
  @DisplayName("Should read items with joined inventory quantity")
  void shouldReadItems() {
    List<LegacyItemRow> items = reader.readAllItems();

    assertThat(items).hasSize(1);
    LegacyItemRow item = items.get(0);
    assertThat(item.itemId()).isEqualTo("EST-1");
    assertThat(item.productId()).isEqualTo("FI-SW-01");
    assertThat(item.listPrice()).isEqualByComparingTo(new BigDecimal("16.50"));
    assertThat(item.inventoryQuantity()).isEqualTo(150);
  }
}
