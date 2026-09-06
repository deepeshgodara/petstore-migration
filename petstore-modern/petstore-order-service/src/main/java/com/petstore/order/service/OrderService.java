package com.petstore.order.service;

import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.document.PaymentDocument;
import com.petstore.order.dto.AdminAnalyticsResponse;
import com.petstore.order.dto.CategorySalesMetric;
import com.petstore.order.dto.CreateOrderRequest;
import com.petstore.order.dto.DailySalesMetric;
import com.petstore.order.dto.OrderSummaryResponse;
import com.petstore.order.kafka.DualWritePublisher;
import com.petstore.order.kafka.OrderEventProducer;
import com.petstore.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service managing order lifecycle operations and orchestrating asynchronous
 * dual-write event dispatching and business domain event streaming.
 */
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final DualWritePublisher dualWritePublisher;
  private final OrderEventProducer orderEventProducer;

  public OrderService(
      OrderRepository orderRepository,
      DualWritePublisher dualWritePublisher,
      OrderEventProducer orderEventProducer) {
    this.orderRepository = orderRepository;
    this.dualWritePublisher = dualWritePublisher;
    this.orderEventProducer = orderEventProducer;
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

    // Publish domain event asynchronously
    orderEventProducer.publishOrderCreated(savedOrder);

    return savedOrder;
  }

  /**
   * Updates an order's lifecycle status, triggers an asynchronous dual-write event to Kafka,
   * and publishes domain state transition events.
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

    // Publish domain event asynchronously
    orderEventProducer.publishOrderStatusUpdated(updatedOrder, previousStatus, newStatus);

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

  /**
   * Generates comprehensive analytics for administrator visibility, including sales by
   * pet category, time-series volume trends, and unique/returning customer cohorts.
   *
   * @param startDate optional starting date filter
   * @param endDate optional ending date filter
   * @return AdminAnalyticsResponse
   */
  public AdminAnalyticsResponse getAdminAnalytics(Instant startDate, Instant endDate) {
    List<OrderDocument> allOrders = orderRepository.findAll();

    List<OrderDocument> matchingOrders = allOrders.stream()
        .filter(o -> {
          if (o.getOrderDate() == null) {
            return true;
          }
          if (startDate != null && o.getOrderDate().isBefore(startDate)) {
            return false;
          }
          if (endDate != null && o.getOrderDate().isAfter(endDate)) {
            return false;
          }
          return true;
        })
        .toList();

    long totalOrders = matchingOrders.size();
    BigDecimal totalRevenue = matchingOrders.stream()
        .map(OrderDocument::getTotalPrice)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal averageOrderValue = (totalOrders > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0)
        ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    // Customer Cohorts
    Map<String, Long> userOrderCounts = matchingOrders.stream()
        .map(OrderDocument::getUserId)
        .filter(u -> u != null && !u.isBlank())
        .collect(Collectors.groupingBy(u -> u, Collectors.counting()));

    long uniqueCustomers = userOrderCounts.size();
    long returningCustomers = userOrderCounts.values().stream()
        .filter(cnt -> cnt >= 2)
        .count();

    double repeatCustomerRate = uniqueCustomers > 0
        ? Math.round(((double) returningCustomers / uniqueCustomers) * 1000.0) / 10.0
        : 0.0;

    // Status Breakdown
    Map<String, Long> statusBreakdown = matchingOrders.stream()
        .filter(o -> o.getStatus() != null)
        .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

    // Category Sales Breakdown
    Map<String, String> categoryNames = Map.of(
        "FISH", "Fish",
        "DOGS", "Dogs",
        "REPTILES", "Reptiles",
        "CATS", "Cats",
        "BIRDS", "Birds"
    );

    Map<String, CategoryAccumulator> categoryMap = new HashMap<>();
    for (String catKey : categoryNames.keySet()) {
      categoryMap.put(catKey, new CategoryAccumulator(catKey, categoryNames.get(catKey)));
    }

    BigDecimal totalCategoryRevenue = BigDecimal.ZERO;

    for (OrderDocument order : matchingOrders) {
      if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
        for (LineItemDocument li : order.getLineItems()) {
          String cat = li.getCategoryId() != null ? li.getCategoryId().toUpperCase().trim() : "FISH";
          CategoryAccumulator acc = categoryMap.computeIfAbsent(cat,
              k -> new CategoryAccumulator(k, categoryNames.getOrDefault(k, k)));
          BigDecimal cost = li.getTotalCost() != null ? li.getTotalCost() : BigDecimal.ZERO;
          acc.revenue = acc.revenue.add(cost);
          acc.quantity += li.getQuantity();
          acc.orderIds.add(order.getId());
          totalCategoryRevenue = totalCategoryRevenue.add(cost);
        }
      }
    }

    final BigDecimal finalCatRevenue = totalCategoryRevenue.compareTo(BigDecimal.ZERO) > 0
        ? totalCategoryRevenue
        : (totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? totalRevenue : BigDecimal.ONE);

    List<CategorySalesMetric> salesByCategory = categoryMap.values().stream()
        .map(acc -> {
          double pct = finalCatRevenue.compareTo(BigDecimal.ZERO) > 0
              ? acc.revenue.multiply(BigDecimal.valueOf(100))
                  .divide(finalCatRevenue, 1, RoundingMode.HALF_UP).doubleValue()
              : 0.0;
          return new CategorySalesMetric(
              acc.categoryId,
              acc.categoryName,
              acc.revenue,
              acc.quantity,
              acc.orderIds.size(),
              pct
          );
        })
        .sorted(Comparator.comparing(CategorySalesMetric::totalRevenue).reversed())
        .toList();

    // Time-series daily sales
    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"));
    Map<String, DailyAccumulator> dailyMap = new TreeMap<>();
    for (OrderDocument order : matchingOrders) {
      if (order.getOrderDate() != null) {
        String day = formatter.format(order.getOrderDate());
        DailyAccumulator dAcc = dailyMap.computeIfAbsent(day, k -> new DailyAccumulator(k));
        dAcc.orderCount++;
        if (order.getTotalPrice() != null) {
          dAcc.revenue = dAcc.revenue.add(order.getTotalPrice());
        }
      }
    }

    List<DailySalesMetric> salesOverTime = dailyMap.values().stream()
        .map(d -> new DailySalesMetric(d.date, d.revenue, d.orderCount))
        .toList();

    String startStr = startDate != null ? startDate.toString() : null;
    String endStr = endDate != null ? endDate.toString() : null;

    return new AdminAnalyticsResponse(
        totalOrders,
        totalRevenue,
        uniqueCustomers,
        returningCustomers,
        repeatCustomerRate,
        averageOrderValue,
        salesByCategory,
        salesOverTime,
        statusBreakdown,
        startStr,
        endStr
    );
  }

  private static class CategoryAccumulator {
    final String categoryId;
    final String categoryName;
    BigDecimal revenue = BigDecimal.ZERO;
    long quantity = 0;
    final Set<String> orderIds = new HashSet<>();

    CategoryAccumulator(String categoryId, String categoryName) {
      this.categoryId = categoryId;
      this.categoryName = categoryName;
    }
  }

  private static class DailyAccumulator {
    final String date;
    BigDecimal revenue = BigDecimal.ZERO;
    long orderCount = 0;

    DailyAccumulator(String date) {
      this.date = date;
    }
  }
}
