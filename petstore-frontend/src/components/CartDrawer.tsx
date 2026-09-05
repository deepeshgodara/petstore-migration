import React from 'react';
import { CartLineItem } from '../types/cart';
import { Locale } from '../types/catalog';
import { X, Trash2, ShoppingBag, ArrowRight, Plus, Minus } from 'lucide-react';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cartItems: CartLineItem[];
  onUpdateQuantity: (lineId: string, quantity: number) => void;
  onRemoveItem: (lineId: string) => void;
  onClearCart: () => void;
  onProceedToCheckout: () => void;
  locale: Locale;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  cartItems,
  onUpdateQuantity,
  onRemoveItem,
  onClearCart,
  onProceedToCheckout,
  locale,
}) => {
  if (!isOpen) return null;

  const subtotal = cartItems.reduce(
    (sum, item) => sum + Number(item.item.listPrice) * item.quantity,
    0
  );
  const tax = subtotal * 0.05; // 5% estimated tax
  const total = subtotal + tax;

  const titles = {
    en_US: {
      title: 'Your Shopping Cart',
      emptyTitle: 'Your Cart is Empty',
      emptyDesc: 'Explore our pedigree dogs, playful cats, exotic birds, and aquatic life to add pets to your cart.',
      explore: 'Browse Catalog',
      subtotal: 'Subtotal',
      tax: 'Estimated Tax (5%)',
      shipping: 'Shipping',
      free: 'FREE',
      total: 'Total Due',
      checkout: 'Proceed to Checkout',
      clear: 'Clear Cart',
    },
    ja_JP: {
      title: 'ショッピングカート',
      emptyTitle: 'カートは空です',
      emptyDesc: 'カタログからお気に入りのペットを選んでカートに追加してください。',
      explore: 'カタログを見る',
      subtotal: '小計',
      tax: '推定消費税 (5%)',
      shipping: '送料',
      free: '無料',
      total: '合計金額',
      checkout: '注文手続きへ',
      clear: 'カートを空にする',
    },
    zh_CN: {
      title: '我的购物车',
      emptyTitle: '购物车暂无商品',
      emptyDesc: '浏览我们为您精选的各种优质宠物并加入购物车。',
      explore: '浏览宠物商城',
      subtotal: '小计',
      tax: '预估税费 (5%)',
      shipping: '配送费',
      free: '免运费',
      total: '应付总额',
      checkout: '立即结算',
      clear: '清空购物车',
    },
  }[locale] || {
    title: 'Your Shopping Cart',
    emptyTitle: 'Your Cart is Empty',
    emptyDesc: 'Explore our pedigree dogs, playful cats, exotic birds, and aquatic life to add pets to your cart.',
    explore: 'Browse Catalog',
    subtotal: 'Subtotal',
    tax: 'Estimated Tax (5%)',
    shipping: 'Shipping',
    free: 'FREE',
    total: 'Total Due',
    checkout: 'Proceed to Checkout',
    clear: 'Clear Cart',
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{
          maxWidth: '480px',
          height: '100%',
          maxHeight: '100vh',
          borderRadius: 0,
          marginRight: 0,
          marginLeft: 'auto',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '-10px 0 30px rgba(0,0,0,0.7)',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <ShoppingBag size={20} color="#6366f1" />
            <h2 style={{ fontSize: '1.2rem', fontWeight: 700 }}>{titles.title}</h2>
            <span className="brand-badge">{cartItems.reduce((a, c) => a + c.quantity, 0)} items</span>
          </div>
          <button className="modal-close-btn" onClick={onClose} style={{ position: 'static' }}>
            <X size={18} />
          </button>
        </div>

        {/* Body / Line items */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '1.25rem 1.5rem' }}>
          {cartItems.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
              <div
                style={{
                  width: '64px',
                  height: '64px',
                  borderRadius: 'var(--radius-full)',
                  background: 'rgba(99, 102, 241, 0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 1rem auto',
                }}
              >
                <ShoppingBag size={32} color="#6366f1" />
              </div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem' }}>{titles.emptyTitle}</h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                {titles.emptyDesc}
              </p>
              <button className="btn-primary" onClick={onClose}>
                {titles.explore}
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {cartItems.map((cartItem) => {
                const img = cartItem.item.image
                  ? `/images/${cartItem.item.image}`
                  : cartItem.product.image
                  ? `/images/${cartItem.product.image}`
                  : '/images/birds_icon.gif';

                const lineTotal = Number(cartItem.item.listPrice) * cartItem.quantity;

                return (
                  <div
                    key={cartItem.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.85rem',
                      background: 'rgba(255, 255, 255, 0.02)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-md)',
                      padding: '0.85rem',
                    }}
                  >
                    <img
                      src={img}
                      alt={cartItem.product.name}
                      style={{
                        width: '56px',
                        height: '56px',
                        objectFit: 'contain',
                        borderRadius: 'var(--radius-sm)',
                        background: 'rgba(15, 23, 42, 0.6)',
                        padding: '4px',
                      }}
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = '/images/banner_logo.gif';
                      }}
                    />

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <h4
                        style={{
                          fontSize: '0.9rem',
                          fontWeight: 600,
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}
                      >
                        {cartItem.product.name}
                      </h4>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                        {cartItem.item.attribute || cartItem.item.itemId} • ${Number(cartItem.item.listPrice).toFixed(2)}
                      </div>

                      {/* Quantity Controls */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            border: '1px solid var(--border-subtle)',
                            borderRadius: 'var(--radius-sm)',
                            background: 'rgba(255, 255, 255, 0.04)',
                          }}
                        >
                          <button
                            type="button"
                            onClick={() => onUpdateQuantity(cartItem.id, Math.max(1, cartItem.quantity - 1))}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--text-primary)',
                              padding: '2px 6px',
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                            }}
                          >
                            <Minus size={12} />
                          </button>
                          <span
                            style={{
                              padding: '0 6px',
                              fontSize: '0.8rem',
                              fontWeight: 700,
                              minWidth: '20px',
                              textAlign: 'center',
                            }}
                          >
                            {cartItem.quantity}
                          </span>
                          <button
                            type="button"
                            onClick={() => onUpdateQuantity(cartItem.id, cartItem.quantity + 1)}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--text-primary)',
                              padding: '2px 6px',
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                            }}
                          >
                            <Plus size={12} />
                          </button>
                        </div>

                        <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--accent-emerald)', marginLeft: 'auto' }}>
                          ${lineTotal.toFixed(2)}
                        </span>

                        <button
                          type="button"
                          onClick={() => onRemoveItem(cartItem.id)}
                          style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--text-muted)',
                            cursor: 'pointer',
                            padding: '4px',
                            display: 'flex',
                            alignItems: 'center',
                          }}
                          title="Remove item"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer with totals & checkout */}
        {cartItems.length > 0 && (
          <div
            style={{
              padding: '1.25rem 1.5rem',
              borderTop: '1px solid var(--border-subtle)',
              background: 'rgba(8, 12, 20, 0.6)',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', marginBottom: '1rem', fontSize: '0.85rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>{titles.subtotal}</span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>${subtotal.toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>{titles.tax}</span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>${tax.toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>{titles.shipping}</span>
                <span style={{ color: 'var(--accent-emerald)', fontWeight: 700 }}>{titles.free}</span>
              </div>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingTop: '0.5rem',
                  borderTop: '1px solid var(--border-subtle)',
                  fontSize: '1.1rem',
                  fontWeight: 800,
                }}
              >
                <span>{titles.total}</span>
                <span style={{ color: 'var(--accent-emerald)' }}>${total.toFixed(2)}</span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn-secondary"
                onClick={onClearCart}
                style={{ padding: '0.65rem', flex: '0 0 auto' }}
                title={titles.clear}
              >
                <Trash2 size={16} />
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={onProceedToCheckout}
                style={{ flex: 1, justifyContent: 'center', padding: '0.75rem 1rem', fontSize: '0.95rem' }}
              >
                <span>{titles.checkout}</span>
                <ArrowRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
