package com.petstore.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MigrationParityMetrics}.
 */
class MigrationParityMetricsTest {

  private SimpleMeterRegistry meterRegistry;
  private MigrationParityMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metrics = new MigrationParityMetrics(meterRegistry);
  }

  @Test
  @DisplayName("Should initialize parity percentage at 100% when no comparisons have run")
  void shouldInitializeParityAt100() {
    assertThat(metrics.getParityPercentage()).isEqualTo(100.0);
    assertThat(metrics.getTotalComparisons()).isZero();
    assertThat(metrics.getTotalMatches()).isZero();
  }

  @Test
  @DisplayName("Should track successful and failed dual-writes")
  void shouldTrackDualWrites() {
    metrics.recordDualWriteSuccess();
    metrics.recordDualWriteSuccess();
    metrics.recordDualWriteFailure();

    double total = meterRegistry.get("petstore.migration.dualwrite.total").counter().count();
    double success = meterRegistry.get("petstore.migration.dualwrite.success").counter().count();
    double failure = meterRegistry.get("petstore.migration.dualwrite.failure").counter().count();

    assertThat(total).isEqualTo(3.0);
    assertThat(success).isEqualTo(2.0);
    assertThat(failure).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should calculate parity percentage accurately from shadow read comparisons")
  void shouldCalculateParityPercentage() {
    // 9 matches and 1 drift = 90.0% parity
    for (int i = 0; i < 9; i++) {
      metrics.recordShadowComparison(true, 1_000_000L);
    }
    metrics.recordShadowComparison(false, 1_500_000L);

    assertThat(metrics.getTotalComparisons()).isEqualTo(10L);
    assertThat(metrics.getTotalMatches()).isEqualTo(9L);
    assertThat(metrics.getParityPercentage()).isCloseTo(90.0, within(0.001));

    double gaugeValue = meterRegistry.get("petstore.migration.parity.percentage").gauge().value();
    assertThat(gaugeValue).isCloseTo(90.0, within(0.001));
  }
}
