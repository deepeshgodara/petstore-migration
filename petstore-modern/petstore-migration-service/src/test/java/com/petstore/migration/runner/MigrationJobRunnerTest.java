package com.petstore.migration.runner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.migration.service.BaselineMigrationService;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link MigrationJobRunner}.
 */
class MigrationJobRunnerTest {

  @Test
  @DisplayName("Should execute migration when auto-run is enabled")
  void shouldExecuteWhenAutoRunEnabled() {
    BaselineMigrationService service = mock(BaselineMigrationService.class);
    when(service.executeBaselineMigration()).thenReturn(
        new MigrationSummary(5, 16, 4, 50L)
    );

    MigrationJobRunner runner = new MigrationJobRunner(service);
    ReflectionTestUtils.setField(runner, "autoRun", true);

    runner.run();

    verify(service).executeBaselineMigration();
  }

  @Test
  @DisplayName("Should skip migration when auto-run is disabled")
  void shouldSkipWhenAutoRunDisabled() {
    BaselineMigrationService service = mock(BaselineMigrationService.class);

    MigrationJobRunner runner = new MigrationJobRunner(service);
    ReflectionTestUtils.setField(runner, "autoRun", false);

    runner.run();

    verify(service, never()).executeBaselineMigration();
  }
}
