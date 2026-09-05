import React, { useState } from 'react';
import { CartLineItem } from '../types/cart';
import { Locale } from '../types/catalog';
import { Address, CreateOrderRequest, OrderDocument, Payment } from '../types/order';
import { orderService } from '../services/orderService';
import { useAuth } from '../auth';
import { X, CheckCircle2, CreditCard, Truck, ShieldCheck, Loader2 } from 'lucide-react';

interface CheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  cartItems: CartLineItem[];
  locale: Locale;
  onOrderSuccess: (order: OrderDocument) => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({
  isOpen,
  onClose,
  cartItems,
  locale,
  onOrderSuccess,
}) => {
  const { user } = useAuth();
  const [shipping, setShipping] = useState<Address>(() => ({
    name: user?.name || 'Jane Doe',
    address1: '100 Market St',
    address2: 'Suite 400',
    city: 'San Francisco',
    state: 'CA',
    postalCode: '94105',
    country: 'USA',
    telephone: '415-555-0199',
    email: user?.email || 'jane.doe@example.com',
  }));

  const [prevUser, setPrevUser] = useState<typeof user>(user);
  if (user !== prevUser) {
    setPrevUser(user);
    if (user) {
      setShipping((prev) => ({
        ...prev,
        name: user.name,
        email: user.email,
      }));
    }
  }

  const [billingSameAsShipping, setBillingSameAsShipping] = useState<boolean>(true);
  const [billing, setBilling] = useState<Address>({
    name: 'Jane Doe',
    address1: '100 Market St',
    address2: 'Suite 400',
    city: 'San Francisco',
    state: 'CA',
    postalCode: '94105',
    country: 'USA',
    telephone: '415-555-0199',
    email: 'jane.doe@example.com',
  });

  const [payment, setPayment] = useState<Payment>({
    cardType: 'VISA',
    cardNumberMasked: '•••• •••• •••• 4242',
    expiryDate: '12/28',
  });

  const [rawCardNumber, setRawCardNumber] = useState<string>('4242 4242 4242 4242');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmedOrder, setConfirmedOrder] = useState<OrderDocument | null>(null);

  if (!isOpen) return null;

  const subtotal = cartItems.reduce(
    (sum, item) => sum + Number(item.item.listPrice) * item.quantity,
    0
  );
  const tax = subtotal * 0.05;
  const total = subtotal + tax;

  const handleCardNumberChange = (val: string) => {
    setRawCardNumber(val);
    const cleaned = val.replace(/\s+/g, '');
    const masked =
      cleaned.length >= 4
        ? `•••• •••• •••• ${cleaned.slice(-4)}`
        : '•••• •••• •••• 4242';
    setPayment((prev) => ({ ...prev, cardNumberMasked: masked }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const activeBilling = billingSameAsShipping ? shipping : billing;

    const lineItemsPayload = cartItems.map((ci, idx) => ({
      lineNumber: idx + 1,
      itemId: ci.item.itemId,
      productId: ci.product.id,
      categoryId: ci.product.categoryId,
      quantity: ci.quantity,
      unitPrice: Number(ci.item.listPrice),
      totalCost: Number(ci.item.listPrice) * ci.quantity,
    }));

    const orderPayload: CreateOrderRequest = {
      userId: user?.username || 'j2ee',
      locale,
      billing: activeBilling,
      shipping,
      payment,
      lineItems: lineItemsPayload,
    };

    try {
      const createdOrder = await orderService.placeOrder(orderPayload);
      setConfirmedOrder(createdOrder);
      onOrderSuccess(createdOrder);
      setLoading(false);
    } catch (err: unknown) {
      console.error('Order checkout failed', err);
      const errMsg = err instanceof Error ? err.message : 'Checkout failed';
      setError(errMsg);
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '680px', padding: '2rem' }}
      >
        <button className="modal-close-btn" onClick={onClose} aria-label="Close checkout">
          <X size={18} />
        </button>

        {confirmedOrder ? (
          /* Confirmation Success Screen */
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <div
              style={{
                width: '72px',
                height: '72px',
                borderRadius: 'var(--radius-full)',
                background: 'rgba(16, 185, 129, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem auto',
                border: '2px solid rgba(16, 185, 129, 0.4)',
              }}
            >
              <CheckCircle2 size={40} color="#10b981" />
            </div>

            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem' }}>
              Order Placed Successfully!
            </h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', fontSize: '0.95rem' }}>
              Thank you for adopting your new pet companion. Your order has been published to Kafka and synced to MongoDB.
            </p>

            <div
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '1.25rem',
                maxWidth: '440px',
                margin: '0 auto 2rem auto',
                textAlign: 'left',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.5rem',
                fontSize: '0.875rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Order ID</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 700, color: '#a5b4fc' }}>
                  #{confirmedOrder.id}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Status</span>
                <span className="brand-badge" style={{ borderColor: 'rgba(245, 158, 11, 0.4)', color: '#fbbf24' }}>
                  {confirmedOrder.status}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Total Paid</span>
                <span style={{ fontWeight: 800, color: 'var(--accent-emerald)', fontSize: '1.1rem' }}>
                  ${Number(confirmedOrder.totalPrice).toFixed(2)}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Customer</span>
                <span>{confirmedOrder.shipping?.name}</span>
              </div>
            </div>

            <button
              type="button"
              className="btn-primary"
              onClick={onClose}
              style={{ padding: '0.75rem 2rem', fontSize: '0.95rem' }}
            >
              Continue Shopping
            </button>
          </div>
        ) : (
          /* Checkout Input Form */
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.5rem' }}>
              <Truck size={24} color="#6366f1" />
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Complete Your Pet Adoption Checkout</h2>
            </div>

            {error && (
              <div
                style={{
                  background: 'rgba(244, 63, 94, 0.15)',
                  border: '1px solid rgba(244, 63, 94, 0.4)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0.75rem 1rem',
                  color: '#fda4af',
                  fontSize: '0.85rem',
                  marginBottom: '1.25rem',
                }}
              >
                {error}
              </div>
            )}

            {/* Shipping Details */}
            <div style={{ marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-cyan)', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                1. Shipping Address
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Full Name</label>
                  <input
                    type="text"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={shipping.name}
                    onChange={(e) => setShipping({ ...shipping, name: e.target.value })}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Email</label>
                  <input
                    type="email"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={shipping.email}
                    onChange={(e) => setShipping({ ...shipping, email: e.target.value })}
                  />
                </div>
                <div style={{ gridColumn: 'span 2' }}>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Address Line 1</label>
                  <input
                    type="text"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={shipping.address1}
                    onChange={(e) => setShipping({ ...shipping, address1: e.target.value })}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>City</label>
                  <input
                    type="text"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={shipping.city}
                    onChange={(e) => setShipping({ ...shipping, city: e.target.value })}
                  />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>State</label>
                    <input
                      type="text"
                      required
                      className="search-input"
                      style={{ borderRadius: 'var(--radius-sm)' }}
                      value={shipping.state}
                      onChange={(e) => setShipping({ ...shipping, state: e.target.value })}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>ZIP</label>
                    <input
                      type="text"
                      required
                      className="search-input"
                      style={{ borderRadius: 'var(--radius-sm)' }}
                      value={shipping.postalCode}
                      onChange={(e) => setShipping({ ...shipping, postalCode: e.target.value })}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Payment & Security */}
            <div style={{ marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--brand-primary)', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                2. Payment Details
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Card Number</label>
                  <div style={{ position: 'relative' }}>
                    <CreditCard size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                    <input
                      type="text"
                      required
                      className="search-input"
                      style={{ borderRadius: 'var(--radius-sm)', paddingLeft: '2.2rem' }}
                      value={rawCardNumber}
                      onChange={(e) => handleCardNumberChange(e.target.value)}
                    />
                  </div>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Card Type</label>
                    <select
                      className="search-input"
                      style={{ borderRadius: 'var(--radius-sm)', padding: '0.6rem 0.5rem' }}
                      value={payment.cardType}
                      onChange={(e) => setPayment({ ...payment, cardType: e.target.value })}
                    >
                      <option value="VISA">VISA</option>
                      <option value="MASTERCARD">MasterCard</option>
                      <option value="AMEX">Amex</option>
                      <option value="DISCOVER">Discover</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Expires</label>
                    <input
                      type="text"
                      required
                      placeholder="MM/YY"
                      className="search-input"
                      style={{ borderRadius: 'var(--radius-sm)' }}
                      value={payment.expiryDate}
                      onChange={(e) => setPayment({ ...payment, expiryDate: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem' }}>
                <input
                  type="checkbox"
                  id="billingSame"
                  checked={billingSameAsShipping}
                  onChange={(e) => setBillingSameAsShipping(e.target.checked)}
                  style={{ cursor: 'pointer' }}
                />
                <label htmlFor="billingSame" style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                  Billing address matches shipping address
                </label>
              </div>

              {!billingSameAsShipping && (
                <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Billing Full Name</label>
                  <input
                    type="text"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)', marginBottom: '0.5rem' }}
                    value={billing.name}
                    onChange={(e) => setBilling({ ...billing, name: e.target.value })}
                  />
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Billing Address Line 1</label>
                  <input
                    type="text"
                    required
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={billing.address1}
                    onChange={(e) => setBilling({ ...billing, address1: e.target.value })}
                  />
                </div>
              )}
            </div>

            {/* Total Due & Submit Button */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingTop: '1.25rem',
                borderTop: '1px solid var(--border-subtle)',
              }}
            >
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Total Due</span>
                <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--accent-emerald)' }}>
                  ${total.toFixed(2)}
                </span>
              </div>

              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={loading || cartItems.length === 0}
                  style={{ padding: '0.65rem 1.75rem', fontSize: '0.9rem' }}
                >
                  {loading ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      <span>Processing...</span>
                    </>
                  ) : (
                    <>
                      <ShieldCheck size={16} />
                      <span>Authorize & Place Order</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
