/**
 * Type definitions for Pet Store Order Domain and Checkout flow.
 */

export interface Address {
  name: string;
  address1: string;
  address2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  telephone: string;
  email: string;
}

export interface Payment {
  cardType: string;
  cardNumberMasked: string;
  expiryDate: string;
}

export interface OrderLineItem {
  lineNumber: number;
  itemId: string;
  productId: string;
  categoryId: string;
  quantity: number;
  unitPrice: number;
  totalCost: number;
}

export interface CreateOrderRequest {
  userId: string;
  locale: string;
  billing: Address;
  shipping: Address;
  payment: Payment;
  lineItems: OrderLineItem[];
}

export type OrderStatus = 'PENDING' | 'APPROVED' | 'COMPLETED' | 'DENIED' | 'CANCELLED';

export interface OrderDocument {
  id: string;
  userId: string;
  orderDate: string;
  status: OrderStatus;
  totalPrice: number;
  locale: string;
  billing: Address;
  shipping: Address;
  payment: Payment;
  lineItems: OrderLineItem[];
  createdAt: string;
  updatedAt: string;
  migratedFromLegacy?: boolean;
}

export interface OrderSummaryResponse {
  totalOrders: number;
  totalRevenue: number;
  statusBreakdown: Record<string, number>;
}
