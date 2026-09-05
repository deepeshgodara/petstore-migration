package com.petstore.order.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petstore.common.event.OrderDomainEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Unit tests for {@link OrderEventProducer}.
 */
class OrderEventProducerTest {

  private KafkaTemplate<String, Object> kafkaTemplate;
  private OrderEventProducer producer;

  private static final String TOPIC_CREATED = "petstore.orders.created";
  private static final String TOPIC_APPROVED = "petstore.orders.approved";
  private static final String TOPIC_COMPLETED = "petstore.orders.completed";

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    producer = new OrderEventProducer(
        kafkaTemplate, TOPIC_CREATED, TOPIC_APPROVED, TOPIC_COMPLETED);
  }

  @Test
  @DisplayName("Should publish OrderDomainEvent to created topic on order creation")
  @SuppressWarnings("unchecked")
  void shouldPublishOrderCreatedSuccessfully() {
    OrderDocument order = new OrderDocument(
        "ORD-101", "shopper1", null, OrderStatus.PENDING,
        BigDecimal.valueOf(45.50), "en_US", null, null, null, Collections.emptyList());

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition(TOPIC_CREATED, 0), 0, 1, System.currentTimeMillis(), 0, 0);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    CompletableFuture<SendResult<String, Object>> future =
        CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq(TOPIC_CREATED), eq("ORD-101"), any(OrderDomainEvent.class)))
        .thenReturn(future);

    CompletableFuture<SendResult<String, Object>> result = producer.publishOrderCreated(order);
    assertThat(result).isNotNull();

    ArgumentCaptor<OrderDomainEvent> captor = ArgumentCaptor.forClass(OrderDomainEvent.class);
    verify(kafkaTemplate).send(eq(TOPIC_CREATED), eq("ORD-101"), captor.capture());

    OrderDomainEvent captured = captor.getValue();
    assertThat(captured.getOrderId()).isEqualTo("ORD-101");
    assertThat(captured.getUserId()).isEqualTo("shopper1");
    assertThat(captured.getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
    assertThat(captured.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(captured.getTotalPrice()).isEqualTo(BigDecimal.valueOf(45.50));
  }

  @Test
  @DisplayName("Should publish OrderDomainEvent to approved topic on order approval")
  @SuppressWarnings("unchecked")
  void shouldPublishOrderApprovedSuccessfully() {
    OrderDocument order = new OrderDocument(
        "ORD-102", "shopper2", null, OrderStatus.APPROVED,
        BigDecimal.valueOf(99.00), "en_US", null, null, null, Collections.emptyList());

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition(TOPIC_APPROVED, 1), 0, 5, System.currentTimeMillis(), 0, 0);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    CompletableFuture<SendResult<String, Object>> future =
        CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq(TOPIC_APPROVED), eq("ORD-102"), any(OrderDomainEvent.class)))
        .thenReturn(future);

    CompletableFuture<SendResult<String, Object>> result =
        producer.publishOrderStatusUpdated(order, OrderStatus.PENDING, OrderStatus.APPROVED);
    assertThat(result).isNotNull();

    ArgumentCaptor<OrderDomainEvent> captor = ArgumentCaptor.forClass(OrderDomainEvent.class);
    verify(kafkaTemplate).send(eq(TOPIC_APPROVED), eq("ORD-102"), captor.capture());

    OrderDomainEvent captured = captor.getValue();
    assertThat(captured.getOrderId()).isEqualTo("ORD-102");
    assertThat(captured.getStatus()).isEqualTo(OrderStatus.APPROVED);
    assertThat(captured.getEventType()).isEqualTo(OrderEventType.ORDER_STATUS_UPDATED);
  }

  @Test
  @DisplayName("Should publish OrderDomainEvent to completed topic on order completion")
  @SuppressWarnings("unchecked")
  void shouldPublishOrderCompletedSuccessfully() {
    OrderDocument order = new OrderDocument(
        "ORD-103", "shopper3", null, OrderStatus.COMPLETED,
        BigDecimal.valueOf(150.00), "en_US", null, null, null, Collections.emptyList());

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition(TOPIC_COMPLETED, 2), 0, 10, System.currentTimeMillis(), 0, 0);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    CompletableFuture<SendResult<String, Object>> future =
        CompletableFuture.completedFuture(sendResult);
    when(kafkaTemplate.send(eq(TOPIC_COMPLETED), eq("ORD-103"), any(OrderDomainEvent.class)))
        .thenReturn(future);

    CompletableFuture<SendResult<String, Object>> result =
        producer.publishOrderStatusUpdated(order, OrderStatus.APPROVED, OrderStatus.COMPLETED);
    assertThat(result).isNotNull();

    ArgumentCaptor<OrderDomainEvent> captor = ArgumentCaptor.forClass(OrderDomainEvent.class);
    verify(kafkaTemplate).send(eq(TOPIC_COMPLETED), eq("ORD-103"), captor.capture());

    OrderDomainEvent captured = captor.getValue();
    assertThat(captured.getOrderId()).isEqualTo("ORD-103");
    assertThat(captured.getStatus()).isEqualTo(OrderStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should skip publishing when status has no dedicated domain topic")
  void shouldSkipWhenStatusHasNoDedicatedTopic() {
    OrderDocument order = new OrderDocument(
        "ORD-104", "shopper4", null, OrderStatus.CANCELLED,
        BigDecimal.valueOf(20.00), "en_US", null, null, null, Collections.emptyList());

    CompletableFuture<SendResult<String, Object>> result =
        producer.publishOrderStatusUpdated(order, OrderStatus.PENDING, OrderStatus.CANCELLED);
    assertThat(result).isCompletedWithValue(null);
    verifyNoInteractions(kafkaTemplate);
  }

  @Test
  @DisplayName("Should handle null order safely without throwing exception")
  void shouldHandleNullOrderSafely() {
    CompletableFuture<SendResult<String, Object>> createdResult =
        producer.publishOrderCreated(null);
    assertThat(createdResult).isCompletedWithValue(null);

    CompletableFuture<SendResult<String, Object>> updatedResult =
        producer.publishOrderStatusUpdated(null, OrderStatus.PENDING, OrderStatus.APPROVED);
    assertThat(updatedResult).isCompletedWithValue(null);

    verifyNoInteractions(kafkaTemplate);
  }
}
