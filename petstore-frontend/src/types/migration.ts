/**
 * Type definitions for Migration Telemetry, Parity Metrics, and Reconciliation.
 */

export interface DatabaseCounts {
  categories: number;
  products: number;
  orders: number;
  users?: number;
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
  categoriesCount?: number;
  productsCount?: number;
  ordersCount?: number;
  usersCount?: number;
  categoriesMigrated?: number;
  productsMigrated?: number;
  ordersMigrated?: number;
  usersMigrated?: number;
  executionDurationMs?: number;
  durationMs?: number;
}

export interface MongoDiagnosticsResponse {
  status: string;
  version: string;
  uptimeSeconds: number;
  replicaSetName: string;
  isPrimary: boolean;
  connections: {
    current?: number;
    available?: number;
    active?: number;
    totalCreated?: number;
  };
  opcounters: {
    insert?: number;
    query?: number;
    update?: number;
    delete?: number;
    command?: number;
  };
  memory: {
    residentMb?: number;
    virtualMb?: number;
  };
  wiredTigerCache: {
    bytesInCache?: number;
    dirtyBytes?: number;
    maxBytes?: number;
  };
  databaseStats: {
    collections?: number;
    objects?: number;
    dataSize?: number;
    storageSize?: number;
    indexSize?: number;
  };
  collectionStats: Record<string, any>;
  compassConnectionUri: string;
}

