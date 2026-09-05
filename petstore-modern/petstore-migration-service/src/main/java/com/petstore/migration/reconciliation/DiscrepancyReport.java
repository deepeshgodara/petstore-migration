package com.petstore.migration.reconciliation;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Encapsulates an actionable reconciliation discrepancy report for alerts and dashboard telemetry.
 *
 * @param reportId unique identifier for the discrepancy report
 * @param entityType entity type (e.g., ORDER, PRODUCT, CATEGORY)
 * @param entityId unique identifier of the entity
 * @param severity alert severity level (INFO, MEDIUM, HIGH, CRITICAL)
 * @param details list of field-level discrepancies
 * @param timestamp time of discrepancy detection
 */
public record DiscrepancyReport(
    String reportId,
    String entityType,
    String entityId,
    AlertSeverity severity,
    List<DiscrepancyDetail> details,
    Instant timestamp
) implements Serializable {

  /**
   * Severity levels for reconciliation alerts.
   */
  public enum AlertSeverity {
    INFO,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}
