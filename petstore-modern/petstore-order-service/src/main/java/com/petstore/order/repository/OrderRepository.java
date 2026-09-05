package com.petstore.order.repository;

import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for Order documents with custom aggregation pipelines.
 */
@Repository
public interface OrderRepository extends MongoRepository<OrderDocument, String> {

  /**
   * Retrieves all orders for a specific customer, ordered by order date descending.
   *
   * @param userId customer user identifier
   * @return list of customer orders
   */
  List<OrderDocument> findByUserIdOrderByOrderDateDesc(String userId);

  /**
   * Retrieves orders by lifecycle status, ordered by order date descending.
   *
   * @param status order status (e.g., PENDING, APPROVED, COMPLETED)
   * @return list of orders with the specified status
   */
  List<OrderDocument> findByStatusOrderByOrderDateDesc(OrderStatus status);

  /**
   * Counts total orders in a given status.
   *
   * @param status order status
   * @return count of orders
   */
  long countByStatus(OrderStatus status);

  /**
   * Aggregation pipeline to compute total revenue for approved/completed orders.
   *
   * @return list containing revenue summary projection if orders exist
   */
  @Aggregation(pipeline = {
      "{ '$match': { 'status': { '$in': ['APPROVED', 'COMPLETED'] } } }",
      "{ '$group': { '_id': null, 'totalRevenue': { '$sum': { '$toDecimal': '$totalPrice' } },"
          + " 'orderCount': { '$sum': 1 } } }"
  })
  List<RevenueSummary> calculateRevenueSummary();

  /**
   * Aggregation pipeline to group order counts by status.
   *
   * @return list of status breakdown projections
   */
  @Aggregation(pipeline = {
      "{ '$group': { '_id': '$status', 'count': { '$sum': 1 },"
          + " 'totalAmount': { '$sum': { '$toDecimal': '$totalPrice' } } } }",
      "{ '$project': { 'status': '$_id', 'count': 1, 'totalAmount': 1, '_id': 0 } }"
  })
  List<OrderStatusSummary> getOrderStatusBreakdown();

  /**
   * Projection class for revenue calculation.
   */
  class RevenueSummary {
    private BigDecimal totalRevenue;
    private long orderCount;

    public RevenueSummary() {}

    public RevenueSummary(BigDecimal totalRevenue, long orderCount) {
      this.totalRevenue = totalRevenue;
      this.orderCount = orderCount;
    }

    public BigDecimal getTotalRevenue() {
      return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
      this.totalRevenue = totalRevenue;
    }

    public long getOrderCount() {
      return orderCount;
    }

    public void setOrderCount(long orderCount) {
      this.orderCount = orderCount;
    }
  }

  /**
   * Projection class for order status breakdown.
   */
  class OrderStatusSummary {
    private String status;
    private long count;
    private BigDecimal totalAmount;

    public OrderStatusSummary() {}

    public OrderStatusSummary(String status, long count, BigDecimal totalAmount) {
      this.status = status;
      this.count = count;
      this.totalAmount = totalAmount;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public long getCount() {
      return count;
    }

    public void setCount(long count) {
      this.count = count;
    }

    public BigDecimal getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
    }
  }
}
