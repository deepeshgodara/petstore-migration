/**
 * Type definitions for Migration Telemetry, Parity Metrics, and Reconciliation.
 */

export interface DatabaseCounts {
  categories: number;
  products: number;
  orders: number;
}

export interface DiscrepancyDetail {
  field: string;
  legacyValue: string;
  mongoValue: string;
  driftDescription: string;
}

export interface DiscrepancyReport {
  reportId: string;
  entityType: string;
  entityId: string;
  severity: 'INFO' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  details: DiscrepancyDetail[];
  timestamp: string;
}

export interface ParityDashboardResponse {
  parityPercentage: number;
  totalComparisons: number;
  totalMatches: number;
  totalDrifts: number;
  cutoverReady: boolean;
  status: string;
  legacyCounts: DatabaseCounts;
  mongoCounts: DatabaseCounts;
  recentDiscrepancies: DiscrepancyReport[];
  timestamp: string;
}

export interface MigrationSummary {
  categoriesMigrated: number;
  productsMigrated: number;
  ordersMigrated: number;
  durationMs: number;
}
