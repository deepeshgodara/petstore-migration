package com.petstore.migration.reconciliation;

import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.reconciliation.DiscrepancyReport.AlertSeverity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automated discrepancy logger evaluating shadow read audit results against alert thresholds,
 * logging structured diagnostic diffs and caching discrepancy reports for observability dashboards.
 */
@Component
@EnableScheduling
public class DiscrepancyLogger {

  private static final Logger log = LoggerFactory.getLogger(DiscrepancyLogger.class);
  private static final int MAX_STORED_REPORTS = 200;

  private final ShadowReadComparator comparator;
  private final MigrationParityMetrics metrics;
  private final Deque<DiscrepancyReport> reportBuffer = new ConcurrentLinkedDeque<>();

  @Value("${migration.shadow-reconciliation.enabled:true}")
  private boolean reconciliationEnabled;

  public DiscrepancyLogger(ShadowReadComparator comparator, MigrationParityMetrics metrics) {
    this.comparator = comparator;
    this.metrics = metrics;
  }

  /**
   * Evaluates a ComparisonResult, categorizes alert severity, logs actionable alerts,
   * and stores the discrepancy in the audit buffer.
   *
   * @param result comparison audit result
   * @return generated DiscrepancyReport or null if parity matched
   */
  public DiscrepancyReport recordAndLog(ComparisonResult result) {
    if (result == null || result.isMatch()) {
      if (result != null) {
        log.debug("Parity verified for {} [{}] in {} ms",
            result.getEntityType(), result.getEntityId(), result.getDurationNanos() / 1_000_000.0);
      }
      return null;
    }

    AlertSeverity severity = evaluateSeverity(result.getDiscrepancies());
    String reportId = UUID.randomUUID().toString();

    DiscrepancyReport report = new DiscrepancyReport(
        reportId,
        result.getEntityType(),
        result.getEntityId(),
        severity,
        result.getDiscrepancies(),
        Instant.now()
    );

    // Maintain bounded FIFO in-memory buffer
    reportBuffer.addFirst(report);
    while (reportBuffer.size() > MAX_STORED_REPORTS) {
      reportBuffer.pollLast();
    }

    // Emit structured alerts based on severity
    if (severity == AlertSeverity.CRITICAL) {
      log.error("CRITICAL PARITY ALERT: Discrepancy detected for {} [{}]: Severity={}, Details={}",
          result.getEntityType(), result.getEntityId(), severity, result.getDiscrepancies());
    } else {
      log.warn("PARITY DRIFT WARNING: Discrepancy detected for {} [{}]: Severity={}, Details={}",
          result.getEntityType(), result.getEntityId(), severity, result.getDiscrepancies());
    }

    return report;
  }

  /**
   * Background scheduled audit running periodically across all historical and active orders.
   */
  @Scheduled(cron = "${migration.shadow-reconciliation.cron:0 */10 * * * *}")
  public void scheduledReconciliationAudit() {
    if (!reconciliationEnabled) {
      log.debug("Shadow reconciliation audit is disabled via configuration");
      return;
    }

    log.info("Starting scheduled shadow reconciliation audit across order aggregates...");
    List<ComparisonResult> results = comparator.compareAllOrders();

    int matched = 0;
    int drifted = 0;
    for (ComparisonResult result : results) {
      if (result.isMatch()) {
        matched++;
      } else {
        drifted++;
        recordAndLog(result);
      }
    }

    log.info("Scheduled shadow reconciliation audit complete. Total: {}, Matched: {}, Drifted: {}, Parity: {}%",
        results.size(), matched, drifted, String.format("%.2f", metrics.getParityPercentage()));
  }

  /**
   * Determines the alert severity based on the types of discrepancies found.
   */
  public AlertSeverity evaluateSeverity(List<DiscrepancyDetail> details) {
    if (details == null || details.isEmpty()) {
      return AlertSeverity.INFO;
    }

    boolean hasCritical = details.stream().anyMatch(d ->
        "STATUS_MISMATCH".equals(d.discrepancyType())
            || "MISSING_DOCUMENT".equals(d.discrepancyType())
            || "ORPHAN_DOCUMENT".equals(d.discrepancyType())
    );

    if (hasCritical) {
      return AlertSeverity.CRITICAL;
    }

    boolean hasPriceDrift = details.stream().anyMatch(d -> "PRICE_DRIFT".equals(d.discrepancyType()));
    if (hasPriceDrift) {
      return AlertSeverity.HIGH;
    }

    return AlertSeverity.MEDIUM;
  }

  /**
   * Retrieves the latest discrepancy reports up to the specified limit.
   *
   * @param limit maximum number of reports to retrieve
   * @return list of recent DiscrepancyReport objects
   */
  public List<DiscrepancyReport> getRecentReports(int limit) {
    int max = Math.max(0, limit);
    return reportBuffer.stream().limit(max).toList();
  }

  /**
   * Returns all stored discrepancy reports.
   */
  public List<DiscrepancyReport> getAllReports() {
    return new ArrayList<>(reportBuffer);
  }

  public int getTotalReportCount() {
    return reportBuffer.size();
  }

  public void clear() {
    reportBuffer.clear();
  }
}
