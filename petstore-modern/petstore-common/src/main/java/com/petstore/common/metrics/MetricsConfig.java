package com.petstore.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common configuration for Micrometer and Prometheus telemetry.
 */
@Configuration
public class MetricsConfig {

  /**
   * Enriches all outgoing metrics with standard enterprise tags.
   *
   * @return a MeterRegistryCustomizer bean
   */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config().commonTags(
        "platform", "petstore-modern",
        "migration.strategy", "dual-write-shadow-reconciliation"
    );
  }
}
