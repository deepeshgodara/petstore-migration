import React, { useState, useEffect } from 'react';
import { OrderDocument, OrderStatus, OrderSummaryResponse } from '../types/order';
import { orderService } from '../services/orderService';
import {
  Check,
  CheckCheck,
  X,
  Eye,
  RefreshCw,
  Clock,
  CheckCircle2,
  DollarSign,
  Package,
  Layers,
  Search,
  AlertTriangle,
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [orders, setOrders] = useState<OrderDocument[]>([]);
  const [summary, setSummary] = useState<OrderSummaryResponse | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<OrderStatus | 'ALL'>('ALL');
  const [searchFilter, setSearchFilter] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<OrderDocument | null>(null);
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      orderService.getOrders(selectedStatus === 'ALL' ? undefined : selectedStatus),
      orderService.getAdminSummary(),
    ])
      .then(([ordersData, summaryData]) => {
        if (isMounted) {
          setOrders(ordersData);
          setSummary(summaryData);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          console.error('Failed to load admin data', err);
          const msg = err instanceof Error ? err.message : 'Error loading admin data';
          setFeedbackMsg({ type: 'error', text: msg });
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [selectedStatus]);

  const handleStatusFilterChange = (st: OrderStatus | 'ALL') => {
    setLoading(true);
    setSelectedStatus(st);
  };

  const handleManualRefresh = () => {
    setLoading(true);
    Promise.all([
      orderService.getOrders(selectedStatus === 'ALL' ? undefined : selectedStatus),
      orderService.getAdminSummary(),
    ])
      .then(([ordersData, summaryData]) => {
        setOrders(ordersData);
        setSummary(summaryData);
      })
      .catch((err: unknown) => {
        console.error('Failed to refresh admin data', err);
      })
      .finally(() => setLoading(false));
  };

  const handleStatusUpdate = async (orderId: string, newStatus: OrderStatus) => {
    setUpdatingId(orderId);
    try {
      const updated = await orderService.updateOrderStatus(orderId, newStatus);
      // Optimistically update orders list
      setOrders((prev) =>
        prev.map((o) => (o.id === orderId ? { ...o, status: updated.status } : o))
      );
      // Reload summary to reflect updated counts
      const newSummary = await orderService.getAdminSummary();
      setSummary(newSummary);

      setFeedbackMsg({
        type: 'success',
        text: `Order #${orderId} successfully transitioned to ${newStatus}`,
      });
      setTimeout(() => setFeedbackMsg(null), 3500);
    } catch (err: unknown) {
      console.error('Failed to update status', err);
      const msg = err instanceof Error ? err.message : 'Failed to update order status';
      setFeedbackMsg({ type: 'error', text: msg });
    } finally {
      setUpdatingId(null);
    }
  };

  const filteredOrders = orders.filter((o) => {
    if (!searchFilter.trim()) return true;
    const term = searchFilter.toLowerCase().trim();
    const matchId = o.id.toLowerCase().includes(term);
    const matchUser = (o.userId || '').toLowerCase().includes(term);
    const matchName = (o.shipping?.name || '').toLowerCase().includes(term);
    return matchId || matchUser || matchName;
  });

  const pendingCount = summary?.statusBreakdown?.PENDING || 0;
  const approvedCount = summary?.statusBreakdown?.APPROVED || 0;
  const completedCount = summary?.statusBreakdown?.COMPLETED || 0;
  const totalCount = summary?.totalOrders || orders.length;

  const pendingPct = totalCount > 0 ? (pendingCount / totalCount) * 100 : 0;
  const approvedPct = totalCount > 0 ? (approvedCount / totalCount) * 100 : 0;
  const completedPct = totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

  return (
    <section>
      {/* Header Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.75rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Layers size={24} color="#6366f1" />
            <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Admin Order Management</h1>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.2rem' }}>
            Modern cloud-native administration replacing legacy Swing client (`petstoreadmin.ear`).
          </p>
        </div>

        <button
          type="button"
          className="btn-secondary"
          onClick={handleManualRefresh}
          disabled={loading}
          style={{ gap: '0.5rem' }}
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Alert Notification */}
      {feedbackMsg && (
        <div
          style={{
            padding: '0.85rem 1.25rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.5rem',
            background:
              feedbackMsg.type === 'success'
                ? 'rgba(16, 185, 129, 0.15)'
                : 'rgba(244, 63, 94, 0.15)',
            border: `1px solid ${
              feedbackMsg.type === 'success'
                ? 'rgba(16, 185, 129, 0.4)'
                : 'rgba(244, 63, 94, 0.4)'
            }`,
            color: feedbackMsg.type === 'success' ? '#6ee7b7' : '#fda4af',
            display: 'flex',
            alignItems: 'center',
            gap: '0.6rem',
            fontSize: '0.875rem',
          }}
        >
          {feedbackMsg.type === 'success' ? <Check size={16} /> : <AlertTriangle size={16} />}
          <span>{feedbackMsg.text}</span>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem',
          marginBottom: '1.75rem',
        }}
      >
        {/* Total Revenue */}
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(16, 185, 129, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <DollarSign size={22} color="#10b981" />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Total Revenue
            </span>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent-emerald)' }}>
              ${Number(summary?.totalRevenue || 0).toFixed(2)}
            </div>
          </div>
        </div>

        {/* Total Orders */}
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(99, 102, 241, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Package size={22} color="#818cf8" />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Total Orders
            </span>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc' }}>
              {summary?.totalOrders ?? orders.length}
            </div>
          </div>
        </div>

        {/* Pending Approvals */}
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(245, 158, 11, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Clock size={22} color="#f59e0b" />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Pending Approval
            </span>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent-amber)' }}>
              {pendingCount}
            </div>
          </div>
        </div>

        {/* Approved Orders */}
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(6, 182, 212, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CheckCircle2 size={22} color="#06b6d4" />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Approved / Active
            </span>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
              {approvedCount}
            </div>
          </div>
        </div>
      </div>

      {/* Visual Status Progress Bar */}
      <div
        style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '1.25rem',
          marginBottom: '1.75rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 700 }}>Order Lifecycle Distribution</span>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{totalCount} Total Transactions</span>
        </div>

        {/* Multi-segmented Progress Bar */}
        <div
          style={{
            height: '10px',
            borderRadius: 'var(--radius-full)',
            background: 'rgba(255,255,255,0.05)',
            display: 'flex',
            overflow: 'hidden',
            marginBottom: '0.85rem',
          }}
        >
          <div style={{ width: `${pendingPct}%`, background: 'var(--accent-amber)', transition: 'width 0.3s' }} title={`Pending: ${pendingCount}`} />
          <div style={{ width: `${approvedPct}%`, background: 'var(--accent-cyan)', transition: 'width 0.3s' }} title={`Approved: ${approvedCount}`} />
          <div style={{ width: `${completedPct}%`, background: 'var(--accent-emerald)', transition: 'width 0.3s' }} title={`Completed: ${completedCount}`} />
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1.5rem', fontSize: '0.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '2px', background: 'var(--accent-amber)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Pending: <strong>{pendingCount}</strong> ({pendingPct.toFixed(0)}%)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '2px', background: 'var(--accent-cyan)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Approved: <strong>{approvedCount}</strong> ({approvedPct.toFixed(0)}%)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '2px', background: 'var(--accent-emerald)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Completed: <strong>{completedCount}</strong> ({completedPct.toFixed(0)}%)</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1rem',
        }}
      >
        {/* Status Filter Tabs */}
        <div className="tab-nav">
          {(['ALL', 'PENDING', 'APPROVED', 'COMPLETED'] as const).map((st) => (
            <button
              key={st}
              type="button"
              className={`tab-btn ${selectedStatus === st ? 'active' : ''}`}
              onClick={() => handleStatusFilterChange(st)}
            >
              {st === 'ALL' ? 'All Orders' : st}
            </button>
          ))}
        </div>

        {/* Search within orders */}
        <div style={{ position: 'relative', width: '280px' }}>
          <Search size={15} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            className="search-input"
            style={{ borderRadius: 'var(--radius-sm)', paddingLeft: '2.2rem' }}
            placeholder="Search by Order ID, User..."
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
          />
        </div>
      </div>

      {/* Orders Table */}
      <div
        style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          overflow: 'hidden',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ background: 'rgba(15, 23, 42, 0.9)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                <th style={{ padding: '0.9rem 1.25rem' }}>Order ID</th>
                <th style={{ padding: '0.9rem 1rem' }}>Date & Time</th>
                <th style={{ padding: '0.9rem 1rem' }}>User / Customer</th>
                <th style={{ padding: '0.9rem 1rem' }}>Items</th>
                <th style={{ padding: '0.9rem 1rem' }}>Total Amount</th>
                <th style={{ padding: '0.9rem 1rem' }}>Status</th>
                <th style={{ padding: '0.9rem 1.25rem', textAlign: 'right' }}>Workflow Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    No orders match the selected status or search filter.
                  </td>
                </tr>
              ) : (
                filteredOrders.map((order) => {
                  const dateStr = order.orderDate
                    ? new Date(order.orderDate).toLocaleString()
                    : 'N/A';

                  const isPending = order.status === 'PENDING';
                  const isApproved = order.status === 'APPROVED';

                  const statusColors: Record<string, { bg: string; text: string; border: string }> = {
                    PENDING: { bg: 'rgba(245, 158, 11, 0.15)', text: '#fbbf24', border: 'rgba(245, 158, 11, 0.35)' },
                    APPROVED: { bg: 'rgba(6, 182, 212, 0.15)', text: '#38bdf8', border: 'rgba(6, 182, 212, 0.35)' },
                    COMPLETED: { bg: 'rgba(16, 185, 129, 0.15)', text: '#34d399', border: 'rgba(16, 185, 129, 0.35)' },
                    DENIED: { bg: 'rgba(244, 63, 94, 0.15)', text: '#fda4af', border: 'rgba(244, 63, 94, 0.35)' },
                    CANCELLED: { bg: 'rgba(148, 163, 184, 0.15)', text: '#94a3b8', border: 'rgba(148, 163, 184, 0.35)' },
                  };

                  const style = statusColors[order.status] || statusColors.PENDING;

                  return (
                    <tr
                      key={order.id}
                      style={{
                        borderBottom: '1px solid var(--border-subtle)',
                        transition: 'background 0.15s ease',
                      }}
                      onMouseEnter={(e) => {
                        (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.02)';
                      }}
                      onMouseLeave={(e) => {
                        (e.currentTarget as HTMLElement).style.background = 'transparent';
                      }}
                    >
                      <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', fontWeight: 700, color: '#a5b4fc' }}>
                        #{order.id}
                        {order.migratedFromLegacy && (
                          <span
                            style={{
                              marginLeft: '0.5rem',
                              fontSize: '0.65rem',
                              padding: '2px 5px',
                              borderRadius: '4px',
                              background: 'rgba(255,255,255,0.06)',
                              color: 'var(--text-muted)',
                            }}
                          >
                            Legacy
                          </span>
                        )}
                      </td>
                      <td style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                        {dateStr}
                      </td>
                      <td style={{ padding: '1rem' }}>
                        <div style={{ fontWeight: 600 }}>{order.shipping?.name || 'Customer'}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>@{order.userId}</div>
                      </td>
                      <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                        {order.lineItems?.length || 0} item{(order.lineItems?.length || 0) === 1 ? '' : 's'}
                      </td>
                      <td style={{ padding: '1rem', fontWeight: 700, color: 'var(--accent-emerald)' }}>
                        ${Number(order.totalPrice || 0).toFixed(2)}
                      </td>
                      <td style={{ padding: '1rem' }}>
                        <span
                          style={{
                            padding: '3px 8px',
                            borderRadius: 'var(--radius-full)',
                            fontSize: '0.725rem',
                            fontWeight: 700,
                            background: style.bg,
                            color: style.text,
                            border: `1px solid ${style.border}`,
                          }}
                        >
                          {order.status}
                        </span>
                      </td>
                      <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.4rem' }}>
                          <button
                            type="button"
                            className="btn-secondary"
                            onClick={() => setSelectedOrder(order)}
                            title="View Order Details"
                            style={{ padding: '0.35rem 0.6rem' }}
                          >
                            <Eye size={14} />
                          </button>

                          {isPending && (
                            <>
                              <button
                                type="button"
                                className="btn-primary"
                                onClick={() => handleStatusUpdate(order.id, 'APPROVED')}
                                disabled={updatingId === order.id}
                                style={{
                                  padding: '0.35rem 0.75rem',
                                  background: 'var(--accent-emerald)',
                                  fontSize: '0.75rem',
                                }}
                                title="Approve Order"
                              >
                                <Check size={14} />
                                <span>Approve</span>
                              </button>
                              <button
                                type="button"
                                className="btn-secondary"
                                onClick={() => handleStatusUpdate(order.id, 'DENIED')}
                                disabled={updatingId === order.id}
                                style={{
                                  padding: '0.35rem 0.6rem',
                                  color: 'var(--accent-rose)',
                                }}
                                title="Reject Order"
                              >
                                <X size={14} />
                              </button>
                            </>
                          )}

                          {isApproved && (
                            <button
                              type="button"
                              className="btn-primary"
                              onClick={() => handleStatusUpdate(order.id, 'COMPLETED')}
                              disabled={updatingId === order.id}
                              style={{
                                padding: '0.35rem 0.75rem',
                                background: 'var(--accent-cyan)',
                                fontSize: '0.75rem',
                              }}
                              title="Mark Completed"
                            >
                              <CheckCheck size={14} />
                              <span>Complete</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Details Modal */}
      {selectedOrder && (
        <div className="modal-overlay" onClick={() => setSelectedOrder(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '620px', padding: '2rem' }}
          >
            <button className="modal-close-btn" onClick={() => setSelectedOrder(null)} aria-label="Close">
              <X size={18} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Purchase Order Record
                </span>
                <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>#{selectedOrder.id}</h2>
              </div>
              <span className="brand-badge" style={{ fontSize: '0.8rem', padding: '4px 10px' }}>
                {selectedOrder.status}
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem', fontSize: '0.85rem' }}>
              <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.85rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
                <strong style={{ color: 'var(--accent-cyan)', display: 'block', marginBottom: '0.3rem' }}>Shipping Address</strong>
                <div>{selectedOrder.shipping?.name || 'N/A'}</div>
                <div>{selectedOrder.shipping?.address1}</div>
                <div>
                  {selectedOrder.shipping?.city} {selectedOrder.shipping?.state} {selectedOrder.shipping?.postalCode}
                </div>
                <div>{selectedOrder.shipping?.country}</div>
              </div>

              <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.85rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
                <strong style={{ color: 'var(--brand-primary)', display: 'block', marginBottom: '0.3rem' }}>Payment Details</strong>
                <div>Card: {selectedOrder.payment?.cardType || 'Credit Card'}</div>
                <div style={{ fontFamily: 'monospace' }}>{selectedOrder.payment?.cardNumberMasked}</div>
                <div>Expires: {selectedOrder.payment?.expiryDate || 'N/A'}</div>
                <div>Customer ID: {selectedOrder.userId}</div>
              </div>
            </div>

            {/* Line items table */}
            <h3 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '0.5rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
              Purchased Line Items
            </h3>
            <div style={{ background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)', marginBottom: '1.5rem' }}>
              {selectedOrder.lineItems && selectedOrder.lineItems.length > 0 ? (
                selectedOrder.lineItems.map((li, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '0.75rem 1rem',
                      borderBottom: idx === selectedOrder.lineItems.length - 1 ? 'none' : '1px solid var(--border-subtle)',
                      fontSize: '0.85rem',
                    }}
                  >
                    <div>
                      <strong style={{ color: '#a5b4fc' }}>{li.itemId}</strong>
                      <span style={{ marginLeft: '0.5rem', color: 'var(--text-muted)' }}>({li.productId})</span>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Quantity: {li.quantity} × ${Number(li.unitPrice).toFixed(2)}</div>
                    </div>
                    <div style={{ fontWeight: 700, color: 'var(--accent-emerald)' }}>
                      ${Number(li.totalCost).toFixed(2)}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  No itemized lines recorded for this migrated baseline order.
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '1rem', borderTop: '1px solid var(--border-subtle)' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Total Paid</span>
                <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--accent-emerald)' }}>
                  ${Number(selectedOrder.totalPrice || 0).toFixed(2)}
                </span>
              </div>
              <button type="button" className="btn-secondary" onClick={() => setSelectedOrder(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};
