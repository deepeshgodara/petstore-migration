package com.petstore.migration.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrderDlqConsumer}.
 */
class OrderDlqConsumerTest {

  @Test
  @DisplayName("Should consume and process message from DLQ")
  void shouldProcessDlqMessage() {
    OrderDlqConsumer dlqConsumer = new OrderDlqConsumer();
    ConsumerRecord<String, Object> record = new ConsumerRecord<>(
        "petstore.orders.dlq", 0, 10L, "ORD-CORRUPT", "{bad-payload}");

    // Verify it processes without throwing exception
    dlqConsumer.onDlqMessage(record);
  }
}
