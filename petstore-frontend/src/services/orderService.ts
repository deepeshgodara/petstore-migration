import { CreateOrderRequest, OrderDocument, OrderStatus, OrderSummaryResponse } from '../types/order';

/**
 * Service providing typed HTTP methods for the Pet Store Order API.
 */
class OrderApiService {
  private readonly baseUrl = '/api/v1/orders';

  /**
   * Submits a customer purchase order.
   */
  async placeOrder(request: CreateOrderRequest): Promise<OrderDocument> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Order placement failed: ${response.status} - ${errorText || response.statusText}`);
    }

    return response.json();
  }

  /**
   * Retrieves an order by ID.
   */
  async getOrderById(orderId: string): Promise<OrderDocument> {
    const response = await fetch(`${this.baseUrl}/${encodeURIComponent(orderId)}`);
    if (!response.ok) {
      throw new Error(`Order not found: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Retrieves paginated orders optionally filtered by status.
   */
  async getOrders(status?: OrderStatus, page: number = 0, size: number = 20): Promise<{
    content: OrderDocument[];
    totalElements: number;
    totalPages: number;
  }> {
    const params = new URLSearchParams();
    if (status) {
      params.set('status', status);
    }
    params.set('page', String(page));
    params.set('size', String(size));

    const response = await fetch(`${this.baseUrl}?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Failed to load orders: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Updates an order's lifecycle status (e.g. PENDING -> APPROVED).
   */
  async updateOrderStatus(orderId: string, status: OrderStatus): Promise<OrderDocument> {
    const response = await fetch(`${this.baseUrl}/${encodeURIComponent(orderId)}/status`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ status }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to update order status: ${response.status} - ${errorText}`);
    }

    return response.json();
  }

  /**
   * Retrieves administrative revenue and status metrics.
   */
  async getAdminSummary(): Promise<OrderSummaryResponse> {
    const response = await fetch(`${this.baseUrl}/admin/summary`);
    if (!response.ok) {
      throw new Error(`Failed to load admin summary: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }
}

export const orderService = new OrderApiService();
