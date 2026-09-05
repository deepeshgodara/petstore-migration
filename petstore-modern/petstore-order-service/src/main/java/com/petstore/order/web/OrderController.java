package com.petstore.order.web;

import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.dto.CreateOrderRequest;
import com.petstore.order.dto.OrderSummaryResponse;
import com.petstore.order.dto.UpdateOrderStatusRequest;
import com.petstore.order.service.OrderService;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for customer order placement, order lookup,
 * and administrative lifecycle transitions (PENDING, APPROVED, COMPLETED, DENIED, CANCELLED).
 */
@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * Places a customer purchase order, persists to MongoDB, and dispatches
   * an asynchronous dual-write event.
   *
   * @param request checkout request details
   * @return 201 Created with saved OrderDocument
   */
  @PostMapping
  public ResponseEntity<OrderDocument> placeOrder(@RequestBody CreateOrderRequest request) {
    OrderDocument created = orderService.placeOrder(request);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + created.getId())).body(created);
  }

  /**
   * Retrieves an order by identifier.
   *
   * @param orderId order identifier
   * @return OrderDocument or 404 Not Found
   */
  @GetMapping("/{orderId}")
  public ResponseEntity<OrderDocument> getOrderById(@PathVariable String orderId) {
    return orderService.getOrderById(orderId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Queries orders with optional customer ID or lifecycle status filters.
   *
   * @param userId optional customer ID
   * @param status optional status filter
   * @return list of matching orders
   */
  @GetMapping
  public ResponseEntity<List<OrderDocument>> getOrders(
      @RequestParam(name = "userId", required = false) String userId,
      @RequestParam(name = "status", required = false) OrderStatus status) {
    List<OrderDocument> orders = orderService.getOrders(userId, status);
    return ResponseEntity.ok(orders);
  }

  /**
   * Updates an order's lifecycle status (e.g., administrator approving
   * or completing a pending order). Supports both PATCH and PUT methods.
   *
   * @param orderId order identifier
   * @param request status update request
   * @return updated OrderDocument or 404 Not Found
   */
  @RequestMapping(
      value = "/{orderId}/status",
      method = {RequestMethod.PATCH, RequestMethod.PUT})
  public ResponseEntity<OrderDocument> updateOrderStatus(
      @PathVariable String orderId,
      @RequestBody UpdateOrderStatusRequest request) {
    if (request == null || request.status() == null) {
      return ResponseEntity.badRequest().build();
    }
    try {
      OrderDocument updated = orderService.updateOrderStatus(orderId, request.status());
      return ResponseEntity.ok(updated);
    } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Analytical summary reporting total orders, aggregate revenue, and status breakdown.
   *
   * @return OrderSummaryResponse
   */
  @GetMapping("/admin/summary")
  public ResponseEntity<OrderSummaryResponse> getAdminSummary() {
    OrderSummaryResponse summary = orderService.getOrderSummary();
    return ResponseEntity.ok(summary);
  }
}
