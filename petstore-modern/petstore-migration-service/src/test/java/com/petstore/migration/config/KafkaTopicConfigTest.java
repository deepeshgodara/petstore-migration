package com.petstore.migration.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link KafkaTopicConfig}.
 */
class KafkaTopicConfigTest {

  @Test
  @DisplayName("Should provision dual-write topic with 3 partitions and replication factor 1")
  void shouldProvisionDualWriteTopic() {
    KafkaTopicConfig config = new KafkaTopicConfig();
    ReflectionTestUtils.setField(config, "dualWriteTopic", "petstore.orders.dualwrite");
    ReflectionTestUtils.setField(config, "dlqTopic", "petstore.orders.dlq");

    NewTopic topic = config.orderDualWriteTopic();
    assertThat(topic.name()).isEqualTo("petstore.orders.dualwrite");
    assertThat(topic.numPartitions()).isEqualTo(3);
    assertThat(topic.replicationFactor()).isEqualTo((short) 1);
  }

  @Test
  @DisplayName("Should provision DLQ topic with 3 partitions and replication factor 1")
  void shouldProvisionDlqTopic() {
    KafkaTopicConfig config = new KafkaTopicConfig();
    ReflectionTestUtils.setField(config, "dualWriteTopic", "petstore.orders.dualwrite");
    ReflectionTestUtils.setField(config, "dlqTopic", "petstore.orders.dlq");

    NewTopic topic = config.orderDlqTopic();
    assertThat(topic.name()).isEqualTo("petstore.orders.dlq");
    assertThat(topic.numPartitions()).isEqualTo(3);
    assertThat(topic.replicationFactor()).isEqualTo((short) 1);
  }
}
