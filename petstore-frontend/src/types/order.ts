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
  productName?: string;
  itemAttribute?: string;
  image?: string;
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

export interface CategorySalesMetric {
  categoryId: string;
  categoryName: string;
  totalRevenue: number;
  unitsSold: number;
  orderCount: number;
  percentageShare: number;
}

export interface DailySalesMetric {
  date: string;
  revenue: number;
  orderCount: number;
}

export interface AdminAnalyticsResponse {
  totalOrders: number;
  totalRevenue: number;
  uniqueCustomers: number;
  returningCustomers: number;
  repeatCustomerRate: number;
  averageOrderValue: number;
  salesByCategory: CategorySalesMetric[];
  salesOverTime: DailySalesMetric[];
  statusBreakdown: Record<string, number>;
  startDate: string | null;
  endDate: string | null;
}

