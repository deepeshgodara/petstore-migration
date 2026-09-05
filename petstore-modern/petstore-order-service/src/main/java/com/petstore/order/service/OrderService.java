package com.petstore.order.service;

import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.document.PaymentDocument;
import com.petstore.order.dto.CreateOrderRequest;
import com.petstore.order.dto.OrderSummaryResponse;
import com.petstore.order.kafka.DualWritePublisher;
import com.petstore.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
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
   * Places a customer order from a CreateOrderRequest, calculates line item costs,
   * masks payment details, persists to MongoDB, and triggers dual-write publishing.
   *
   * @param request checkout request payload
   * @return the newly placed OrderDocument
   */
  public OrderDocument placeOrder(CreateOrderRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("CreateOrderRequest cannot be null");
    }
    if (request.userId() == null || request.userId().isBlank()) {
      throw new IllegalArgumentException("Customer userId is required to place an order");
    }

    String orderId = String.valueOf(System.currentTimeMillis());
    Instant now = Instant.now();

    BigDecimal totalPrice = BigDecimal.ZERO;
    List<LineItemDocument> lineItems = new ArrayList<>();
    if (request.lineItems() != null) {
      int lineNum = 1;
      for (LineItemDocument item : request.lineItems()) {
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;
        BigDecimal itemCost = unitPrice.multiply(BigDecimal.valueOf(qty));
        totalPrice = totalPrice.add(itemCost);

        LineItemDocument lineItem = new LineItemDocument(
            lineNum++,
            item.getItemId(),
            item.getProductId(),
            item.getCategoryId(),
            qty,
            unitPrice,
            itemCost
        );
        lineItems.add(lineItem);
      }
    }

    PaymentDocument payment = request.payment();
    if (payment != null
        && (payment.getCardNumberMasked() == null || payment.getCardNumberMasked().isBlank())) {
      payment.setCardNumberMasked("XXXX-XXXX-XXXX-0000");
    }

    String locale = (request.locale() != null && !request.locale().isBlank())
        ? request.locale()
        : "en_US";

    OrderDocument order = new OrderDocument(
        orderId,
        request.userId(),
        now,
        OrderStatus.PENDING,
        totalPrice,
        locale,
        request.billing(),
        request.shipping(),
        payment,
        lineItems
    );

    return createOrder(order);
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
    log.info("Successfully persisted order [{}] with status [{}]",
        savedOrder.getId(), savedOrder.getStatus());

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
   * Retrieves orders optionally filtered by user ID or lifecycle status.
   *
   * @param userId optional customer ID
   * @param status optional order status
   * @return list of matching orders
   */
  public List<OrderDocument> getOrders(String userId, OrderStatus status) {
    if (userId != null && !userId.isBlank()) {
      return getOrdersByUserId(userId.trim());
    } else if (status != null) {
      return getOrdersByStatus(status);
    } else {
      return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
    }
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

  /**
   * Aggregates total orders, total revenue, and status breakdown for administrator overview.
   *
   * @return OrderSummaryResponse
   */
  public OrderSummaryResponse getOrderSummary() {
    long totalOrders = orderRepository.count();
    List<OrderRepository.RevenueSummary> revenueList = orderRepository.calculateRevenueSummary();
    BigDecimal totalRevenue = BigDecimal.ZERO;
    if (revenueList != null && !revenueList.isEmpty()
        && revenueList.get(0).getTotalRevenue() != null) {
      totalRevenue = revenueList.get(0).getTotalRevenue();
    }

    List<OrderRepository.OrderStatusSummary> breakdownList =
        orderRepository.getOrderStatusBreakdown();
    Map<String, Long> statusMap = new HashMap<>();
    if (breakdownList != null) {
      for (OrderRepository.OrderStatusSummary s : breakdownList) {
        if (s.getStatus() != null) {
          statusMap.put(s.getStatus(), s.getCount());
        }
      }
    }

    return new OrderSummaryResponse(totalOrders, totalRevenue, statusMap);
  }
}
