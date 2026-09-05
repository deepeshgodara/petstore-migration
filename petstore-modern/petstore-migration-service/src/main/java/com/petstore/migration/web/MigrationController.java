package com.petstore.migration.web;

import com.petstore.migration.service.BaselineMigrationService;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for triggering live baseline extraction
 * and inspecting migration status and data parity.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

  private final BaselineMigrationService baselineMigrationService;

  public MigrationController(BaselineMigrationService baselineMigrationService) {
    this.baselineMigrationService = baselineMigrationService;
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
   * Health and readiness endpoint for the migration worker.
   *
   * @return status confirmation
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Migration Service is active");
  }
}
