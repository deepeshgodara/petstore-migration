import React from 'react';
import { Product, Item, Locale } from '../types/catalog';
import { Eye, Plus } from 'lucide-react';

interface ProductCardProps {
  product: Product;
  locale: Locale;
  onSelectProduct: (product: Product) => void;
  onAddToCart: (product: Product, item: Item) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  locale,
  onSelectProduct,
  onAddToCart,
}) => {
  // Find lowest price among SKU items
  const minPrice =
    product.items && product.items.length > 0
      ? Math.min(...product.items.map((i) => Number(i.listPrice) || 0))
      : 0;

  const defaultItem = product.items && product.items.length > 0 ? product.items[0] : null;

  const imgSrc = product.image ? `/images/${product.image}` : '/images/birds_icon.gif';

  const viewDetailsLabel =
    locale === 'ja_JP' ? '詳細を見る' : locale === 'zh_CN' ? '查看详情' : 'Quick View';

  const addLabel =
    locale === 'ja_JP' ? 'カートへ' : locale === 'zh_CN' ? '加入购物车' : 'Add to Cart';

  return (
    <article className="product-card">
      <div className="product-card-img-wrap" onClick={() => onSelectProduct(product)} style={{ cursor: 'pointer' }}>
        <span className="product-cat-tag">{product.categoryId}</span>
        <img
          src={imgSrc}
          alt={product.name}
          className="product-card-img"
          onError={(e) => {
            // Replace with fallback icon on error
            (e.target as HTMLImageElement).src = '/images/banner_logo.gif';
          }}
        />
      </div>

      <div className="product-card-body">
        <span className="product-id-label">{product.id}</span>
        <h3 className="product-title" onClick={() => onSelectProduct(product)} style={{ cursor: 'pointer' }}>
          {product.name}
        </h3>
        <p className="product-desc">{product.description || 'Premium pedigree pet bred with care and health guarantees.'}</p>

        <div className="product-card-footer">
          <div className="product-price-block">
            <span className="product-price-label">Starts at</span>
            <span className="product-price-value">${minPrice.toFixed(2)}</span>
          </div>

          <div style={{ display: 'flex', gap: '0.4rem' }}>
            <button
              className="btn-secondary"
              onClick={() => onSelectProduct(product)}
              title={viewDetailsLabel}
              aria-label={viewDetailsLabel}
            >
              <Eye size={15} />
            </button>
            {defaultItem && (
              <button
                className="btn-primary"
                onClick={() => onAddToCart(product, defaultItem)}
                title={addLabel}
              >
                <Plus size={15} />
                <span>Add</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </article>
  );
};
