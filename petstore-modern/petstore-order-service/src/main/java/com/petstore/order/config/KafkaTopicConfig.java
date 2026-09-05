package com.petstore.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic provisioning configuration for order lifecycle domain event topics.
 */
@Configuration
public class KafkaTopicConfig {

  @Value("${app.kafka.topics.order-created:petstore.orders.created}")
  private String orderCreatedTopic;

  @Value("${app.kafka.topics.order-approved:petstore.orders.approved}")
  private String orderApprovedTopic;

  @Value("${app.kafka.topics.order-completed:petstore.orders.completed}")
  private String orderCompletedTopic;

  /**
   * Topic for order created domain events.
   *
   * @return NewTopic configured with 3 partitions and replication factor 1
   */
  @Bean
  public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(orderCreatedTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Topic for order approved domain events.
   *
   * @return NewTopic configured with 3 partitions and replication factor 1
   */
  @Bean
  public NewTopic orderApprovedTopic() {
    return TopicBuilder.name(orderApprovedTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Topic for order completed domain events.
   *
   * @return NewTopic configured with 3 partitions and replication factor 1
   */
  @Bean
  public NewTopic orderCompletedTopic() {
    return TopicBuilder.name(orderCompletedTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }
}
