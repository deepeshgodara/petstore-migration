package com.petstore.migration.model;

import com.petstore.migration.reconciliation.DiscrepancyReport;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Real-time telemetry dashboard model reporting live migration parity metrics,
 * comparison audit counts, database record parity, and recent discrepancy alerts.
 *
 * @param parityPercentage percentage of audits matching identically (0.0 to 100.0)
 * @param totalComparisons total shadow comparison audits executed
 * @param totalMatches total comparisons with 100% data fidelity
 * @param totalDrifts total comparisons where data or schema drift was detected
 * @param cutoverReady whether parity meets the threshold for production read cutover
 * @param status operational status (e.g., CUTOVER_READY, DRIFT_DETECTED, SYNCHRONIZED)
 * @param legacyCounts record counts from legacy relational database
 * @param mongoCounts document counts from MongoDB target collections
 * @param recentDiscrepancies latest actionable discrepancy reports
 * @param timestamp generation timestamp
 */
public record ParityDashboardResponse(
    double parityPercentage,
    long totalComparisons,
    long totalMatches,
    long totalDrifts,
    boolean cutoverReady,
    String status,
    DatabaseCounts legacyCounts,
    DatabaseCounts mongoCounts,
    List<DiscrepancyReport> recentDiscrepancies,
    Instant timestamp
) implements Serializable {

  /**
   * Database entity count summary.
   */
  public record DatabaseCounts(
      long categories,
      long products,
      long orders
  ) implements Serializable {}
}
