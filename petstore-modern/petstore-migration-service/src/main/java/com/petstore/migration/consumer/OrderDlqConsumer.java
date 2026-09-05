package com.petstore.migration.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Dead-Letter Queue (DLQ) listener capturing poisoned, corrupted, or failed
 * dual-write events for observability, triage, and replay operations.
 */
@Component
public class OrderDlqConsumer {

  private static final Logger log = LoggerFactory.getLogger(OrderDlqConsumer.class);

  /**
   * Consumes records from the Dead-Letter Queue and logs critical alerts.
   *
   * @param record failed Kafka consumer record routed to DLQ
   */
  @KafkaListener(
      topics = "${migration.dualwrite.dlq-topic:petstore.orders.dlq}",
      groupId = "petstore-dlq-monitor-group"
  )
  public void onDlqMessage(ConsumerRecord<String, Object> record) {
    log.error("CRITICAL ALERT: Message isolated in Dead-Letter Queue! Topic: {}, Partition: {}, Offset: {}, Key: {}, Payload: {}",
        record.topic(),
        record.partition(),
        record.offset(),
        record.key(),
        record.value());
  }
}
