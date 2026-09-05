package com.petstore.migration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.migration.model.ParityDashboardResponse;
import com.petstore.migration.model.ParityDashboardResponse.DatabaseCounts;
import com.petstore.migration.service.BaselineMigrationService;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import com.petstore.migration.service.ParityDashboardService;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link MigrationController}.
 */
class MigrationControllerTest {

  private BaselineMigrationService baselineService;
  private ParityDashboardService parityService;
  private MigrationController controller;

  @BeforeEach
  void setUp() {
    baselineService = mock(BaselineMigrationService.class);
    parityService = mock(ParityDashboardService.class);
    controller = new MigrationController(baselineService, parityService);
  }

  @Test
  @DisplayName("Should trigger baseline extraction and return 200 OK with summary")
  void shouldTriggerBaselineExtraction() {
    MigrationSummary summary = new MigrationSummary(5, 16, 4, 100L);
    when(baselineService.executeBaselineMigration()).thenReturn(summary);

    ResponseEntity<MigrationSummary> response = controller.triggerBaselineExtraction();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(summary);
  }

  @Test
  @DisplayName("Should return parity dashboard telemetry on GET /parity")
  void shouldReturnParityDashboard() {
    ParityDashboardResponse dashboard = new ParityDashboardResponse(
        100.0, 4, 4, 0, true, "CUTOVER_READY",
        new DatabaseCounts(5, 16, 4),
        new DatabaseCounts(5, 16, 4),
        Collections.emptyList(),
        Instant.now()
    );

    when(parityService.getDashboardMetrics(false)).thenReturn(dashboard);

    ResponseEntity<ParityDashboardResponse> response = controller.getParityDashboard(false);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(dashboard);
    assertThat(response.getBody().status()).isEqualTo("CUTOVER_READY");
  }

  @Test
  @DisplayName("Should return 200 OK on GET /health")
  void shouldReturnHealthStatus() {
    ResponseEntity<String> response = controller.health();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("active");
  }
}
