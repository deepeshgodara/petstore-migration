package com.petstore.order.document;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing an event in the Transactional Outbox pattern.
 * Persisted atomically alongside domain entities to guarantee at-least-once
 * Kafka event dispatching without distributed two-phase commits.
 * Maps to the {@code petstore_outbox} collection.
 */
@Document(collection = "petstore_outbox")
@CompoundIndexes({
    @CompoundIndex(name = "status_createdAt_idx", def = "{'status': 1, 'createdAt': 1}")
})
public class OutboxDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  private String id;

  @Indexed
  private String aggregateType;

  @Indexed
  private String aggregateId;

  private String eventType;
  private String topic;
  private String payload;

  @Indexed
  private String status = "PENDING"; // PENDING, PROCESSED, FAILED

  private int retryCount = 0;
  private Instant createdAt = Instant.now();
  private Instant processedAt;
  private String errorMessage;

  public OutboxDocument() {
    this.id = UUID.randomUUID().toString();
  }

  public OutboxDocument(
      String aggregateType,
      String aggregateId,
      String eventType,
      String topic,
      String payload) {
    this.id = UUID.randomUUID().toString();
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.topic = topic;
    this.payload = payload;
    this.createdAt = Instant.now();
    this.status = "PENDING";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
