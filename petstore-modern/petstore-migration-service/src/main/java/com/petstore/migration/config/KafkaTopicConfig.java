package com.petstore.migration.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic provisioning configuration ensuring that the dual-write stream
 * and Dead-Letter Queue (DLQ) topics exist with appropriate partitions and replicas.
 */
@Configuration
public class KafkaTopicConfig {

  @Value("${migration.dualwrite.topic:petstore.orders.dualwrite}")
  private String dualWriteTopic;

  @Value("${migration.dualwrite.dlq-topic:petstore.orders.dlq}")
  private String dlqTopic;

  /**
   * Defines the primary dual-write Kafka topic for order stream replication.
   *
   * @return NewTopic configured with 3 partitions and replication factor 1
   */
  @Bean
  public NewTopic orderDualWriteTopic() {
    return TopicBuilder.name(dualWriteTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Defines the Dead-Letter Queue (DLQ) topic for isolated poisoned or unparseable messages.
   *
   * @return NewTopic configured with 3 partitions and replication factor 1
   */
  @Bean
  public NewTopic orderDlqTopic() {
    return TopicBuilder.name(dlqTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }
}
