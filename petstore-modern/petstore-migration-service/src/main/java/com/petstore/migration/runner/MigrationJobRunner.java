package com.petstore.migration.runner;

import com.petstore.migration.service.BaselineMigrationService;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner that automatically executes the live baseline migration
 * when configured via {@code migration.baseline.auto-run=true}.
 */
@Component
public class MigrationJobRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(MigrationJobRunner.class);

  private final BaselineMigrationService baselineMigrationService;

  @Value("${migration.baseline.auto-run:false}")
  private boolean autoRun;

  public MigrationJobRunner(BaselineMigrationService baselineMigrationService) {
    this.baselineMigrationService = baselineMigrationService;
  }

  @Override
  public void run(String... args) {
    if (autoRun) {
      log.info("Executing automated startup baseline migration...");
      MigrationSummary summary = baselineMigrationService.executeBaselineMigration();
      log.info("Automated baseline migration finished: {}", summary);
    } else {
      log.info("Automated baseline migration disabled. Trigger manually via POST /api/v1/migration/extract-baseline");
    }
  }
}
