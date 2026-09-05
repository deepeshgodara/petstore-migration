package com.petstore.migration.web;

import com.petstore.migration.model.ParityDashboardResponse;
import com.petstore.migration.service.BaselineMigrationService;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import com.petstore.migration.service.ParityDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for triggering live baseline extraction,
 * inspecting migration status, and monitoring real-time data parity.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

  private final BaselineMigrationService baselineMigrationService;
  private final ParityDashboardService parityDashboardService;

  public MigrationController(
      BaselineMigrationService baselineMigrationService,
      ParityDashboardService parityDashboardService) {
    this.baselineMigrationService = baselineMigrationService;
    this.parityDashboardService = parityDashboardService;
  }

  /**
   * Triggers the idempotent baseline extraction from the legacy database into MongoDB.
   *
   * @return MigrationSummary detailing record counts and execution duration
   */
  @PostMapping("/extract-baseline")
  public ResponseEntity<MigrationSummary> triggerBaselineExtraction() {
    MigrationSummary summary = baselineMigrationService.executeBaselineMigration();
    return ResponseEntity.ok(summary);
  }

  /**
   * Exposes the real-time parity dashboard telemetry and audit discrepancy reports.
   *
   * @param runAudit optional parameter to execute an immediate reconciliation audit
   * @return ParityDashboardResponse snapshot
   */
  @GetMapping("/parity")
  public ResponseEntity<ParityDashboardResponse> getParityDashboard(
      @RequestParam(name = "runAudit", defaultValue = "false") boolean runAudit) {
    ParityDashboardResponse dashboard = parityDashboardService.getDashboardMetrics(runAudit);
    return ResponseEntity.ok(dashboard);
  }

  /**
   * Health and readiness endpoint for the migration worker.
   *
   * @return status confirmation
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Migration Service is active");
  }
}
