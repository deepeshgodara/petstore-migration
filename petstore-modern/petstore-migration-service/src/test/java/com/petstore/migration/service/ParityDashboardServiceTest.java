package com.petstore.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.MongoCollection;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.model.ParityDashboardResponse;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.migration.reconciliation.DiscrepancyLogger;
import com.petstore.order.document.OrderDocument;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Unit tests for {@link ParityDashboardService}.
 */
class ParityDashboardServiceTest {

  private MigrationParityMetrics metrics;
  private DiscrepancyLogger discrepancyLogger;
  private LegacyCatalogCursorReader catalogReader;
  private LegacyOrderCursorReader orderReader;
  private MongoTemplate mongoTemplate;
  private MongoCollection<Document> mockCollection;
  private ParityDashboardService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    metrics = new MigrationParityMetrics(new SimpleMeterRegistry());
    discrepancyLogger = mock(DiscrepancyLogger.class);
    catalogReader = mock(LegacyCatalogCursorReader.class);
    orderReader = mock(LegacyOrderCursorReader.class);
    mongoTemplate = mock(MongoTemplate.class);
    mockCollection = mock(MongoCollection.class);

    when(mongoTemplate.getCollection("petstore_categories")).thenReturn(mockCollection);
    when(mongoTemplate.getCollection("petstore_products")).thenReturn(mockCollection);
    when(mongoTemplate.getCollection("petstore_orders")).thenReturn(mockCollection);

    service = new ParityDashboardService(
        metrics,
        discrepancyLogger,
        catalogReader,
        orderReader,
        mongoTemplate
    );
  }

  @Test
  @DisplayName("Should return CUTOVER_READY when parity is 100% and record counts match")
  void shouldReturnCutoverReadyWhenParityPerfect() {
    when(catalogReader.readAllCategories()).thenReturn(List.of(
        new LegacyCategoryRow("BIRDS", "en_US", "Birds", null, null)
    ));
    when(catalogReader.readAllProducts()).thenReturn(List.of(
        new LegacyProductRow("AV-01", "BIRDS", "en_US", "Parrot", null, null)
    ));
    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(List.of(
        new OrderDocument("100113", "shopper", null, null, null, null, null, null, null, null)
    ));

    when(mockCollection.countDocuments()).thenReturn(1L);
    when(discrepancyLogger.getTotalReportCount()).thenReturn(0);
    when(discrepancyLogger.getRecentReports(20)).thenReturn(Collections.emptyList());

    // Record comparison match in metrics
    metrics.recordShadowComparison(true, 1000L);

    ParityDashboardResponse response = service.getDashboardMetrics(false);

    assertThat(response.parityPercentage()).isEqualTo(100.0);
    assertThat(response.totalComparisons()).isEqualTo(1L);
    assertThat(response.totalMatches()).isEqualTo(1L);
    assertThat(response.totalDrifts()).isZero();
    assertThat(response.cutoverReady()).isTrue();
    assertThat(response.status()).isEqualTo("CUTOVER_READY");
    assertThat(response.legacyCounts().orders()).isEqualTo(1L);
    assertThat(response.mongoCounts().orders()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should trigger on-demand reconciliation audit when requested")
  void shouldTriggerAuditWhenRequested() {
    when(catalogReader.readAllCategories()).thenReturn(Collections.emptyList());
    when(catalogReader.readAllProducts()).thenReturn(Collections.emptyList());
    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(Collections.emptyList());
    when(mockCollection.countDocuments()).thenReturn(0L);

    service.getDashboardMetrics(true);

    verify(discrepancyLogger).scheduledReconciliationAudit();
  }
}
