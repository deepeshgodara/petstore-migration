package com.petstore.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.kafka.DualWritePublisher;
import com.petstore.order.kafka.OrderEventProducer;
import com.petstore.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrderService}.
 */
class OrderServiceTest {

  private OrderRepository orderRepository;
  private DualWritePublisher dualWritePublisher;
  private OrderEventProducer orderEventProducer;
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderRepository = mock(OrderRepository.class);
    dualWritePublisher = mock(DualWritePublisher.class);
    orderEventProducer = mock(OrderEventProducer.class);
    orderService = new OrderService(orderRepository, dualWritePublisher, orderEventProducer);
  }

  @Test
  @DisplayName("Should create order, persist, and trigger dual-write and domain events")
  void shouldCreateOrderAndTriggerDualWrite() {
    OrderDocument inputOrder = new OrderDocument();
    inputOrder.setUserId("shopper");
    inputOrder.setTotalPrice(BigDecimal.valueOf(120.00));

    when(orderRepository.save(any(OrderDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrderDocument created = orderService.createOrder(inputOrder);

    assertThat(created.getId()).isNotBlank();
    assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(created.getCreatedAt()).isNotNull();

    verify(orderRepository).save(created);
    verify(dualWritePublisher).publishOrderCreated(created);
    verify(orderEventProducer).publishOrderCreated(created);
  }

  @Test
  @DisplayName("Should update status, persist, and trigger dual-write and domain events")
  void shouldUpdateOrderStatusAndTriggerDualWrite() {
    OrderDocument existingOrder = new OrderDocument(
        "ORD-555", "shopper", null, OrderStatus.PENDING,
        BigDecimal.valueOf(80), "en_US", null, null, null, null);

    when(orderRepository.findById("ORD-555")).thenReturn(Optional.of(existingOrder));
    when(orderRepository.save(any(OrderDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrderDocument updated = orderService.updateOrderStatus("ORD-555", OrderStatus.APPROVED);

    assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED);
    verify(orderRepository).save(existingOrder);
    verify(dualWritePublisher).publishOrderStatusUpdated(
        existingOrder, OrderStatus.PENDING, OrderStatus.APPROVED);
    verify(orderEventProducer).publishOrderStatusUpdated(
        existingOrder, OrderStatus.PENDING, OrderStatus.APPROVED);
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when updating non-existent order")
  void shouldThrowWhenUpdatingNonExistentOrder() {
    when(orderRepository.findById("ORD-999")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.updateOrderStatus("ORD-999", OrderStatus.COMPLETED))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("ORD-999");
  }

  @Test
  @DisplayName("Should retrieve order by ID")
  void shouldGetOrderById() {
    OrderDocument order = new OrderDocument();
    order.setId("ORD-123");
    when(orderRepository.findById("ORD-123")).thenReturn(Optional.of(order));

    Optional<OrderDocument> result = orderService.getOrderById("ORD-123");
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("ORD-123");
  }

  @Test
  @DisplayName("Should retrieve orders by user ID")
  void shouldGetOrdersByUserId() {
    List<OrderDocument> orders = List.of(new OrderDocument());
    when(orderRepository.findByUserIdOrderByOrderDateDesc("user1")).thenReturn(orders);

    List<OrderDocument> result = orderService.getOrdersByUserId("user1");
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("Should retrieve orders by status")
  void shouldGetOrdersByStatus() {
    List<OrderDocument> orders = List.of(new OrderDocument());
    when(orderRepository.findByStatusOrderByOrderDateDesc(OrderStatus.PENDING)).thenReturn(orders);

    List<OrderDocument> result = orderService.getOrdersByStatus(OrderStatus.PENDING);
    assertThat(result).hasSize(1);
  }
}
