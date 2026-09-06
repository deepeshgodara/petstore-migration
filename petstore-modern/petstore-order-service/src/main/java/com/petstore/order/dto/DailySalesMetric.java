package com.petstore.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Time-series metric tracking daily revenue and order volume.
 *
 * @param date date string in ISO format (YYYY-MM-DD)
 * @param revenue total revenue on this date
 * @param orderCount number of orders placed on this date
 */
public record DailySalesMetric(
    String date,
    BigDecimal revenue,
    long orderCount
) implements Serializable {}
