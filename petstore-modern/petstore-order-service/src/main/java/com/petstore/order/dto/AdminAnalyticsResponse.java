package com.petstore.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive analytical report for store administrators, modernizing the
 * legacy Java Swing AdminApp sales reporting charts.
 *
 * @param totalOrders total count of matching orders
 * @param totalRevenue total revenue across matching orders
 * @param uniqueCustomers number of unique customer accounts
 * @param returningCustomers number of customers who have placed 2 or more orders
 * @param repeatCustomerRate percentage of customers who are returning
 * @param averageOrderValue average order value (totalRevenue / totalOrders)
 * @param salesByCategory sales metrics breakdown per category for pie/bar charts
 * @param salesOverTime daily time-series sales trend metrics
 * @param statusBreakdown order counts per lifecycle status
 * @param startDate filtered start date (if specified)
 * @param endDate filtered end date (if specified)
 */
public record AdminAnalyticsResponse(
    long totalOrders,
    BigDecimal totalRevenue,
    long uniqueCustomers,
    long returningCustomers,
    double repeatCustomerRate,
    BigDecimal averageOrderValue,
    List<CategorySalesMetric> salesByCategory,
    List<DailySalesMetric> salesOverTime,
    Map<String, Long> statusBreakdown,
    String startDate,
    String endDate
) implements Serializable {}
