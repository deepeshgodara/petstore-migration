package com.petstore.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.dto.CreateOrderRequest;
import com.petstore.order.dto.OrderSummaryResponse;
import com.petstore.order.dto.UpdateOrderStatusRequest;
import com.petstore.order.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link OrderController}.
 */
class OrderControllerTest {

  private OrderService orderService;
  private OrderController controller;

  @BeforeEach
  void setUp() {
    orderService = mock(OrderService.class);
    controller = new OrderController(orderService);
  }

  @Test
  @DisplayName("Should place customer order and return 201 Created")
  void shouldPlaceOrder() {
    CreateOrderRequest request = new CreateOrderRequest(
        "shopper", "en_US", null, null, null, null);
    OrderDocument saved = new OrderDocument(
        "ORD-777", "shopper", null, OrderStatus.PENDING,
        BigDecimal.TEN, "en_US", null, null, null, null);

    when(orderService.placeOrder(any(CreateOrderRequest.class))).thenReturn(saved);

    ResponseEntity<OrderDocument> response = controller.placeOrder(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo(saved);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/orders/ORD-777");
  }

  @Test
  @DisplayName("Should retrieve order by ID")
  void shouldGetOrderById() {
    OrderDocument order = new OrderDocument(
        "ORD-100", "user1", null, OrderStatus.PENDING,
        BigDecimal.TEN, "en_US", null, null, null, null);
    when(orderService.getOrderById("ORD-100")).thenReturn(Optional.of(order));

    ResponseEntity<OrderDocument> response = controller.getOrderById("ORD-100");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(order);
  }

  @Test
  @DisplayName("Should return 404 when looking up non-existent order")
  void shouldReturn404WhenNotFound() {
    when(orderService.getOrderById("UNKNOWN")).thenReturn(Optional.empty());

    ResponseEntity<OrderDocument> response = controller.getOrderById("UNKNOWN");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Should update order status to APPROVED and return 200 OK")
  void shouldUpdateOrderStatus() {
    UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.APPROVED);
    OrderDocument updated = new OrderDocument(
        "ORD-100", "user1", null, OrderStatus.APPROVED,
        BigDecimal.TEN, "en_US", null, null, null, null);

    when(orderService.updateOrderStatus(eq("ORD-100"), eq(OrderStatus.APPROVED)))
        .thenReturn(updated);

    ResponseEntity<OrderDocument> response = controller.updateOrderStatus("ORD-100", request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.APPROVED);
  }

  @Test
  @DisplayName("Should return 404 when updating status of non-existent order")
  void shouldReturn404OnUpdateNonExistent() {
    UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.COMPLETED);
    when(orderService.updateOrderStatus(eq("NON-EXISTENT"), eq(OrderStatus.COMPLETED)))
        .thenThrow(new NoSuchElementException("Not found"));

    ResponseEntity<OrderDocument> response = controller.updateOrderStatus("NON-EXISTENT", request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Should return admin summary analytics on GET /admin/summary")
  void shouldGetAdminSummary() {
    OrderSummaryResponse summary = new OrderSummaryResponse(
        10L, BigDecimal.valueOf(1500.00), Map.of("PENDING", 2L, "APPROVED", 3L, "COMPLETED", 5L));
    when(orderService.getOrderSummary()).thenReturn(summary);

    ResponseEntity<OrderSummaryResponse> response = controller.getAdminSummary();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().totalOrders()).isEqualTo(10L);
    assertThat(response.getBody().totalRevenue()).isEqualTo(BigDecimal.valueOf(1500.00));
  }
}
