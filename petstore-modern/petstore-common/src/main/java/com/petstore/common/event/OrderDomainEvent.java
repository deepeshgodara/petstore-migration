package com.petstore.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.petstore.order.document.OrderStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event dispatched to Kafka upon order lifecycle state transitions.
 */
public class OrderDomainEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String eventId;
  private final String orderId;
  private final String userId;
  private final OrderEventType eventType;
  private final OrderStatus status;
  private final BigDecimal totalPrice;
  private final int totalItems;
  private final Instant timestamp;

  @JsonCreator
  public OrderDomainEvent(
      @JsonProperty("eventId") String eventId,
      @JsonProperty("orderId") String orderId,
      @JsonProperty("userId") String userId,
      @JsonProperty("eventType") OrderEventType eventType,
      @JsonProperty("status") OrderStatus status,
      @JsonProperty("totalPrice") BigDecimal totalPrice,
      @JsonProperty("totalItems") int totalItems,
      @JsonProperty("timestamp") Instant timestamp) {
    this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
    this.orderId = orderId;
    this.userId = userId;
    this.eventType = eventType;
    this.status = status;
    this.totalPrice = totalPrice;
    this.totalItems = totalItems;
    this.timestamp = timestamp != null ? timestamp : Instant.now();
  }

  public String getEventId() {
    return eventId;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getUserId() {
    return userId;
  }

  public OrderEventType getEventType() {
    return eventType;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public int getTotalItems() {
    return totalItems;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OrderDomainEvent that)) {
      return false;
    }
    return totalItems == that.totalItems
        && Objects.equals(eventId, that.eventId)
        && Objects.equals(orderId, that.orderId)
        && Objects.equals(userId, that.userId)
        && eventType == that.eventType
        && status == that.status
        && Objects.equals(totalPrice, that.totalPrice)
        && Objects.equals(timestamp, that.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        eventId, orderId, userId, eventType, status, totalPrice, totalItems, timestamp);
  }

  @Override
  public String toString() {
    return "OrderDomainEvent{"
        + "eventId='" + eventId + '\''
        + ", orderId='" + orderId + '\''
        + ", userId='" + userId + '\''
        + ", eventType=" + eventType
        + ", status=" + status
        + ", totalPrice=" + totalPrice
        + ", totalItems=" + totalItems
        + ", timestamp=" + timestamp
        + '}';
  }
}
