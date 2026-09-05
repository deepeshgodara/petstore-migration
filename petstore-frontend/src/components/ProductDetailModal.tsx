import React, { useState } from 'react';
import { Product, Item, Locale } from '../types/catalog';
import { X, Check, ShoppingBag } from 'lucide-react';

interface ProductDetailModalProps {
  product: Product | null;
  locale: Locale;
  onClose: () => void;
  onAddToCart: (product: Product, item: Item, quantity: number) => void;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({
  product,
  locale,
  onClose,
  onAddToCart,
}) => {
  const [selectedItemId, setSelectedItemId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(1);
  const [addedAnimation, setAddedAnimation] = useState<boolean>(false);

  if (!product) return null;

  const labels = {
    en_US: {
      selectVariant: 'Select Breed Variant / SKU',
      unitPrice: 'Unit Price',
      status: 'Inventory Status',
      inStock: 'in stock',
      addToCart: 'Add to Cart',
      added: 'Added to Cart!',
      verified: 'Verified Health',
    },
    ja_JP: {
      selectVariant: '品種バリエーション / SKU を選択',
      unitPrice: '単価',
      status: '在庫状況',
      inStock: '点在庫あり',
      addToCart: 'カートに追加',
      added: 'カートに追加しました！',
      verified: '健康確認済み',
    },
    zh_CN: {
      selectVariant: '选择品种规格 / SKU',
      unitPrice: '单价',
      status: '库存状态',
      inStock: '件库存',
      addToCart: '加入购物车',
      added: '已加入购物车！',
      verified: '健康体检认证',
    },
  }[locale] || {
    selectVariant: 'Select Breed Variant / SKU',
    unitPrice: 'Unit Price',
    status: 'Inventory Status',
    inStock: 'in stock',
    addToCart: 'Add to Cart',
    added: 'Added to Cart!',
    verified: 'Verified Health',
  };

  const currentItemId =
    selectedItemId || (product.items && product.items.length > 0 ? product.items[0].itemId : '');

  const selectedItem: Item | undefined = product.items.find(
    (item) => item.itemId === currentItemId
  ) || product.items[0];

  const handleAdd = () => {
    if (selectedItem) {
      onAddToCart(product, selectedItem, quantity);
      setAddedAnimation(true);
      setTimeout(() => {
        setAddedAnimation(false);
        onClose();
      }, 750);
    }
  };

  const itemImage = selectedItem?.image
    ? `/images/${selectedItem.image}`
    : product.image
    ? `/images/${product.image}`
    : '/images/birds_icon.gif';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
          <X size={18} />
        </button>

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(220px, 1fr) 1.2fr', gap: '1.5rem', padding: '1.75rem' }}>
          {/* Left Column: Image */}
          <div
            style={{
              background: 'linear-gradient(180deg, rgba(15, 23, 42, 0.6) 0%, rgba(30, 41, 59, 0.8) 100%)',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '1.5rem',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <img
              src={itemImage}
              alt={product.name}
              style={{
                maxWidth: '180px',
                maxHeight: '180px',
                objectFit: 'contain',
                filter: 'drop-shadow(0 12px 20px rgba(0,0,0,0.5))',
              }}
              onError={(e) => {
                (e.target as HTMLImageElement).src = '/images/banner_logo.gif';
              }}
            />
            <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
              <span className="brand-badge">{product.categoryId}</span>
              <span className="brand-badge" style={{ borderColor: 'rgba(16, 185, 129, 0.4)', color: '#6ee7b7' }}>
                Verified Health
              </span>
            </div>
          </div>

          {/* Right Column: Details & SKU Picker */}
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: 'var(--text-muted)' }}>
              SKU ID: {selectedItem?.itemId || product.id}
            </span>
            <h2 style={{ fontSize: '1.6rem', fontWeight: 800, marginTop: '0.2rem', marginBottom: '0.5rem' }}>
              {product.name}
            </h2>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1.25rem' }}>
              {product.description || 'Health checked, certified pedigree, ready for adoption.'}
            </p>

            {/* Variant / Item Selection */}
            {product.items && product.items.length > 0 && (
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase' }}>
                  {labels.selectVariant}
                </label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                  {product.items.map((item) => {
                    const isSelected = item.itemId === selectedItemId;
                    return (
                      <button
                        key={item.itemId}
                        type="button"
                        onClick={() => setSelectedItemId(item.itemId)}
                        style={{
                          background: isSelected ? 'var(--brand-primary)' : 'rgba(255,255,255,0.05)',
                          border: isSelected ? '1px solid transparent' : '1px solid var(--border-subtle)',
                          borderRadius: 'var(--radius-sm)',
                          padding: '0.45rem 0.85rem',
                          color: isSelected ? '#fff' : 'var(--text-secondary)',
                          fontSize: '0.825rem',
                          fontWeight: 600,
                          cursor: 'pointer',
                          transition: 'all 0.2s ease',
                        }}
                      >
                        {item.attribute || item.itemId} - ${Number(item.listPrice).toFixed(2)}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Price & Inventory */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', padding: '0.75rem 1rem', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
              <div>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>{labels.unitPrice}</span>
                <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent-emerald)' }}>
                  ${Number(selectedItem?.listPrice || 0).toFixed(2)}
                </span>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>{labels.status}</span>
                <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#38bdf8' }}>
                  {selectedItem?.inventoryQuantity ?? 10000} {labels.inStock}
                </span>
              </div>
            </div>

            {/* Quantity and Actions */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: 'auto' }}>
              <div style={{ display: 'flex', alignItems: 'center', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-md)', background: 'rgba(255,255,255,0.04)' }}>
                <button
                  type="button"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  style={{ background: 'none', border: 'none', color: 'var(--text-primary)', padding: '0.5rem 0.8rem', cursor: 'pointer', fontSize: '1rem', fontWeight: 700 }}
                >
                  -
                </button>
                <span style={{ padding: '0 0.5rem', fontWeight: 700, minWidth: '24px', textAlign: 'center' }}>
                  {quantity}
                </span>
                <button
                  type="button"
                  onClick={() => setQuantity(quantity + 1)}
                  style={{ background: 'none', border: 'none', color: 'var(--text-primary)', padding: '0.5rem 0.8rem', cursor: 'pointer', fontSize: '1rem', fontWeight: 700 }}
                >
                  +
                </button>
              </div>

              <button
                type="button"
                className="btn-primary"
                onClick={handleAdd}
                style={{ flex: 1, padding: '0.65rem 1.25rem', justifyContent: 'center', fontSize: '0.9rem' }}
              >
                {addedAnimation ? (
                  <>
                    <Check size={18} />
                    <span>{labels.added}</span>
                  </>
                ) : (
                  <>
                    <ShoppingBag size={18} />
                    <span>{labels.addToCart} (${((selectedItem ? Number(selectedItem.listPrice) : 0) * quantity).toFixed(2)})</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
