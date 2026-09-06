package com.petstore.order.kafka;

import com.petstore.common.event.UserDomainEvent;
import com.petstore.user.document.UserDocument;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Producer publishing user lifecycle events (registration, profile update) to Kafka.
 */
@Component
public class UserEventProducer {

  private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String userCreatedTopic;

  public UserEventProducer(
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("${app.kafka.topics.user-created:petstore.users.created}") String userCreatedTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.userCreatedTopic = userCreatedTopic;
  }

  public CompletableFuture<SendResult<String, Object>> publishUserCreated(UserDocument user) {
    if (user == null) {
      return CompletableFuture.completedFuture(null);
    }
    UserDomainEvent event = UserDomainEvent.created(
        user.getUsername(),
        user.getEmail(),
        user.getRole()
    );

    log.info("Emitting UserDomainEvent [USER_CREATED] for user [{}] to Kafka topic [{}]",
        user.getUsername(), userCreatedTopic);

    return kafkaTemplate.send(userCreatedTopic, user.getUsername(), event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish UserDomainEvent for user [{}]: {}",
                user.getUsername(), ex.getMessage());
          } else {
            log.debug("Successfully published UserDomainEvent for user [{}] at offset [{}]",
                user.getUsername(), result.getRecordMetadata().offset());
          }
        });
  }
}
