package com.petstore.order.kafka;

import com.petstore.common.event.OrderDomainEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Producer publishing business domain events for order lifecycle transitions to Kafka topics.
 */
@Component
public class OrderEventProducer {

  private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String orderCreatedTopic;
  private final String orderApprovedTopic;
  private final String orderCompletedTopic;

  public OrderEventProducer(
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("${app.kafka.topics.order-created:petstore.orders.created}")
          String orderCreatedTopic,
      @Value("${app.kafka.topics.order-approved:petstore.orders.approved}")
          String orderApprovedTopic,
      @Value("${app.kafka.topics.order-completed:petstore.orders.completed}")
          String orderCompletedTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.orderCreatedTopic = orderCreatedTopic;
    this.orderApprovedTopic = orderApprovedTopic;
    this.orderCompletedTopic = orderCompletedTopic;
  }

  /**
   * Publishes an order created domain event.
   *
   * @param order the created order document
   * @return CompletableFuture representing the asynchronous send result
   */
  public CompletableFuture<SendResult<String, Object>> publishOrderCreated(OrderDocument order) {
    if (order == null) {
      return CompletableFuture.completedFuture(null);
    }
    int totalItems = order.getLineItems() != null ? order.getLineItems().size() : 0;
    OrderDomainEvent event = new OrderDomainEvent(
        UUID.randomUUID().toString(),
        order.getId(),
        order.getUserId(),
        OrderEventType.ORDER_CREATED,
        order.getStatus(),
        order.getTotalPrice(),
        totalItems,
        Instant.now()
    );
    return sendEvent(orderCreatedTopic, order.getId(), event);
  }

  /**
   * Publishes an order status transition domain event to the appropriate topic.
   *
   * @param order the order with updated status
   * @param previousStatus status before update
   * @param newStatus status after update
   * @return CompletableFuture representing the asynchronous send result
   */
  public CompletableFuture<SendResult<String, Object>> publishOrderStatusUpdated(
      OrderDocument order, OrderStatus previousStatus, OrderStatus newStatus) {
    if (order == null) {
      return CompletableFuture.completedFuture(null);
    }
    int totalItems = order.getLineItems() != null ? order.getLineItems().size() : 0;
    OrderDomainEvent event = new OrderDomainEvent(
        UUID.randomUUID().toString(),
        order.getId(),
        order.getUserId(),
        OrderEventType.ORDER_STATUS_UPDATED,
        newStatus,
        order.getTotalPrice(),
        totalItems,
        Instant.now()
    );

    String topic = resolveTopicForStatus(newStatus);
    if (topic != null) {
      return sendEvent(topic, order.getId(), event);
    } else {
      log.debug(
          "No dedicated domain topic configured for status [{}], skipping event publication",
          newStatus);
      return CompletableFuture.completedFuture(null);
    }
  }

  private String resolveTopicForStatus(OrderStatus status) {
    if (status == OrderStatus.APPROVED) {
      return orderApprovedTopic;
    } else if (status == OrderStatus.COMPLETED) {
      return orderCompletedTopic;
    }
    return null;
  }

  private CompletableFuture<SendResult<String, Object>> sendEvent(
      String topic, String key, OrderDomainEvent event) {
    log.info("Publishing domain event [{}] for order [{}] to topic [{}]",
        event.getEventId(), key, topic);
    return kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish domain event [{}] for order [{}] to topic [{}]",
                event.getEventId(), key, topic, ex);
          } else {
            log.info("Successfully published domain event [{}] to partition [{}] at offset [{}]",
                event.getEventId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
