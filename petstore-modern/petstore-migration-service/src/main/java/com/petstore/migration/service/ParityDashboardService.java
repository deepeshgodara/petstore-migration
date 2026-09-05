package com.petstore.migration.service;

import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.model.ParityDashboardResponse;
import com.petstore.migration.model.ParityDashboardResponse.DatabaseCounts;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.migration.reconciliation.DiscrepancyLogger;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Service aggregating real-time parity telemetry, comparing collection counts
 * across relational and document databases, and evaluating read-cutover readiness.
 */
@Service
public class ParityDashboardService {

  private static final Logger log = LoggerFactory.getLogger(ParityDashboardService.class);

  private final MigrationParityMetrics metrics;
  private final DiscrepancyLogger discrepancyLogger;
  private final LegacyCatalogCursorReader catalogReader;
  private final LegacyOrderCursorReader orderReader;
  private final MongoTemplate mongoTemplate;

  public ParityDashboardService(
      MigrationParityMetrics metrics,
      DiscrepancyLogger discrepancyLogger,
      LegacyCatalogCursorReader catalogReader,
      LegacyOrderCursorReader orderReader,
      MongoTemplate mongoTemplate) {
    this.metrics = metrics;
    this.discrepancyLogger = discrepancyLogger;
    this.catalogReader = catalogReader;
    this.orderReader = orderReader;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Generates a real-time parity dashboard snapshot.
   *
   * @param triggerAudit if true, executes an on-demand reconciliation audit before assembling metrics
   * @return ParityDashboardResponse containing metrics, database counts, and discrepancy alerts
   */
  public ParityDashboardResponse getDashboardMetrics(boolean triggerAudit) {
    if (triggerAudit) {
      log.info("Executing on-demand shadow reconciliation audit requested via dashboard endpoint...");
      discrepancyLogger.scheduledReconciliationAudit();
    }

    // 1. Gather database counts from relational store
    long legacyCategories = catalogReader.readAllCategories()
        .stream()
        .map(LegacyCategoryRow::catId)
        .distinct()
        .count();

    long legacyProducts = catalogReader.readAllProducts()
        .stream()
        .map(LegacyProductRow::productId)
        .distinct()
        .count();

    long legacyOrders = orderReader.readCompleteOrdersAsDocuments().size();
    DatabaseCounts legacyCounts = new DatabaseCounts(legacyCategories, legacyProducts, legacyOrders);

    // 2. Gather database counts from MongoDB collections
    long mongoCategories = getCollectionCount("petstore_categories");
    long mongoProducts = getCollectionCount("petstore_products");
    long mongoOrders = getCollectionCount("petstore_orders");
    DatabaseCounts mongoCounts = new DatabaseCounts(mongoCategories, mongoProducts, mongoOrders);

    // 3. Assemble parity metrics
    long totalComparisons = metrics.getTotalComparisons();
    long totalMatches = metrics.getTotalMatches();
    long totalDrifts = Math.max(0, totalComparisons - totalMatches);
    double parityPercentage = metrics.getParityPercentage();

    boolean cutoverReady = parityPercentage >= 99.99
        && totalComparisons > 0
        && discrepancyLogger.getTotalReportCount() == 0
        && legacyOrders == mongoOrders
        && legacyProducts == mongoProducts;

    String status;
    if (cutoverReady) {
      status = "CUTOVER_READY";
    } else if (discrepancyLogger.getTotalReportCount() > 0 || totalDrifts > 0) {
      status = "DRIFT_DETECTED";
    } else {
      status = "SYNCHRONIZED";
    }

    return new ParityDashboardResponse(
        parityPercentage,
        totalComparisons,
        totalMatches,
        totalDrifts,
        cutoverReady,
        status,
        legacyCounts,
        mongoCounts,
        discrepancyLogger.getRecentReports(20),
        Instant.now()
    );
  }

  private long getCollectionCount(String collectionName) {
    try {
      return mongoTemplate.getCollection(collectionName).countDocuments();
    } catch (Exception e) {
      log.warn("Failed retrieving count for collection {}: {}", collectionName, e.getMessage());
      return 0L;
    }
  }
}
