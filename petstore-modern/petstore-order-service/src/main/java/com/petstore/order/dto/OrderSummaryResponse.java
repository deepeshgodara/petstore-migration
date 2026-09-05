package com.petstore.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Analytical summary of orders for administrative visibility.
 *
 * @param totalOrders total count of orders
 * @param totalRevenue total monetary revenue from approved/completed orders
 * @param statusBreakdown counts per lifecycle status
 */
public record OrderSummaryResponse(
    long totalOrders,
    BigDecimal totalRevenue,
    Map<String, Long> statusBreakdown
) implements Serializable {}
