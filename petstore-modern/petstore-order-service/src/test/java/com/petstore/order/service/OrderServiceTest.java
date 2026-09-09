package com.petstore.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.kafka.DualWritePublisher;
import com.petstore.order.kafka.OrderEventProducer;
import com.petstore.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
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

  @Test
  @DisplayName("Should generate comprehensive admin analytics with category sales and customer cohorts")
  void shouldGenerateAdminAnalytics() {
    OrderDocument o1 = new OrderDocument();
    o1.setId("ORD-1");
    o1.setUserId("customerA");
    o1.setOrderDate(Instant.now());
    o1.setStatus(OrderStatus.APPROVED);
    o1.setTotalPrice(BigDecimal.valueOf(100.00));
    LineItemDocument li1 = new LineItemDocument(1, "EST-1", "FI-SW-01", "FISH", 2,
        BigDecimal.valueOf(50.00), BigDecimal.valueOf(100.00));
    o1.setLineItems(List.of(li1));

    OrderDocument o2 = new OrderDocument();
    o2.setId("ORD-2");
    o2.setUserId("customerA"); // returning customer!
    o2.setOrderDate(Instant.now());
    o2.setStatus(OrderStatus.COMPLETED);
    o2.setTotalPrice(BigDecimal.valueOf(50.00));
    LineItemDocument li2 = new LineItemDocument(1, "EST-6", "K9-BD-01", "DOGS", 1,
        BigDecimal.valueOf(50.00), BigDecimal.valueOf(50.00));
    o2.setLineItems(List.of(li2));

    OrderDocument o3 = new OrderDocument();
    o3.setId("ORD-3");
    o3.setUserId("customerB"); // unique customer
    o3.setOrderDate(Instant.now());
    o3.setStatus(OrderStatus.PENDING);
    o3.setTotalPrice(BigDecimal.valueOf(25.00));

    when(orderRepository.findAll()).thenReturn(List.of(o1, o2, o3));

    com.petstore.order.dto.AdminAnalyticsResponse analytics = orderService.getAdminAnalytics(null, null);

    assertThat(analytics.totalOrders()).isEqualTo(3);
    assertThat(analytics.totalRevenue()).isEqualByComparingTo("175.00");
    assertThat(analytics.uniqueCustomers()).isEqualTo(2);
    assertThat(analytics.returningCustomers()).isEqualTo(1);
    assertThat(analytics.repeatCustomerRate()).isEqualTo(50.0);
    assertThat(analytics.salesByCategory()).isNotEmpty();

    // Check FISH category was computed
    var fishMetric = analytics.salesByCategory().stream()
        .filter(c -> "FISH".equalsIgnoreCase(c.categoryId()))
        .findFirst();
    assertThat(fishMetric).isPresent();
    assertThat(fishMetric.get().totalRevenue()).isEqualByComparingTo("100.00");
    assertThat(fishMetric.get().unitsSold()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should generate strictly numeric integer order ID disjoint from legacy format")
  void shouldGenerateStrictlyNumericIntegerOrderId() {
    when(orderRepository.existsById(any(String.class))).thenReturn(false);

    String orderId = orderService.generateUniqueOrderId();

    assertThat(orderId).isNotBlank();
    assertThat(orderId).matches("^\\d+$");
    assertThat(orderId.length()).isGreaterThanOrEqualTo(16);

    // Verify it is parseable as a 64-bit signed integer (Long)
    long numericVal = Long.parseLong(orderId);
    assertThat(numericVal).isPositive();

    // Verify disjoint range: modern timestamp-based ID is > 10^16, legacy is < 10^7
    assertThat(numericVal).isGreaterThan(100_000_000_000_000L);
  }

  @Test
  @DisplayName("Should retry when candidate order ID collides with existing document")
  void shouldRetryWhenCandidateOrderIdCollides() {
    // First candidate collides, second candidate is available
    when(orderRepository.existsById(any(String.class)))
        .thenReturn(true)
        .thenReturn(false);

    String orderId = orderService.generateUniqueOrderId();

    assertThat(orderId).isNotBlank();
    assertThat(orderId).matches("^\\d+$");
    verify(orderRepository, org.mockito.Mockito.atLeast(2)).existsById(any(String.class));
  }
}
