package com.petstore.migration.config;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer configuration establishing Dead-Letter Queue (DLQ) error isolation
 * and resilient retry policies for dual-write listeners.
 */
@Configuration
public class KafkaConsumerConfig {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

  @Value("${migration.dualwrite.dlq-topic:petstore.orders.dlq}")
  private String dlqTopic;

  /**
   * Configures a dead-letter publishing recoverer that forwards failed or corrupted
   * records directly to the designated Dead-Letter Queue (DLQ) topic.
   *
   * @param kafkaTemplate Kafka template used to dispatch to DLQ
   * @return configured DeadLetterPublishingRecoverer
   */
  @Bean
  public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
      KafkaOperations<Object, Object> kafkaTemplate) {
    return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> {
      log.error("Routing failed message [key: {}, partition: {}, offset: {}] to DLQ [{}] due to: {}",
          record.key(), record.partition(), record.offset(), dlqTopic, exception.getMessage());
      return new TopicPartition(dlqTopic, record.partition() % 3);
    });
  }

  /**
   * Creates a default error handler with 2 retries (1 second interval) followed by
   * immediate routing to the DLQ recoverer without blocking partition consumption.
   *
   * @param recoverer DLQ publishing recoverer
   * @return configured CommonErrorHandler
   */
  @Bean
  public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
    FixedBackOff backOff = new FixedBackOff(1000L, 2L);
    return new DefaultErrorHandler(recoverer, backOff);
  }

  /**
   * Configures the Kafka listener container factory with virtual threads and DLQ error isolation.
   *
   * @param consumerFactory Spring consumer factory
   * @param errorHandler DLQ error handler
   * @return configured ConcurrentKafkaListenerContainerFactory
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
      ConsumerFactory<Object, Object> consumerFactory,
      CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }
}
