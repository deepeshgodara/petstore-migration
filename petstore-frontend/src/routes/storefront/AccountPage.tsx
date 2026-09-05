import React, { useState, useEffect } from 'react';
import { useAuth } from '../../auth';
import { OrderDocument } from '../../types/order';
import { orderService } from '../../services/orderService';
import { User, Package, Calendar, DollarSign, Clock, ArrowLeft, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export const AccountPage: React.FC = () => {
  const { user } = useAuth();
  const [orders, setOrders] = useState<OrderDocument[]>([]);
  const [loading, setLoading] = useState<boolean>(!!user);

  useEffect(() => {
    let isMounted = true;
    if (!user) return;

    orderService
      .getOrders(undefined, user.username)
      .then((data) => {
        if (isMounted) {
          setOrders(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        console.error('Failed to load user orders', err);
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [user]);

  if (!user) {
    return (
      <div className="empty-state" style={{ margin: '3rem auto', maxWidth: '500px' }}>
        <User size={48} className="empty-state-icon" />
        <h2>Sign In to View Account</h2>
        <p>Please log in with your credentials to access your customer profile and order history.</p>
        <Link to="/" className="btn-primary" style={{ marginTop: '1.25rem', textDecoration: 'none' }}>
          Return to Storefront
        </Link>
      </div>
    );
  }

  return (
    <section>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/" className="btn-secondary" style={{ padding: '0.45rem 0.75rem', textDecoration: 'none' }}>
            <ArrowLeft size={16} />
            <span>Storefront</span>
          </Link>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Customer Account & Orders</h1>
        </div>
      </div>

      {/* User Profile Card */}
      <div
        style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          padding: '1.75rem',
          marginBottom: '2rem',
          display: 'grid',
          gridTemplateColumns: 'auto 1fr auto',
          gap: '1.5rem',
          alignItems: 'center',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div
          style={{
            width: '64px',
            height: '64px',
            borderRadius: 'var(--radius-full)',
            background: 'var(--grad-primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.5rem',
            fontWeight: 800,
            color: '#fff',
            boxShadow: 'var(--shadow-glow)',
          }}
        >
          {user.name.charAt(0).toUpperCase()}
        </div>

        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 700 }}>{user.name}</h2>
            <span className="brand-badge">{user.role}</span>
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
            @{user.username} • {user.email}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', color: '#10b981', marginTop: '0.4rem' }}>
            <ShieldCheck size={14} />
            <span>Authenticated Customer Session Active</span>
          </div>
        </div>

        <div style={{ textAlign: 'right' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', display: 'block' }}>
            Total Orders Placed
          </span>
          <span style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
            {orders.length}
          </span>
        </div>
      </div>

      {/* Order History Section */}
      <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ fontSize: '1.2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Package size={20} color="#6366f1" />
          <span>Purchase Order History</span>
        </h3>
        <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
          {orders.length} {orders.length === 1 ? 'order' : 'orders'} recorded
        </span>
      </div>

      {loading ? (
        <div className="empty-state">
          <Clock size={36} className="empty-state-icon animate-spin" />
          <p>Loading your orders...</p>
        </div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <Package size={48} className="empty-state-icon" />
          <h3>No Orders Found</h3>
          <p style={{ maxWidth: '400px', margin: '0.5rem auto 1.5rem auto' }}>
            You haven&apos;t adopted any pets yet! Explore our catalog to place your first order.
          </p>
          <Link to="/" className="btn-primary" style={{ textDecoration: 'none' }}>
            Browse Storefront
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {orders.map((order) => {
            const dateFormatted = order.orderDate
              ? new Date(order.orderDate).toLocaleDateString(undefined, {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })
              : 'Recent';

            const statusColors: Record<string, { bg: string; text: string; border: string }> = {
              PENDING: { bg: 'rgba(245, 158, 11, 0.15)', text: '#fbbf24', border: 'rgba(245, 158, 11, 0.35)' },
              APPROVED: { bg: 'rgba(6, 182, 212, 0.15)', text: '#38bdf8', border: 'rgba(6, 182, 212, 0.35)' },
              COMPLETED: { bg: 'rgba(16, 185, 129, 0.15)', text: '#34d399', border: 'rgba(16, 185, 129, 0.35)' },
            };

            const stStyle = statusColors[order.status] || statusColors.PENDING;

            return (
              <div
                key={order.id}
                style={{
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1.25rem 1.5rem',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '1rem', color: '#a5b4fc' }}>
                      #{order.id}
                    </span>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: 'var(--radius-full)',
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        background: stStyle.bg,
                        color: stStyle.text,
                        border: `1px solid ${stStyle.border}`,
                      }}
                    >
                      {order.status}
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.85rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'var(--text-muted)' }}>
                      <Calendar size={14} />
                      <span>{dateFormatted}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontWeight: 800, color: 'var(--accent-emerald)', fontSize: '1.1rem' }}>
                      <DollarSign size={16} />
                      <span>{Number(order.totalPrice || 0).toFixed(2)}</span>
                    </div>
                  </div>
                </div>

                {/* Line Items List */}
                <div style={{ background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)', padding: '0.75rem', border: '1px solid var(--border-subtle)' }}>
                  {order.lineItems && order.lineItems.length > 0 ? (
                    order.lineItems.map((li, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          fontSize: '0.825rem',
                          padding: '0.25rem 0',
                        }}
                      >
                        <span style={{ color: 'var(--text-secondary)' }}>
                          • {li.itemId} ({li.productId}) × {li.quantity}
                        </span>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                          ${Number(li.totalCost).toFixed(2)}
                        </span>
                      </div>
                    ))
                  ) : (
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      Migrated baseline transaction
                    </div>
                  )}
                </div>

                {/* Shipping summary footer */}
                <div style={{ marginTop: '0.75rem', fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between' }}>
                  <span>Ship To: {order.shipping?.name || user.name}, {order.shipping?.city || 'Austin'}, {order.shipping?.country || 'USA'}</span>
                  <span style={{ fontFamily: 'monospace' }}>Payment: {order.payment?.cardNumberMasked || '•••• •••• •••• 4242'}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
};
