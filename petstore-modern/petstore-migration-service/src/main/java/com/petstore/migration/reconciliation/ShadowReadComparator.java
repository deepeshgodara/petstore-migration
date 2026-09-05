package com.petstore.migration.reconciliation;

import com.petstore.catalog.document.ProductDocument;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.order.document.OrderDocument;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Shadow Read Comparator performing asynchronous parity audits between legacy
 * relational data and MongoDB documents, tracking parity metrics and detecting schema or value drift.
 */
@Component
public class ShadowReadComparator {

  private static final Logger log = LoggerFactory.getLogger(ShadowReadComparator.class);

  private final LegacyOrderCursorReader orderReader;
  private final LegacyCatalogCursorReader catalogReader;
  private final MongoTemplate mongoTemplate;
  private final MigrationParityMetrics metrics;

  @Value("${migration.shadow-reconciliation.drift-tolerance-cents:1}")
  private int driftToleranceCents;

  public ShadowReadComparator(
      LegacyOrderCursorReader orderReader,
      LegacyCatalogCursorReader catalogReader,
      MongoTemplate mongoTemplate,
      MigrationParityMetrics metrics) {
    this.orderReader = orderReader;
    this.catalogReader = catalogReader;
    this.mongoTemplate = mongoTemplate;
    this.metrics = metrics;
  }

  /**
   * Compares an order between the legacy relational database and MongoDB.
   *
   * @param orderId the order identifier to audit
   * @return ComparisonResult detailing match status or specific discrepancies
   */
  public ComparisonResult compareOrder(String orderId) {
    long startNanos = System.nanoTime();
    List<DiscrepancyDetail> discrepancies = new ArrayList<>();

    Optional<OrderDocument> legacyOrderOpt = orderReader.readCompleteOrdersAsDocuments()
        .stream()
        .filter(o -> orderId.equals(o.getId()))
        .findFirst();

    OrderDocument mongoOrder = mongoTemplate.findById(orderId, OrderDocument.class, "petstore_orders");

    if (legacyOrderOpt.isEmpty() && mongoOrder == null) {
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("ORDER", orderId, duration);
    }

    if (legacyOrderOpt.isEmpty()) {
      discrepancies.add(new DiscrepancyDetail(
          "existence",
          "ABSENT",
          "PRESENT",
          "ORPHAN_DOCUMENT",
          "Order exists in MongoDB but not found in legacy relational store"
      ));
    } else if (mongoOrder == null) {
      discrepancies.add(new DiscrepancyDetail(
          "existence",
          "PRESENT",
          "ABSENT",
          "MISSING_DOCUMENT",
          "Order exists in legacy relational store but has not replicated to MongoDB"
      ));
    } else {
      OrderDocument legacyOrder = legacyOrderOpt.get();

      // 1. Audit status parity
      if (legacyOrder.getStatus() != mongoOrder.getStatus()) {
        discrepancies.add(new DiscrepancyDetail(
            "status",
            String.valueOf(legacyOrder.getStatus()),
            String.valueOf(mongoOrder.getStatus()),
            "STATUS_MISMATCH",
            "Order status mismatch: Relational=" + legacyOrder.getStatus() + ", Mongo=" + mongoOrder.getStatus()
        ));
      }

      // 2. Audit total price parity within tolerance threshold
      BigDecimal legacyPrice = legacyOrder.getTotalPrice() != null ? legacyOrder.getTotalPrice() : BigDecimal.ZERO;
      BigDecimal mongoPrice = mongoOrder.getTotalPrice() != null ? mongoOrder.getTotalPrice() : BigDecimal.ZERO;
      BigDecimal priceDiff = legacyPrice.subtract(mongoPrice).abs();
      BigDecimal maxAllowedDrift = BigDecimal.valueOf(driftToleranceCents).movePointLeft(2);

      if (priceDiff.compareTo(maxAllowedDrift) > 0) {
        discrepancies.add(new DiscrepancyDetail(
            "totalPrice",
            legacyPrice.toPlainString(),
            mongoPrice.toPlainString(),
            "PRICE_DRIFT",
            "Price drift exceeds tolerance (" + maxAllowedDrift + "): diff=" + priceDiff
        ));
      }

      // 3. Audit customer identifier
      if (!Objects.equals(legacyOrder.getUserId(), mongoOrder.getUserId())) {
        discrepancies.add(new DiscrepancyDetail(
            "userId",
            legacyOrder.getUserId(),
            mongoOrder.getUserId(),
            "USER_MISMATCH",
            "Customer ID mismatch"
        ));
      }

      // 4. Audit locale
      if (!Objects.equals(legacyOrder.getLocale(), mongoOrder.getLocale())) {
        discrepancies.add(new DiscrepancyDetail(
            "locale",
            legacyOrder.getLocale(),
            mongoOrder.getLocale(),
            "LOCALE_MISMATCH",
            "Locale configuration mismatch"
        ));
      }
    }

    long duration = System.nanoTime() - startNanos;
    boolean match = discrepancies.isEmpty();
    metrics.recordShadowComparison(match, duration);

    return match
        ? ComparisonResult.match("ORDER", orderId, duration)
        : ComparisonResult.drift("ORDER", orderId, discrepancies, duration);
  }

  /**
   * Compares a product between the legacy relational catalog and MongoDB.
   *
   * @param productId the product identifier to audit
   * @return ComparisonResult detailing catalog parity
   */
  public ComparisonResult compareProduct(String productId) {
    long startNanos = System.nanoTime();
    List<DiscrepancyDetail> discrepancies = new ArrayList<>();

    List<LegacyProductRow> legacyRows = catalogReader.readAllProducts()
        .stream()
        .filter(p -> productId.equals(p.productId()))
        .toList();

    ProductDocument mongoProduct = mongoTemplate.findById(productId, ProductDocument.class, "petstore_products");

    if (legacyRows.isEmpty() && mongoProduct == null) {
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("PRODUCT", productId, duration);
    }

    if (legacyRows.isEmpty()) {
      discrepancies.add(new DiscrepancyDetail(
          "existence", "ABSENT", "PRESENT", "ORPHAN_DOCUMENT", "Product in Mongo but absent in legacy DB"));
    } else if (mongoProduct == null) {
      discrepancies.add(new DiscrepancyDetail(
          "existence", "PRESENT", "ABSENT", "MISSING_DOCUMENT", "Product in legacy DB but missing in MongoDB"));
    } else {
      String expectedCategory = legacyRows.get(0).categoryId();
      if (!Objects.equals(expectedCategory, mongoProduct.getCategoryId())) {
        discrepancies.add(new DiscrepancyDetail(
            "categoryId",
            expectedCategory,
            mongoProduct.getCategoryId(),
            "CATEGORY_MISMATCH",
            "Category ID does not match"
        ));
      }
    }

    long duration = System.nanoTime() - startNanos;
    boolean match = discrepancies.isEmpty();
    metrics.recordShadowComparison(match, duration);

    return match
        ? ComparisonResult.match("PRODUCT", productId, duration)
        : ComparisonResult.drift("PRODUCT", productId, discrepancies, duration);
  }

  /**
   * Performs an exhaustive parity comparison across all orders in the system.
   *
   * @return list of ComparisonResult for every relational order
   */
  public List<ComparisonResult> compareAllOrders() {
    List<OrderDocument> legacyOrders = orderReader.readCompleteOrdersAsDocuments();
    return legacyOrders.stream()
        .map(o -> compareOrder(o.getId()))
        .toList();
  }
}
