package com.petstore.order.dto;

import com.petstore.order.document.OrderStatus;
import java.io.Serializable;

/**
 * Request payload for administrative order status updates.
 *
 * @param status target lifecycle status (e.g., APPROVED, COMPLETED, DENIED, CANCELLED)
 */
public record UpdateOrderStatusRequest(
    OrderStatus status
) implements Serializable {}
