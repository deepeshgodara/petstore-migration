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
   * @return revenue summary projection
   */
  @Aggregation(pipeline = {
      "{ '$match': { 'status': { '$in': ['APPROVED', 'COMPLETED'] } } }",
      "{ '$group': { '_id': null, 'totalRevenue': { '$sum': '$totalPrice' }, 'orderCount': { '$sum': 1 } } }"
  })
  RevenueSummary calculateRevenueSummary();

  /**
   * Aggregation pipeline to group order counts by status.
   *
   * @return list of status breakdown projections
   */
  @Aggregation(pipeline = {
      "{ '$group': { '_id': '$status', 'count': { '$sum': 1 }, 'totalAmount': { '$sum': '$totalPrice' } } }",
      "{ '$project': { 'status': '$_id', 'count': 1, 'totalAmount': 1, '_id': 0 } }"
  })
  List<OrderStatusSummary> getOrderStatusBreakdown();

  /**
   * Projection interface for revenue calculation.
   */
  interface RevenueSummary {
    BigDecimal getTotalRevenue();
    long getOrderCount();
  }

  /**
   * Projection interface for order status breakdown.
   */
  interface OrderStatusSummary {
    String getStatus();
    long getCount();
    BigDecimal getTotalAmount();
  }
}
