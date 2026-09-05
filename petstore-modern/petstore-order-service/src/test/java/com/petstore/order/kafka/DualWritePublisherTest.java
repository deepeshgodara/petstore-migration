package com.petstore.order.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link DualWritePublisher}.
 */
class DualWritePublisherTest {

  private KafkaTemplate<String, OrderDualWriteEvent> kafkaTemplate;
  private MigrationParityMetrics metrics;
  private DualWritePublisher publisher;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    metrics = new MigrationParityMetrics(new SimpleMeterRegistry());
    publisher = new DualWritePublisher(kafkaTemplate, metrics);
    ReflectionTestUtils.setField(publisher, "dualWriteTopic", "petstore.orders.dualwrite");
    ReflectionTestUtils.setField(publisher, "dualWriteEnabled", true);
    ReflectionTestUtils.setField(publisher, "applicationName", "petstore-order-service");
  }

  @Test
  @DisplayName("Should publish ORDER_CREATED event and record success metric")
  @SuppressWarnings("unchecked")
  void shouldPublishOrderCreatedSuccessfully() {
    OrderDocument order = new OrderDocument(
        "ORD-001", "shopper", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null);

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition("petstore.orders.dualwrite", 0), 0, 1, System.currentTimeMillis(), 0, 0);
    SendResult<String, OrderDualWriteEvent> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> future = CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq("petstore.orders.dualwrite"), eq("ORD-001"), any(OrderDualWriteEvent.class)))
        .thenReturn(future);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> resultFuture = publisher.publishOrderCreated(order);
    assertThat(resultFuture).isNotNull();

    ArgumentCaptor<OrderDualWriteEvent> captor = ArgumentCaptor.forClass(OrderDualWriteEvent.class);
    verify(kafkaTemplate).send(eq("petstore.orders.dualwrite"), eq("ORD-001"), captor.capture());

    OrderDualWriteEvent captured = captor.getValue();
    assertThat(captured.getOrderId()).isEqualTo("ORD-001");
    assertThat(captured.getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
    assertThat(captured.getNewStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(captured.getSource()).isEqualTo("petstore-order-service");
  }

  @Test
  @DisplayName("Should publish ORDER_STATUS_UPDATED event with previous and new status")
  @SuppressWarnings("unchecked")
  void shouldPublishOrderStatusUpdatedSuccessfully() {
    OrderDocument order = new OrderDocument(
        "ORD-002", "shopper", null, OrderStatus.APPROVED, BigDecimal.valueOf(50), "en_US", null, null, null, null);

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition("petstore.orders.dualwrite", 1), 0, 2, System.currentTimeMillis(), 0, 0);
    SendResult<String, OrderDualWriteEvent> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> future = CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq("petstore.orders.dualwrite"), eq("ORD-002"), any(OrderDualWriteEvent.class)))
        .thenReturn(future);

    publisher.publishOrderStatusUpdated(order, OrderStatus.PENDING, OrderStatus.APPROVED);

    ArgumentCaptor<OrderDualWriteEvent> captor = ArgumentCaptor.forClass(OrderDualWriteEvent.class);
    verify(kafkaTemplate).send(eq("petstore.orders.dualwrite"), eq("ORD-002"), captor.capture());

    OrderDualWriteEvent captured = captor.getValue();
    assertThat(captured.getOrderId()).isEqualTo("ORD-002");
    assertThat(captured.getEventType()).isEqualTo(OrderEventType.ORDER_STATUS_UPDATED);
    assertThat(captured.getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(captured.getNewStatus()).isEqualTo(OrderStatus.APPROVED);
  }

  @Test
  @DisplayName("Should record failure metric when Kafka dispatch encounters an exception")
  @SuppressWarnings("unchecked")
  void shouldRecordFailureMetricOnException() {
    OrderDocument order = new OrderDocument(
        "ORD-003", "shopper", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

    when(kafkaTemplate.send(eq("petstore.orders.dualwrite"), eq("ORD-003"), any(OrderDualWriteEvent.class)))
        .thenReturn(failedFuture);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> resultFuture = publisher.publishOrderCreated(order);
    assertThat(resultFuture).isCompletedExceptionally();
  }

  @Test
  @DisplayName("Should skip dispatch when dual-write is disabled in configuration")
  void shouldSkipDispatchWhenDisabled() {
    ReflectionTestUtils.setField(publisher, "dualWriteEnabled", false);

    OrderDocument order = new OrderDocument(
        "ORD-004", "shopper", null, OrderStatus.PENDING, BigDecimal.TEN, "en_US", null, null, null, null);

    publisher.publishOrderCreated(order);

    verify(kafkaTemplate, never()).send(any(), any(), any());
  }

  @Test
  @DisplayName("Should handle null order gracefully without throwing exception")
  void shouldHandleNullOrderGracefully() {
    CompletableFuture<SendResult<String, OrderDualWriteEvent>> future = publisher.publishOrderCreated(null);
    assertThat(future).isNotNull();
    verify(kafkaTemplate, never()).send(any(), any(), any());
  }
}
