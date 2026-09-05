package com.petstore.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrderDualWriteEvent}.
 */
class OrderDualWriteEventTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  @DisplayName("Should create ORDER_CREATED dual-write event via factory")
  void shouldCreateOrderCreatedEvent() {
    OrderDocument order = new OrderDocument("ORD-100", "user1", null, OrderStatus.PENDING, BigDecimal.valueOf(99.99), "en_US", null, null, null, null);
    OrderDualWriteEvent event = OrderDualWriteEvent.ofCreated(order, "TEST_SYSTEM");

    assertThat(event.getEventId()).isNotBlank();
    assertThat(event.getOrderId()).isEqualTo("ORD-100");
    assertThat(event.getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
    assertThat(event.getOrder()).isEqualTo(order);
    assertThat(event.getPreviousStatus()).isNull();
    assertThat(event.getNewStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(event.getSource()).isEqualTo("TEST_SYSTEM");
    assertThat(event.getTimestamp()).isNotNull();
  }

  @Test
  @DisplayName("Should create ORDER_STATUS_UPDATED dual-write event via factory")
  void shouldCreateOrderStatusUpdatedEvent() {
    OrderDocument order = new OrderDocument("ORD-100", "user1", null, OrderStatus.APPROVED, BigDecimal.valueOf(99.99), "en_US", null, null, null, null);
    OrderDualWriteEvent event = OrderDualWriteEvent.ofStatusUpdated(order, OrderStatus.PENDING, OrderStatus.APPROVED, "TEST_SYSTEM");

    assertThat(event.getOrderId()).isEqualTo("ORD-100");
    assertThat(event.getEventType()).isEqualTo(OrderEventType.ORDER_STATUS_UPDATED);
    assertThat(event.getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(event.getNewStatus()).isEqualTo(OrderStatus.APPROVED);
  }

  @Test
  @DisplayName("Should serialize and deserialize JSON payload losslessly")
  void shouldSerializeAndDeserializeJson() throws Exception {
    OrderDocument order = new OrderDocument("ORD-200", "shopper", null, OrderStatus.COMPLETED, BigDecimal.valueOf(45.50), "ja_JP", null, null, null, null);
    OrderDualWriteEvent original = OrderDualWriteEvent.ofStatusUpdated(order, OrderStatus.APPROVED, OrderStatus.COMPLETED, "ADMIN_DASHBOARD");

    String json = objectMapper.writeValueAsString(original);
    assertThat(json).contains("ORD-200").contains("ORDER_STATUS_UPDATED");

    OrderDualWriteEvent deserialized = objectMapper.readValue(json, OrderDualWriteEvent.class);
    assertThat(deserialized.getEventId()).isEqualTo(original.getEventId());
    assertThat(deserialized.getOrderId()).isEqualTo("ORD-200");
    assertThat(deserialized.getEventType()).isEqualTo(OrderEventType.ORDER_STATUS_UPDATED);
    assertThat(deserialized.getPreviousStatus()).isEqualTo(OrderStatus.APPROVED);
    assertThat(deserialized.getNewStatus()).isEqualTo(OrderStatus.COMPLETED);
    assertThat(deserialized.getSource()).isEqualTo("ADMIN_DASHBOARD");
  }
}
