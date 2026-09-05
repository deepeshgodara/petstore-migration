package com.petstore.migration.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.migration.model.LegacyLineItemRow;
import com.petstore.migration.model.LegacyOrderRow;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Unit tests for {@link LegacyOrderCursorReader} using embedded in-memory HSQLDB.
 */
class LegacyOrderCursorReaderTest {

  private LegacyOrderCursorReader reader;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    DataSource dataSource = new EmbeddedDatabaseBuilder()
        .generateUniqueName(true)
        .setType(EmbeddedDatabaseType.HSQL)
        .addScript("classpath:schema-test.sql")
        .build();

    jdbcTemplate = new JdbcTemplate(dataSource);
    reader = new LegacyOrderCursorReader(jdbcTemplate);
  }

  @Test
  @DisplayName("Should read raw legacy order rows with joined manager status and customer info")
  void shouldReadRawOrderRows() {
    List<LegacyOrderRow> orderRows = reader.readAllOrderRows();

    assertThat(orderRows).hasSize(1);
    LegacyOrderRow row = orderRows.get(0);
    assertThat(row.poId()).isEqualTo("100115");
    assertThat(row.userId()).isEqualTo("j2ee");
    assertThat(row.status()).isEqualTo("PENDING");
    assertThat(row.poValue()).isEqualByComparingTo(new BigDecimal("38.50"));
    assertThat(row.city()).isEqualTo("Santa Clara");
    assertThat(row.cardType()).isEqualTo("Visa");
  }

  @Test
  @DisplayName("Should read legacy line item rows associated with orders")
  void shouldReadLineItemRows() {
    List<LegacyLineItemRow> lineItems = reader.readAllLineItemRows();

    assertThat(lineItems).hasSize(2);
    LegacyLineItemRow item1 = lineItems.get(0);
    assertThat(item1.orderId()).isEqualTo("100115");
    assertThat(item1.itemId()).isEqualTo("EST-1");
    assertThat(item1.quantity()).isEqualTo(2);
    assertThat(item1.unitPrice()).isEqualByComparingTo(new BigDecimal("16.50"));
  }

  @Test
  @DisplayName("Should read and transform into complete modern OrderDocument aggregates")
  void shouldTransformToOrderDocuments() {
    List<OrderDocument> documents = reader.readCompleteOrdersAsDocuments();

    assertThat(documents).hasSize(1);
    OrderDocument doc = documents.get(0);
    assertThat(doc.getId()).isEqualTo("100115");
    assertThat(doc.getUserId()).isEqualTo("j2ee");
    assertThat(doc.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(doc.getTotalPrice()).isEqualByComparingTo(new BigDecimal("38.50"));
    assertThat(doc.getBilling().getCity()).isEqualTo("Santa Clara");
    assertThat(doc.getBilling().getName()).isEqualTo("Duke Java");
    assertThat(doc.getPayment().getCardNumberMasked()).isEqualTo("XXXX-XXXX-XXXX-4444");
    assertThat(doc.isMigratedFromLegacy()).isTrue();
    assertThat(doc.getLineItems()).hasSize(2);
  }
}
