package com.petstore.migration.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Unit tests for {@link MongoDualWriteConsumer}.
 */
class MongoDualWriteConsumerTest {

  private MongoTemplate mongoTemplate;
  private MigrationParityMetrics metrics;
  private MongoDualWriteConsumer consumer;

  @BeforeEach
  void setUp() {
    mongoTemplate = mock(MongoTemplate.class);
    metrics = new MigrationParityMetrics(new SimpleMeterRegistry());
    consumer = new MongoDualWriteConsumer(mongoTemplate, metrics);
  }

  @Test
  @DisplayName("Should sync ORDER_CREATED event to MongoDB and record success metric")
  void shouldSyncOrderCreated() {
    OrderDocument order = new OrderDocument(
        "ORD-101", "user1", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null);
    OrderDualWriteEvent event = OrderDualWriteEvent.ofCreated(order, "TEST");

    consumer.onDualWriteEvent(event);

    verify(mongoTemplate).save(order, "petstore_orders");
  }

  @Test
  @DisplayName("Should update status in MongoDB for ORDER_STATUS_UPDATED event")
  void shouldSyncOrderStatusUpdated() {
    OrderDocument order = new OrderDocument(
        "ORD-102", "user1", null, OrderStatus.APPROVED, BigDecimal.TEN, "en_US", null, null, null, null);
    OrderDualWriteEvent event = OrderDualWriteEvent.ofStatusUpdated(
        order, OrderStatus.PENDING, OrderStatus.APPROVED, "TEST");

    UpdateResult updateResult = mock(UpdateResult.class);
    when(updateResult.getMatchedCount()).thenReturn(1L);
    when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("petstore_orders")))
        .thenReturn(updateResult);

    consumer.onDualWriteEvent(event);

    verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("petstore_orders"));
  }

  @Test
  @DisplayName("Should update status to CANCELLED for ORDER_CANCELLED event")
  void shouldSyncOrderCancelled() {
    OrderDocument order = new OrderDocument(
        "ORD-103", "user1", null, OrderStatus.CANCELLED, BigDecimal.TEN, "en_US", null, null, null, null);
    OrderDualWriteEvent event = new OrderDualWriteEvent(
        "evt-103", "ORD-103", OrderEventType.ORDER_CANCELLED, order, OrderStatus.PENDING, OrderStatus.CANCELLED, null, "TEST");

    consumer.onDualWriteEvent(event);

    verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("petstore_orders"));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException and record failure metric when event has null orderId")
  void shouldThrowAndRecordFailureOnNullOrderId() {
    OrderDualWriteEvent invalidEvent = new OrderDualWriteEvent(
        "evt-null", null, OrderEventType.ORDER_CREATED, null, null, null, null, "TEST");

    assertThatThrownBy(() -> consumer.onDualWriteEvent(invalidEvent))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should rethrow exception to trigger DLQ recoverer when Mongo save fails")
  void shouldRethrowExceptionWhenMongoFails() {
    OrderDocument order = new OrderDocument(
        "ORD-ERR", "user1", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null);
    OrderDualWriteEvent event = OrderDualWriteEvent.ofCreated(order, "TEST");

    when(mongoTemplate.save(any(), eq("petstore_orders")))
        .thenThrow(new RuntimeException("MongoDB replica set connection timeout"));

    assertThatThrownBy(() -> consumer.onDualWriteEvent(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("MongoDB replica set connection timeout");
  }
}
