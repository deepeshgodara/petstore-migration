package com.petstore.common.event;

/**
 * Lifecycle event types for order operations across the legacy bridge and modern services.
 */
public enum OrderEventType {
  ORDER_CREATED,
  ORDER_STATUS_UPDATED,
  ORDER_CANCELLED
}
