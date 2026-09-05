package com.petstore.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Enterprise metrics tracker for live dual-write success rates, shadow read
 * comparisons, latency profiles, and real-time reconciliation parity percentage.
 */
@Component
public class MigrationParityMetrics {

  private final Counter totalDualWrites;
  private final Counter successfulDualWrites;
  private final Counter failedDualWrites;
  private final Counter shadowComparisons;
  private final Counter shadowMatches;
  private final Counter shadowDrifts;
  private final Timer shadowComparisonTimer;

  private final AtomicLong totalComparisonsCount = new AtomicLong(0);
  private final AtomicLong totalMatchesCount = new AtomicLong(0);

  public MigrationParityMetrics(MeterRegistry registry) {
    this.totalDualWrites = Counter.builder("petstore.migration.dualwrite.total")
        .description("Total dual-write events dispatched to secondary store")
        .register(registry);

    this.successfulDualWrites = Counter.builder("petstore.migration.dualwrite.success")
        .description("Successfully acknowledged dual-writes to MongoDB")
        .register(registry);

    this.failedDualWrites = Counter.builder("petstore.migration.dualwrite.failure")
        .description("Failed dual-writes routed to Dead-Letter Queue")
        .register(registry);

    this.shadowComparisons = Counter.builder("petstore.migration.shadow.comparisons")
        .description("Total shadow read comparison audits performed")
        .register(registry);

    this.shadowMatches = Counter.builder("petstore.migration.shadow.matches")
        .description("Shadow read comparisons with 100% payload parity")
        .register(registry);

    this.shadowDrifts = Counter.builder("petstore.migration.shadow.drifts")
        .description("Shadow read comparisons where payload drift was detected")
        .register(registry);

    this.shadowComparisonTimer = Timer.builder("petstore.migration.shadow.duration")
        .description("Execution time of shadow comparison audits")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);

    Gauge.builder("petstore.migration.parity.percentage", this, MigrationParityMetrics::getParityPercentage)
        .description("Real-time data parity percentage between Legacy SoR and MongoDB")
        .baseUnit("percent")
        .register(registry);
  }

  /**
   * Records a successful secondary write into MongoDB.
   */
  public void recordDualWriteSuccess() {
    successfulDualWrites.increment();
    totalDualWrites.increment();
  }

  /**
   * Records a failed secondary write that was routed to the DLQ.
   */
  public void recordDualWriteFailure() {
    failedDualWrites.increment();
    totalDualWrites.increment();
  }

  /**
   * Records the result of an asynchronous shadow read comparison.
   *
   * @param match true if legacy and modern documents match completely
   * @param durationNanos elapsed comparison time in nanoseconds
   */
  public void recordShadowComparison(boolean match, long durationNanos) {
    shadowComparisons.increment();
    totalComparisonsCount.incrementAndGet();
    shadowComparisonTimer.record(durationNanos, TimeUnit.NANOSECONDS);

    if (match) {
      shadowMatches.increment();
      totalMatchesCount.incrementAndGet();
    } else {
      shadowDrifts.increment();
    }
  }

  /**
   * Calculates the current parity percentage across all evaluated comparisons.
   * Defaults to 100.0% when no comparisons have run yet.
   *
   * @return parity percentage between 0.0 and 100.0
   */
  public double getParityPercentage() {
    long comparisons = totalComparisonsCount.get();
    if (comparisons == 0) {
      return 100.0;
    }
    return (double) totalMatchesCount.get() / comparisons * 100.0;
  }

  public long getTotalComparisons() {
    return totalComparisonsCount.get();
  }

  public long getTotalMatches() {
    return totalMatchesCount.get();
  }
}
