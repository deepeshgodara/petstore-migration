import { MigrationSummary, MongoDiagnosticsResponse, ParityDashboardResponse } from '../types/migration';

/**
 * Service providing typed HTTP methods for the Pet Store Migration & Parity API.
 */
class MigrationApiService {
  private readonly baseUrl = '/api/v1/migration';

  /**
   * Retrieves real-time parity telemetry and discrepancy reports.
   * @param runAudit if true, triggers an immediate shadow reconciliation audit before returning
   */
  async getParityMetrics(runAudit: boolean = false): Promise<ParityDashboardResponse> {
    const response = await fetch(`${this.baseUrl}/parity?runAudit=${runAudit}`);
    if (!response.ok) {
      throw new Error(`Failed to load parity metrics: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Triggers an idempotent baseline extraction from legacy relational DB into MongoDB.
   */
  async triggerBaselineExtraction(): Promise<MigrationSummary> {
    const response = await fetch(`${this.baseUrl}/extract-baseline`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Baseline extraction failed: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Drops modern MongoDB collections to reset to an empty slate for demonstration.
   */
  async cleanSlate(): Promise<{ status: string; message: string }> {
    const response = await fetch(`${this.baseUrl}/clean-slate`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Clean slate failed: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Retrieves real-time MongoDB engine telemetry, cache metrics, and Compass connection parameters.
   */
  async getMongoDiagnostics(): Promise<MongoDiagnosticsResponse> {
    const response = await fetch(`${this.baseUrl}/mongo-diagnostics`);
    if (!response.ok) {
      throw new Error(`Failed to load MongoDB diagnostics: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }
}

export const migrationService = new MigrationApiService();
