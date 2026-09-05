package com.petstore.order.kafka;

import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Asynchronous Kafka publisher dispatching dual-write events for order operations
 * (creation, status updates, cancellations) to decouple the primary write path
 * from secondary store synchronization.
 */
@Component
public class DualWritePublisher {

  private static final Logger log = LoggerFactory.getLogger(DualWritePublisher.class);

  private final KafkaTemplate<String, OrderDualWriteEvent> kafkaTemplate;
  private final MigrationParityMetrics metrics;

  @Value("${app.kafka.topics.order-dualwrite:petstore.orders.dualwrite}")
  private String dualWriteTopic;

  @Value("${migration.dualwrite.enabled:true}")
  private boolean dualWriteEnabled;

  @Value("${spring.application.name:petstore-order-service}")
  private String applicationName;

  /**
   * Constructs the publisher with KafkaTemplate and MigrationParityMetrics.
   *
   * @param kafkaTemplate Kafka template configured for OrderDualWriteEvent serialization
   * @param metrics Micrometer metrics tracker for migration parity
   */
  public DualWritePublisher(
      KafkaTemplate<String, OrderDualWriteEvent> kafkaTemplate,
      MigrationParityMetrics metrics) {
    this.kafkaTemplate = kafkaTemplate;
    this.metrics = metrics;
  }

  /**
   * Dispatches an asynchronous dual-write event when a new order is created.
   *
   * @param order the created order aggregate
   * @return CompletableFuture resolving to the SendResult or null if skipped/disabled
   */
  public CompletableFuture<SendResult<String, OrderDualWriteEvent>> publishOrderCreated(
      OrderDocument order) {
    if (order == null) {
      log.warn("Cannot publish dual-write event for null order");
      return CompletableFuture.completedFuture(null);
    }
    OrderDualWriteEvent event = OrderDualWriteEvent.ofCreated(order, applicationName);
    return publishEvent(event);
  }

  /**
   * Dispatches an asynchronous dual-write event when an order status is updated.
   *
   * @param order the updated order aggregate
   * @param previousStatus previous status prior to transition
   * @param newStatus updated status
   * @return CompletableFuture resolving to the SendResult or null if skipped/disabled
   */
  public CompletableFuture<SendResult<String, OrderDualWriteEvent>> publishOrderStatusUpdated(
      OrderDocument order,
      OrderStatus previousStatus,
      OrderStatus newStatus) {
    if (order == null) {
      log.warn("Cannot publish status update event for null order");
      return CompletableFuture.completedFuture(null);
    }
    OrderDualWriteEvent event =
        OrderDualWriteEvent.ofStatusUpdated(order, previousStatus, newStatus, applicationName);
    return publishEvent(event);
  }

  /**
   * Asynchronously publishes an OrderDualWriteEvent to Kafka keyed by orderId.
   * Non-blocking callback records metrics upon completion or failure.
   *
   * @param event the event to dispatch
   * @return CompletableFuture for Kafka send
   */
  public CompletableFuture<SendResult<String, OrderDualWriteEvent>> publishEvent(
      OrderDualWriteEvent event) {
    if (!dualWriteEnabled) {
      String eventId = event != null ? event.getEventId() : "null";
      log.debug("Dual-write is disabled via configuration. Skipping event [{}]", eventId);
      return CompletableFuture.completedFuture(null);
    }

    if (event == null || event.getOrderId() == null) {
      log.warn("Attempted to publish invalid or null dual-write event");
      return CompletableFuture.completedFuture(null);
    }

    log.debug("Dispatching dual-write event [{}] for order [{}] (type: {}) to topic [{}]",
        event.getEventId(), event.getOrderId(), event.getEventType(), dualWriteTopic);

    CompletableFuture<SendResult<String, OrderDualWriteEvent>> future =
        kafkaTemplate.send(dualWriteTopic, event.getOrderId(), event);

    future.whenComplete((result, ex) -> {
      if (ex == null) {
        log.info("Successfully published dual-write event [{}] for order [{}] to partition [{}] with offset [{}]",
            event.getEventId(),
            event.getOrderId(),
            result != null && result.getRecordMetadata() != null ? result.getRecordMetadata().partition() : -1,
            result != null && result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : -1);
        metrics.recordDualWriteSuccess();
      } else {
        log.error("Failed to publish dual-write event [{}] for order [{}] to topic [{}]: {}",
            event.getEventId(),
            event.getOrderId(),
            dualWriteTopic,
            ex.getMessage(),
            ex);
        metrics.recordDualWriteFailure();
      }
    });

    return future;
  }

  public String getDualWriteTopic() {
    return dualWriteTopic;
  }

  public boolean isDualWriteEnabled() {
    return dualWriteEnabled;
  }
}
