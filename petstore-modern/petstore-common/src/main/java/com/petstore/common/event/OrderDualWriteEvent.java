package com.petstore.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Event payload dispatched to the dual-write Kafka topic (orders.dualwrite)
 * whenever an order is created or its lifecycle status is updated.
 */
public class OrderDualWriteEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String eventId;
  private final String orderId;
  private final OrderEventType eventType;
  private final OrderDocument order;
  private final OrderStatus previousStatus;
  private final OrderStatus newStatus;
  private final Instant timestamp;
  private final String source;

  @JsonCreator
  public OrderDualWriteEvent(
      @JsonProperty("eventId") String eventId,
      @JsonProperty("orderId") String orderId,
      @JsonProperty("eventType") OrderEventType eventType,
      @JsonProperty("order") OrderDocument order,
      @JsonProperty("previousStatus") OrderStatus previousStatus,
      @JsonProperty("newStatus") OrderStatus newStatus,
      @JsonProperty("timestamp") Instant timestamp,
      @JsonProperty("source") String source) {
    this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
    this.orderId = orderId;
    this.eventType = eventType;
    this.order = order;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.timestamp = timestamp != null ? timestamp : Instant.now();
    this.source = source != null ? source : "PETSTORE_ORDER_SERVICE";
  }

  /**
   * Factory method for creating an ORDER_CREATED dual-write event.
   *
   * @param order the newly created order document aggregate
   * @param source the originating source system
   * @return initialized OrderDualWriteEvent
   */
  public static OrderDualWriteEvent ofCreated(OrderDocument order, String source) {
    return new OrderDualWriteEvent(
        UUID.randomUUID().toString(),
        order != null ? order.getId() : null,
        OrderEventType.ORDER_CREATED,
        order,
        null,
        order != null ? order.getStatus() : OrderStatus.PENDING,
        Instant.now(),
        source
    );
  }

  /**
   * Factory method for creating an ORDER_STATUS_UPDATED dual-write event.
   *
   * @param order the updated order document aggregate
   * @param previousStatus status prior to update
   * @param newStatus updated status
   * @param source the originating source system
   * @return initialized OrderDualWriteEvent
   */
  public static OrderDualWriteEvent ofStatusUpdated(
      OrderDocument order,
      OrderStatus previousStatus,
      OrderStatus newStatus,
      String source) {
    return new OrderDualWriteEvent(
        UUID.randomUUID().toString(),
        order != null ? order.getId() : null,
        OrderEventType.ORDER_STATUS_UPDATED,
        order,
        previousStatus,
        newStatus,
        Instant.now(),
        source
    );
  }

  public String getEventId() {
    return eventId;
  }

  public String getOrderId() {
    return orderId;
  }

  public OrderEventType getEventType() {
    return eventType;
  }

  public OrderDocument getOrder() {
    return order;
  }

  public OrderStatus getPreviousStatus() {
    return previousStatus;
  }

  public OrderStatus getNewStatus() {
    return newStatus;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderDualWriteEvent that = (OrderDualWriteEvent) o;
    return Objects.equals(eventId, that.eventId)
        && Objects.equals(orderId, that.orderId)
        && eventType == that.eventType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventId, orderId, eventType);
  }

  @Override
  public String toString() {
    return "OrderDualWriteEvent{"
        + "eventId='" + eventId + '\''
        + ", orderId='" + orderId + '\''
        + ", eventType=" + eventType
        + ", previousStatus=" + previousStatus
        + ", newStatus=" + newStatus
        + ", timestamp=" + timestamp
        + ", source='" + source + '\''
        + '}';
  }
}
