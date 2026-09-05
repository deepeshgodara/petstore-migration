package com.petstore.migration.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ShadowReadComparator}.
 */
class ShadowReadComparatorTest {

  private LegacyOrderCursorReader orderReader;
  private LegacyCatalogCursorReader catalogReader;
  private MongoTemplate mongoTemplate;
  private MigrationParityMetrics metrics;
  private ShadowReadComparator comparator;

  @BeforeEach
  void setUp() {
    orderReader = mock(LegacyOrderCursorReader.class);
    catalogReader = mock(LegacyCatalogCursorReader.class);
    mongoTemplate = mock(MongoTemplate.class);
    metrics = new MigrationParityMetrics(new SimpleMeterRegistry());

    comparator = new ShadowReadComparator(orderReader, catalogReader, mongoTemplate, metrics);
    ReflectionTestUtils.setField(comparator, "driftToleranceCents", 1);
  }

  @Test
  @DisplayName("Should report match when relational order and Mongo document match identically")
  void shouldReportMatchWhenOrderIdentical() {
    OrderDocument legacyOrder = new OrderDocument(
        "100113", "j2ee-ja", null, OrderStatus.COMPLETED, BigDecimal.valueOf(125.00), "ja_JP", null, null, null, null);
    OrderDocument mongoOrder = new OrderDocument(
        "100113", "j2ee-ja", null, OrderStatus.COMPLETED, BigDecimal.valueOf(125.00), "ja_JP", null, null, null, null);

    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(List.of(legacyOrder));
    when(mongoTemplate.findById("100113", OrderDocument.class, "petstore_orders")).thenReturn(mongoOrder);

    ComparisonResult result = comparator.compareOrder("100113");

    assertThat(result.isMatch()).isTrue();
    assertThat(result.getDiscrepancies()).isEmpty();
    assertThat(metrics.getParityPercentage()).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Should detect STATUS_MISMATCH when order status differs between stores")
  void shouldDetectStatusMismatch() {
    OrderDocument legacyOrder = new OrderDocument(
        "100114", "shopper", null, OrderStatus.APPROVED, BigDecimal.valueOf(50.00), "en_US", null, null, null, null);
    OrderDocument mongoOrder = new OrderDocument(
        "100114", "shopper", null, OrderStatus.PENDING, BigDecimal.valueOf(50.00), "en_US", null, null, null, null);

    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(List.of(legacyOrder));
    when(mongoTemplate.findById("100114", OrderDocument.class, "petstore_orders")).thenReturn(mongoOrder);

    ComparisonResult result = comparator.compareOrder("100114");

    assertThat(result.isMatch()).isFalse();
    assertThat(result.getDiscrepancies()).hasSize(1);
    assertThat(result.getDiscrepancies().get(0).discrepancyType()).isEqualTo("STATUS_MISMATCH");
    assertThat(metrics.getParityPercentage()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should detect PRICE_DRIFT when order price differs beyond tolerance")
  void shouldDetectPriceDrift() {
    OrderDocument legacyOrder = new OrderDocument(
        "100115", "j2ee", null, OrderStatus.PENDING, BigDecimal.valueOf(100.00), "en_US", null, null, null, null);
    OrderDocument mongoOrder = new OrderDocument(
        "100115", "j2ee", null, OrderStatus.PENDING, BigDecimal.valueOf(95.00), "en_US", null, null, null, null);

    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(List.of(legacyOrder));
    when(mongoTemplate.findById("100115", OrderDocument.class, "petstore_orders")).thenReturn(mongoOrder);

    ComparisonResult result = comparator.compareOrder("100115");

    assertThat(result.isMatch()).isFalse();
    assertThat(result.getDiscrepancies()).anyMatch(d -> "PRICE_DRIFT".equals(d.discrepancyType()));
  }

  @Test
  @DisplayName("Should detect MISSING_DOCUMENT when order missing in MongoDB")
  void shouldDetectMissingDocument() {
    OrderDocument legacyOrder = new OrderDocument(
        "100116", "j2ee", null, OrderStatus.PENDING, BigDecimal.valueOf(25.00), "en_US", null, null, null, null);

    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(List.of(legacyOrder));
    when(mongoTemplate.findById("100116", OrderDocument.class, "petstore_orders")).thenReturn(null);

    ComparisonResult result = comparator.compareOrder("100116");

    assertThat(result.isMatch()).isFalse();
    assertThat(result.getDiscrepancies()).anyMatch(d -> "MISSING_DOCUMENT".equals(d.discrepancyType()));
  }
}
