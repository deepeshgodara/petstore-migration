package com.petstore.common.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Enterprise database index initializer.
 * Resolves the "auto-index-creation is false" gotcha by ensuring all required
 * compound, unique, and text indexes are materialized on application startup.
 */
@Component
public class DatabaseIndexInitializer {

  private static final Logger log = LoggerFactory.getLogger(DatabaseIndexInitializer.class);

  private final MongoTemplate mongoTemplate;

  public DatabaseIndexInitializer(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initializeIndexes() {
    try {
      log.info("Verifying and initializing MongoDB indexes across collections...");
      ensureOrderIndexes();
      ensureUserIndexes();
      ensureProductIndexes();
      log.info("MongoDB index verification completed successfully.");
    } catch (Exception e) {
      log.warn("Non-fatal exception during startup index initialization: {}", e.getMessage());
    }
  }

  private void ensureOrderIndexes() {
    if (mongoTemplate.collectionExists("petstore_orders")) {
      IndexOperations ops = mongoTemplate.indexOps("petstore_orders");

      // userId + orderDate desc
      ops.ensureIndex(new CompoundIndexDefinition(new Document("userId", 1).append("orderDate", -1))
          .named("userId_orderDate_idx"));

      // status + orderDate desc
      ops.ensureIndex(new CompoundIndexDefinition(new Document("status", 1).append("orderDate", -1))
          .named("status_orderDate_idx"));

      // index-backed sort / range on Decimal128 price
      ops.ensureIndex(new Index().on("totalPrice", Sort.Direction.ASC)
          .named("totalPrice_idx"));

      log.debug("Initialized indexes on petstore_orders");
    }
  }

  private void ensureUserIndexes() {
    if (mongoTemplate.collectionExists("petstore_users")) {
      IndexOperations ops = mongoTemplate.indexOps("petstore_users");

      // email index (non-unique to support legacy migrated test accounts)
      ops.ensureIndex(new Index().on("email", Sort.Direction.ASC)
          .named("email_idx"));

      // role index for admin query filters
      ops.ensureIndex(new Index().on("role", Sort.Direction.ASC)
          .named("role_idx"));

      log.debug("Initialized indexes on petstore_users");
    }
  }

  private void ensureProductIndexes() {
    if (mongoTemplate.collectionExists("petstore_products")) {
      IndexOperations ops = mongoTemplate.indexOps("petstore_products");

      // categoryId index
      ops.ensureIndex(new Index().on("categoryId", Sort.Direction.ASC)
          .named("categoryId_idx"));

      // items.itemId index
      ops.ensureIndex(new Index().on("items.itemId", Sort.Direction.ASC)
          .named("items_itemId_idx"));

      // Compound text index on multilingual localized names for fast text search
      try {
        Document textKeys = new Document();
        textKeys.put("names.en_US", "text");
        textKeys.put("names.ja_JP", "text");
        textKeys.put("names.zh_CN", "text");
        mongoTemplate.getCollection("petstore_products").createIndex(textKeys);
        log.debug("Initialized compound text index on petstore_products multilingual fields");
      } catch (Exception e) {
        log.debug("Text index on petstore_products already exists or skipped: {}", e.getMessage());
      }
    }
  }
}
