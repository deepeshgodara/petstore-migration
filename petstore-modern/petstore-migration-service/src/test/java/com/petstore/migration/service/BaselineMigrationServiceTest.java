package com.petstore.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.WriteModel;
import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import com.petstore.migration.processor.CatalogTransformationProcessor;
import com.petstore.migration.reader.LegacyCatalogCursorReader;
import com.petstore.migration.reader.LegacyOrderCursorReader;
import com.petstore.migration.service.BaselineMigrationService.MigrationSummary;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * Unit tests for {@link BaselineMigrationService}.
 */
class BaselineMigrationServiceTest {

  private LegacyCatalogCursorReader catalogReader;
  private LegacyOrderCursorReader orderReader;
  private CatalogTransformationProcessor transformationProcessor;
  private MongoTemplate mongoTemplate;
  private MongoCollection<Document> mockCollection;
  private BaselineMigrationService migrationService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    catalogReader = mock(LegacyCatalogCursorReader.class);
    orderReader = mock(LegacyOrderCursorReader.class);
    transformationProcessor = mock(CatalogTransformationProcessor.class);
    mongoTemplate = mock(MongoTemplate.class);
    mockCollection = mock(MongoCollection.class);

    MongoConverter mockConverter = mock(MongoConverter.class);
    when(mongoTemplate.getConverter()).thenReturn(mockConverter);
    when(mongoTemplate.getCollection(any())).thenReturn(mockCollection);

    migrationService = new BaselineMigrationService(
        catalogReader,
        orderReader,
        transformationProcessor,
        mongoTemplate
    );
  }

  @Test
  @DisplayName("Should orchestrate extraction, transformation, and bulkWrite upsert")
  @SuppressWarnings("unchecked")
  void shouldExecuteFullBaselineMigration() {
    // 1. Setup category mocks
    List<LegacyCategoryRow> catRows = List.of(new LegacyCategoryRow("FISH", "en_US", "Fish", null, null));
    List<CategoryDocument> catDocs = List.of(new CategoryDocument("FISH", Map.of("en_US", "Fish"), null, null));
    when(catalogReader.readAllCategories()).thenReturn(catRows);
    when(transformationProcessor.transformCategories(catRows)).thenReturn(catDocs);

    // 2. Setup product mocks
    List<LegacyProductRow> prodRows = List.of(new LegacyProductRow("P1", "FISH", "en_US", "Angelfish", null, null));
    List<LegacyItemRow> itemRows = List.of(new LegacyItemRow("I1", "P1", BigDecimal.TEN, BigDecimal.ONE, "en_US", null, null, null, 10));
    List<ProductDocument> prodDocs = List.of(new ProductDocument("P1", "FISH", Map.of("en_US", "Angelfish"), null, null, null));
    when(catalogReader.readAllProducts()).thenReturn(prodRows);
    when(catalogReader.readAllItems()).thenReturn(itemRows);
    when(transformationProcessor.transformProducts(prodRows, itemRows)).thenReturn(prodDocs);

    // 3. Setup order mocks
    List<OrderDocument> orderDocs = List.of(
        new OrderDocument("100115", "j2ee", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null)
    );
    when(orderReader.readCompleteOrdersAsDocuments()).thenReturn(orderDocs);

    // 4. Execute
    MigrationSummary summary = migrationService.executeBaselineMigration();

    // 5. Verify results and bulkWrite invocations
    assertThat(summary.categoriesCount()).isEqualTo(1);
    assertThat(summary.productsCount()).isEqualTo(1);
    assertThat(summary.ordersCount()).isEqualTo(1);
    assertThat(summary.executionDurationMs()).isGreaterThanOrEqualTo(0);

    verify(mongoTemplate).getCollection("petstore_categories");
    verify(mongoTemplate).getCollection("petstore_products");
    verify(mongoTemplate).getCollection("petstore_orders");
    verify(mockCollection, times(3)).bulkWrite(anyList());
  }
}
