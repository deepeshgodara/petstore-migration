package com.petstore.migration.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.processor.CatalogTransformationProcessor;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.order.document.OrderDocument;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Migration Service executing idempotent batch extraction from the legacy relational
 * database and bulkWrite upserts into MongoDB collections (petstore_categories,
 * petstore_products, and petstore_orders).
 */
@Service
public class BaselineMigrationService {

  private static final Logger log = LoggerFactory.getLogger(BaselineMigrationService.class);

  private final LegacyCatalogCursorReader catalogReader;
  private final LegacyOrderCursorReader orderReader;
  private final CatalogTransformationProcessor transformationProcessor;
  private final MongoTemplate mongoTemplate;

  public BaselineMigrationService(
      LegacyCatalogCursorReader catalogReader,
      LegacyOrderCursorReader orderReader,
      CatalogTransformationProcessor transformationProcessor,
      MongoTemplate mongoTemplate) {
    this.catalogReader = catalogReader;
    this.orderReader = orderReader;
    this.transformationProcessor = transformationProcessor;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Executes the full baseline migration pipeline: extracting live legacy data,
   * transforming relational rows into document aggregates, and executing bulkWrite upserts.
   *
   * @return MigrationSummary detailing record counts and elapsed execution time
   */
  public MigrationSummary executeBaselineMigration() {
    Instant startTime = Instant.now();
    log.info("Starting live historical baseline extraction from legacy database...");

    // 1. Extract & Transform Categories
    List<LegacyCategoryRow> categoryRows = catalogReader.readAllCategories();
    List<CategoryDocument> categories = transformationProcessor.transformCategories(categoryRows);
    int categoriesUpserted = bulkUpsertCategories(categories);

    // 2. Extract & Transform Products and Items
    List<LegacyProductRow> productRows = catalogReader.readAllProducts();
    List<LegacyItemRow> itemRows = catalogReader.readAllItems();
    List<ProductDocument> products = transformationProcessor.transformProducts(productRows, itemRows);
    int productsUpserted = bulkUpsertProducts(products);

    // 3. Extract & Transform Orders
    List<OrderDocument> orders = orderReader.readCompleteOrdersAsDocuments();
    int ordersUpserted = bulkUpsertOrders(orders);

    Duration elapsed = Duration.between(startTime, Instant.now());
    MigrationSummary summary = new MigrationSummary(
        categoriesUpserted,
        productsUpserted,
        ordersUpserted,
        elapsed.toMillis()
    );

    log.info("Baseline migration completed successfully in {} ms: {}", elapsed.toMillis(), summary);
    return summary;
  }

  /**
   * Idempotent bulk upsert of CategoryDocument list into petstore_categories.
   */
  public int bulkUpsertCategories(List<CategoryDocument> categories) {
    if (categories == null || categories.isEmpty()) {
      return 0;
    }

    List<WriteModel<Document>> operations = new ArrayList<>();
    ReplaceOptions options = new ReplaceOptions().upsert(true);

    for (CategoryDocument category : categories) {
      Document doc = new Document();
      mongoTemplate.getConverter().write(category, doc);
      operations.add(new ReplaceOneModel<>(Filters.eq("_id", category.getId()), doc, options));
    }

    mongoTemplate.getCollection("petstore_categories").bulkWrite(operations);
    return categories.size();
  }

  /**
   * Idempotent bulk upsert of ProductDocument list into petstore_products.
   */
  public int bulkUpsertProducts(List<ProductDocument> products) {
    if (products == null || products.isEmpty()) {
      return 0;
    }

    List<WriteModel<Document>> operations = new ArrayList<>();
    ReplaceOptions options = new ReplaceOptions().upsert(true);

    for (ProductDocument product : products) {
      Document doc = new Document();
      mongoTemplate.getConverter().write(product, doc);
      operations.add(new ReplaceOneModel<>(Filters.eq("_id", product.getId()), doc, options));
    }

    mongoTemplate.getCollection("petstore_products").bulkWrite(operations);
    return products.size();
  }

  /**
   * Idempotent bulk upsert of OrderDocument list into petstore_orders.
   */
  public int bulkUpsertOrders(List<OrderDocument> orders) {
    if (orders == null || orders.isEmpty()) {
      return 0;
    }

    List<WriteModel<Document>> operations = new ArrayList<>();
    ReplaceOptions options = new ReplaceOptions().upsert(true);

    for (OrderDocument order : orders) {
      Document doc = new Document();
      mongoTemplate.getConverter().write(order, doc);
      operations.add(new ReplaceOneModel<>(Filters.eq("_id", order.getId()), doc, options));
    }

    mongoTemplate.getCollection("petstore_orders").bulkWrite(operations);
    return orders.size();
  }

  /**
   * Summary record containing record counts and execution duration.
   */
  public record MigrationSummary(
      int categoriesCount,
      int productsCount,
      int ordersCount,
      long executionDurationMs
  ) {}
}
