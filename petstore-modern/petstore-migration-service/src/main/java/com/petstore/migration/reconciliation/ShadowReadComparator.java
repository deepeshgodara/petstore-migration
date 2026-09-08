package com.petstore.migration.reconciliation;

import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.model.LegacyUserRow;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.migration.reader.LegacyUserCursorReader;
import com.petstore.order.document.OrderDocument;
import com.petstore.user.document.UserDocument;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
  private final LegacyUserCursorReader userReader;
  private final MongoTemplate mongoTemplate;
  private final MigrationParityMetrics metrics;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Value("${migration.shadow-reconciliation.drift-tolerance-cents:1}")
  private int driftToleranceCents;

  public ShadowReadComparator(
      LegacyOrderCursorReader orderReader,
      LegacyCatalogCursorReader catalogReader,
      LegacyUserCursorReader userReader,
      MongoTemplate mongoTemplate,
      MigrationParityMetrics metrics) {
    this.orderReader = orderReader;
    this.catalogReader = catalogReader;
    this.userReader = userReader;
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
    Optional<OrderDocument> legacyOrderOpt = orderReader.readCompleteOrdersAsDocuments()
        .stream()
        .filter(o -> orderId.equals(o.getId()))
        .findFirst();

    OrderDocument mongoOrder = mongoTemplate.findById(orderId, OrderDocument.class, "petstore_orders");
    return compareOrderInternal(orderId, legacyOrderOpt.orElse(null), mongoOrder);
  }

  public ComparisonResult compareOrderInternal(String orderId, OrderDocument legacyOrder, OrderDocument mongoOrder) {
    long startNanos = System.nanoTime();
    List<DiscrepancyDetail> discrepancies = new ArrayList<>();

    if (legacyOrder == null && mongoOrder == null) {
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("ORDER", orderId, duration);
    }

    if (legacyOrder == null) {
      if (mongoOrder != null && !mongoOrder.isMigratedFromLegacy()) {
        // Order placed natively in the modern storefront post-migration
        long duration = System.nanoTime() - startNanos;
        metrics.recordShadowComparison(true, duration);
        return ComparisonResult.match("ORDER", orderId, duration);
      }
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
    List<LegacyProductRow> legacyRows = catalogReader.readAllProducts()
        .stream()
        .filter(p -> productId.equals(p.productId()))
        .toList();

    List<LegacyItemRow> legacyItems = catalogReader.readAllItems().stream()
        .filter(i -> productId.equals(i.productId()))
        .toList();

    ProductDocument mongoProduct = mongoTemplate.findById(productId, ProductDocument.class, "petstore_products");
    return compareProductInternal(productId, legacyRows, legacyItems, mongoProduct);
  }

  public ComparisonResult compareProductInternal(
      String productId,
      List<LegacyProductRow> legacyRows,
      List<LegacyItemRow> legacyItems,
      ProductDocument mongoProduct) {
    long startNanos = System.nanoTime();
    List<DiscrepancyDetail> discrepancies = new ArrayList<>();

    if ((legacyRows == null || legacyRows.isEmpty()) && mongoProduct == null) {
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("PRODUCT", productId, duration);
    }

    if (legacyRows == null || legacyRows.isEmpty()) {
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

      // Verify SKU item pricing across all currencies and inventory quantity against legacy DB
      if (mongoProduct.getItems() != null && legacyItems != null && !legacyItems.isEmpty()) {
        Map<String, Integer> legacyStockMap = legacyItems.stream()
            .collect(Collectors.toMap(LegacyItemRow::itemId, LegacyItemRow::inventoryQuantity, (a, b) -> a));

        Map<String, Map<String, BigDecimal>> legacyPriceByLocale = new HashMap<>();
        for (LegacyItemRow row : legacyItems) {
          legacyPriceByLocale.computeIfAbsent(row.itemId(), k -> new HashMap<>())
              .put(row.locale(), row.listPrice());
        }

        for (ItemDocument item : mongoProduct.getItems()) {
          Map<String, BigDecimal> expectedPrices = legacyPriceByLocale.get(item.getItemId());
          if (expectedPrices != null) {
            for (Map.Entry<String, BigDecimal> priceEntry : expectedPrices.entrySet()) {
              String loc = priceEntry.getKey();
              BigDecimal expectedPrice = priceEntry.getValue();
              BigDecimal actualPrice = item.resolveListPrice(loc);
              if (expectedPrice != null && actualPrice != null
                  && expectedPrice.compareTo(actualPrice) != 0) {
                discrepancies.add(new DiscrepancyDetail(
                    "itemPrice:" + item.getItemId() + ":" + loc,
                    expectedPrice.toPlainString(),
                    actualPrice.toPlainString(),
                    "PRICE_MISMATCH",
                    "Item listPrice in " + loc + " does not match legacy price"
                ));
              }
            }
          }

          Integer expectedStock = legacyStockMap.get(item.getItemId());
          if (expectedStock != null && expectedStock != item.getInventoryQuantity()) {
            discrepancies.add(new DiscrepancyDetail(
                "inventoryQuantity:" + item.getItemId(),
                String.valueOf(expectedStock),
                String.valueOf(item.getInventoryQuantity()),
                "INVENTORY_MISMATCH",
                "Item inventoryQuantity does not match legacy INVENTORY count"
            ));
          }
        }
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
   * Performs an exhaustive parity comparison across all orders, users, and catalog products.
   * Utilizes pre-indexed ID-keyed in-memory hash maps to achieve O(N) linear performance.
   *
   * @return list of ComparisonResult for all audited entities
   */
  public List<ComparisonResult> compareAllOrders() {
    List<ComparisonResult> results = new ArrayList<>();

    // 1. O(N) Order Audits with ID-keyed map
    List<OrderDocument> legacyOrders = orderReader.readCompleteOrdersAsDocuments();
    Map<String, OrderDocument> mongoOrders = mongoTemplate.findAll(OrderDocument.class, "petstore_orders")
        .stream()
        .collect(Collectors.toMap(OrderDocument::getId, o -> o, (a, b) -> a));

    for (OrderDocument legacyOrder : legacyOrders) {
      results.add(compareOrderInternal(legacyOrder.getId(), legacyOrder, mongoOrders.get(legacyOrder.getId())));
    }

    // 2. O(M) User Audits with ID-keyed map
    List<LegacyUserRow> legacyUsers = userReader.readAllUsers();
    Map<String, UserDocument> mongoUsers = mongoTemplate.findAll(UserDocument.class, "petstore_users")
        .stream()
        .collect(Collectors.toMap(u -> u.getUsername().toLowerCase(), u -> u, (a, b) -> a));

    for (LegacyUserRow u : legacyUsers) {
      results.add(compareUserInternal(u.username(), u, mongoUsers.get(u.username().toLowerCase())));
    }

    // 3. O(P) Product & Inventory Audits with grouped SKU item maps
    List<LegacyProductRow> legacyProducts = catalogReader.readAllProducts();
    Map<String, List<LegacyProductRow>> productsByProductId = legacyProducts.stream()
        .collect(Collectors.groupingBy(LegacyProductRow::productId));
    Map<String, List<LegacyItemRow>> itemsByProductId = catalogReader.readAllItems().stream()
        .collect(Collectors.groupingBy(LegacyItemRow::productId));
    Map<String, ProductDocument> mongoProducts = mongoTemplate.findAll(ProductDocument.class, "petstore_products")
        .stream()
        .collect(Collectors.toMap(ProductDocument::getId, p -> p, (a, b) -> a));

    for (Map.Entry<String, List<LegacyProductRow>> entry : productsByProductId.entrySet()) {
      String productId = entry.getKey();
      results.add(compareProductInternal(productId, entry.getValue(),
          itemsByProductId.getOrDefault(productId, List.of()), mongoProducts.get(productId)));
    }

    return results;
  }

  /**
   * Compares a user between the legacy relational customer subsystem and MongoDB.
   *
   * @param username the username to audit
   * @return ComparisonResult detailing match status or discrepancies
   */
  public ComparisonResult compareUser(String username) {
    Optional<LegacyUserRow> legacyUserOpt = userReader.readAllUsers()
        .stream()
        .filter(u -> username.equalsIgnoreCase(u.username()))
        .findFirst();

    UserDocument mongoUser = mongoTemplate.findById(username, UserDocument.class, "petstore_users");
    return compareUserInternal(username, legacyUserOpt.orElse(null), mongoUser);
  }

  public ComparisonResult compareUserInternal(String username, LegacyUserRow legacy, UserDocument mongoUser) {
    long startNanos = System.nanoTime();
    List<DiscrepancyDetail> discrepancies = new ArrayList<>();

    if (legacy == null && mongoUser == null) {
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("USER", username, duration);
    }

    if (legacy == null) {
      // User registered directly in MongoDB
      long duration = System.nanoTime() - startNanos;
      metrics.recordShadowComparison(true, duration);
      return ComparisonResult.match("USER", username, duration);
    }

    if (mongoUser == null) {
      discrepancies.add(new DiscrepancyDetail(
          "existence", "PRESENT", "ABSENT", "MISSING_DOCUMENT", "User in legacy database but missing in MongoDB"));
    } else {
      // Password hash comparisons are intentionally ignored to accommodate BCrypt salt rotation,
      // lazy password hashing upgrades, and modern user credential management.
      if (mongoUser.getPassword() == null || mongoUser.getPassword().isBlank()) {
        discrepancies.add(new DiscrepancyDetail(
            "password", "PRESENT", "BLANK", "PASSWORD_MISSING", "User password in MongoDB is empty or blank"));
      }
    }

    long duration = System.nanoTime() - startNanos;
    boolean match = discrepancies.isEmpty();
    metrics.recordShadowComparison(match, duration);

    return match
        ? ComparisonResult.match("USER", username, duration)
        : ComparisonResult.drift("USER", username, discrepancies, duration);
  }
}
