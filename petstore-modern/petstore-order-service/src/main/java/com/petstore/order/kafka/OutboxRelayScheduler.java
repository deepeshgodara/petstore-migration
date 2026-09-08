package com.petstore.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.order.document.OutboxDocument;
import com.petstore.order.repository.OutboxRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background relay scheduler implementing the Transactional Outbox pattern.
 * Continuously polls pending outbox entries, reliably streams them to Kafka,
 * and tracks delivery confirmation or routes permanently failed messages.
 */
@Component
@EnableScheduling
public class OutboxRelayScheduler {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

  private final OutboxRepository outboxRepository;
  private final KafkaTemplate<String, OrderDualWriteEvent> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public OutboxRelayScheduler(
      OutboxRepository outboxRepository,
      KafkaTemplate<String, OrderDualWriteEvent> kafkaTemplate,
      ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
  public void relayPendingEvents() {
    List<OutboxDocument> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
        "PENDING", PageRequest.of(0, 50));

    if (pending.isEmpty()) {
      return;
    }

    log.debug("Found {} pending outbox records to relay to Kafka", pending.size());

    for (OutboxDocument outbox : pending) {
      try {
        OrderDualWriteEvent event = objectMapper.readValue(outbox.getPayload(), OrderDualWriteEvent.class);
        kafkaTemplate.send(outbox.getTopic(), outbox.getAggregateId(), event).whenComplete((res, ex) -> {
          if (ex == null) {
            outbox.setStatus("PROCESSED");
            outbox.setProcessedAt(Instant.now());
            outboxRepository.save(outbox);
            log.debug("Successfully relayed outbox event [{}]", outbox.getId());
          } else {
            handleFailure(outbox, ex.getMessage());
          }
        });
      } catch (Exception e) {
        handleFailure(outbox, e.getMessage());
      }
    }
  }

  private void handleFailure(OutboxDocument outbox, String error) {
    int retries = outbox.getRetryCount() + 1;
    outbox.setRetryCount(retries);
    outbox.setErrorMessage(error);
    if (retries >= 5) {
      outbox.setStatus("FAILED");
      log.error("Outbox event [{}] permanently failed after {} attempts: {}",
          outbox.getId(), retries, error);
    }
    outboxRepository.save(outbox);
  }
}
