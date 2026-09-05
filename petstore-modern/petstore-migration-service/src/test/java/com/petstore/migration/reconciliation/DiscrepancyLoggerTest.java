package com.petstore.migration.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.reconciliation.DiscrepancyReport.AlertSeverity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link DiscrepancyLogger}.
 */
class DiscrepancyLoggerTest {

  private ShadowReadComparator comparator;
  private MigrationParityMetrics metrics;
  private DiscrepancyLogger discrepancyLogger;

  @BeforeEach
  void setUp() {
    comparator = mock(ShadowReadComparator.class);
    metrics = new MigrationParityMetrics(new SimpleMeterRegistry());
    discrepancyLogger = new DiscrepancyLogger(comparator, metrics);
    ReflectionTestUtils.setField(discrepancyLogger, "reconciliationEnabled", true);
  }

  @Test
  @DisplayName("Should return null and not buffer report when comparison matches completely")
  void shouldNotBufferWhenComparisonMatches() {
    ComparisonResult matchResult = ComparisonResult.match("ORDER", "100113", 500_000L);
    DiscrepancyReport report = discrepancyLogger.recordAndLog(matchResult);

    assertThat(report).isNull();
    assertThat(discrepancyLogger.getTotalReportCount()).isZero();
  }

  @Test
  @DisplayName("Should log and buffer CRITICAL alert on STATUS_MISMATCH")
  void shouldLogAndBufferCriticalAlertOnStatusMismatch() {
    DiscrepancyDetail detail = new DiscrepancyDetail(
        "status", "APPROVED", "PENDING", "STATUS_MISMATCH", "Status mismatch detected");
    ComparisonResult driftResult = ComparisonResult.drift(
        "ORDER", "100114", List.of(detail), 1_000_000L);

    DiscrepancyReport report = discrepancyLogger.recordAndLog(driftResult);

    assertThat(report).isNotNull();
    assertThat(report.severity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(report.entityId()).isEqualTo("100114");
    assertThat(discrepancyLogger.getTotalReportCount()).isEqualTo(1);
    assertThat(discrepancyLogger.getRecentReports(10)).hasSize(1);
  }

  @Test
  @DisplayName("Should categorize PRICE_DRIFT as HIGH severity")
  void shouldCategorizePriceDriftAsHighSeverity() {
    DiscrepancyDetail detail = new DiscrepancyDetail(
        "totalPrice", "100.00", "95.00", "PRICE_DRIFT", "Price difference exceeds limit");
    ComparisonResult driftResult = ComparisonResult.drift(
        "ORDER", "100115", List.of(detail), 800_000L);

    DiscrepancyReport report = discrepancyLogger.recordAndLog(driftResult);

    assertThat(report).isNotNull();
    assertThat(report.severity()).isEqualTo(AlertSeverity.HIGH);
  }

  @Test
  @DisplayName("Should execute scheduled audit across all orders")
  void shouldExecuteScheduledAudit() {
    ComparisonResult match1 = ComparisonResult.match("ORDER", "100113", 500_000L);
    DiscrepancyDetail detail = new DiscrepancyDetail(
        "status", "COMPLETED", "PENDING", "STATUS_MISMATCH", "Mismatch");
    ComparisonResult drift1 = ComparisonResult.drift("ORDER", "100114", List.of(detail), 600_000L);

    when(comparator.compareAllOrders()).thenReturn(List.of(match1, drift1));

    discrepancyLogger.scheduledReconciliationAudit();

    verify(comparator).compareAllOrders();
    assertThat(discrepancyLogger.getTotalReportCount()).isEqualTo(1);
  }
}
