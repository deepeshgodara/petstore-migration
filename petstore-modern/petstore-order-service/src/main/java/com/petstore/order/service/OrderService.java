package com.petstore.order.service;

import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.kafka.DualWritePublisher;
import com.petstore.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service managing order lifecycle operations and orchestrating asynchronous
 * dual-write event dispatching via {@link DualWritePublisher}.
 */
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final DualWritePublisher dualWritePublisher;

  public OrderService(
      OrderRepository orderRepository,
      DualWritePublisher dualWritePublisher) {
    this.orderRepository = orderRepository;
    this.dualWritePublisher = dualWritePublisher;
  }

  /**
   * Persists a new customer order and triggers an asynchronous dual-write event to Kafka.
   *
   * @param order the order to create
   * @return the saved order document
   */
  public OrderDocument createOrder(OrderDocument order) {
    if (order == null) {
      throw new IllegalArgumentException("Order document cannot be null");
    }

    if (order.getId() == null || order.getId().isBlank()) {
      order.setId(UUID.randomUUID().toString());
    }

    if (order.getOrderDate() == null) {
      order.setOrderDate(Instant.now());
    }

    if (order.getStatus() == null) {
      order.setStatus(OrderStatus.PENDING);
    }

    order.setCreatedAt(Instant.now());
    order.setUpdatedAt(Instant.now());

    OrderDocument savedOrder = orderRepository.save(order);
    log.info("Successfully persisted order [{}] with status [{}]", savedOrder.getId(), savedOrder.getStatus());

    // Trigger dual-write asynchronously
    dualWritePublisher.publishOrderCreated(savedOrder);

    return savedOrder;
  }

  /**
   * Updates an order's lifecycle status and triggers an asynchronous dual-write event to Kafka.
   *
   * @param orderId the order identifier
   * @param newStatus the target order status
   * @return the updated order document
   * @throws NoSuchElementException if the order is not found
   */
  public OrderDocument updateOrderStatus(String orderId, OrderStatus newStatus) {
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order ID cannot be null or blank");
    }
    if (newStatus == null) {
      throw new IllegalArgumentException("Target order status cannot be null");
    }

    OrderDocument order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));

    OrderStatus previousStatus = order.getStatus();
    order.setStatus(newStatus);
    order.setUpdatedAt(Instant.now());

    OrderDocument updatedOrder = orderRepository.save(order);
    log.info("Updated order [{}] status from [{}] to [{}]", orderId, previousStatus, newStatus);

    // Trigger dual-write asynchronously
    dualWritePublisher.publishOrderStatusUpdated(updatedOrder, previousStatus, newStatus);

    return updatedOrder;
  }

  /**
   * Retrieves an order by unique identifier.
   *
   * @param orderId order identifier
   * @return optional containing the order document if found
   */
  public Optional<OrderDocument> getOrderById(String orderId) {
    return orderRepository.findById(orderId);
  }

  /**
   * Retrieves all orders for a customer ordered by order date descending.
   *
   * @param userId customer identifier
   * @return list of customer orders
   */
  public List<OrderDocument> getOrdersByUserId(String userId) {
    return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
  }

  /**
   * Retrieves all orders currently in a specific lifecycle status.
   *
   * @param status order status filter
   * @return list of matching orders
   */
  public List<OrderDocument> getOrdersByStatus(OrderStatus status) {
    return orderRepository.findByStatusOrderByOrderDateDesc(status);
  }
}
