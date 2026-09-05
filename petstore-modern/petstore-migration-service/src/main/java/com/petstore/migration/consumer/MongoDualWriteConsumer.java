package com.petstore.migration.consumer;

import com.mongodb.client.result.UpdateResult;
import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.common.metrics.MigrationParityMetrics;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer replicating asynchronous dual-write events into MongoDB with
 * automatic Dead-Letter Queue (DLQ) error isolation.
 */
@Component
public class MongoDualWriteConsumer {

  private static final Logger log = LoggerFactory.getLogger(MongoDualWriteConsumer.class);
  private static final String ORDERS_COLLECTION = "petstore_orders";

  private final MongoTemplate mongoTemplate;
  private final MigrationParityMetrics metrics;

  public MongoDualWriteConsumer(MongoTemplate mongoTemplate, MigrationParityMetrics metrics) {
    this.mongoTemplate = mongoTemplate;
    this.metrics = metrics;
  }

  /**
   * Processes dual-write events from Kafka and replicates state into MongoDB.
   * Unhandled exceptions trigger the DefaultErrorHandler which routes the message to the DLQ.
   *
   * @param event the dual-write event received from Kafka
   */
  @KafkaListener(
      topics = "${migration.dualwrite.topic:petstore.orders.dualwrite}",
      groupId = "${spring.kafka.consumer.group-id:petstore-migration-group}",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void onDualWriteEvent(OrderDualWriteEvent event) {
    if (event == null || event.getOrderId() == null) {
      metrics.recordDualWriteFailure();
      throw new IllegalArgumentException("Invalid OrderDualWriteEvent: payload or orderId is null");
    }

    log.debug("Received dual-write event [{}] for order [{}] (type: {})",
        event.getEventId(), event.getOrderId(), event.getEventType());

    try {
      if (event.getEventType() == OrderEventType.ORDER_CREATED) {
        handleOrderCreated(event);
      } else if (event.getEventType() == OrderEventType.ORDER_STATUS_UPDATED) {
        handleOrderStatusUpdated(event);
      } else if (event.getEventType() == OrderEventType.ORDER_CANCELLED) {
        handleOrderCancelled(event);
      } else {
        log.warn("Unknown event type [{}] in dual-write event [{}]", event.getEventType(), event.getEventId());
      }

      metrics.recordDualWriteSuccess();
    } catch (Exception e) {
      metrics.recordDualWriteFailure();
      log.error("Failed processing dual-write event [{}] for order [{}]: {}",
          event.getEventId(), event.getOrderId(), e.getMessage(), e);
      throw e;
    }
  }

  private void handleOrderCreated(OrderDualWriteEvent event) {
    OrderDocument order = event.getOrder();
    if (order == null) {
      throw new IllegalArgumentException("ORDER_CREATED event missing OrderDocument payload");
    }
    mongoTemplate.save(order, ORDERS_COLLECTION);
    log.info("Dual-write synced new order [{}] to MongoDB collection [{}]", order.getId(), ORDERS_COLLECTION);
  }

  private void handleOrderStatusUpdated(OrderDualWriteEvent event) {
    Query query = Query.query(Criteria.where("_id").is(event.getOrderId()));
    Update update = new Update()
        .set("status", event.getNewStatus())
        .set("updatedAt", Instant.now());

    UpdateResult result = mongoTemplate.updateFirst(query, update, ORDERS_COLLECTION);
    if (result.getMatchedCount() == 0 && event.getOrder() != null) {
      // Document not yet in secondary store: save entire aggregate
      mongoTemplate.save(event.getOrder(), ORDERS_COLLECTION);
      log.info("Dual-write inserted missing order [{}] during status update to [{}]",
          event.getOrderId(), event.getNewStatus());
    } else {
      log.info("Dual-write updated order [{}] status to [{}] (matched: {})",
          event.getOrderId(), event.getNewStatus(), result.getMatchedCount());
    }
  }

  private void handleOrderCancelled(OrderDualWriteEvent event) {
    Query query = Query.query(Criteria.where("_id").is(event.getOrderId()));
    Update update = new Update()
        .set("status", OrderStatus.CANCELLED)
        .set("updatedAt", Instant.now());
    mongoTemplate.updateFirst(query, update, ORDERS_COLLECTION);
    log.info("Dual-write cancelled order [{}] in MongoDB", event.getOrderId());
  }
}
