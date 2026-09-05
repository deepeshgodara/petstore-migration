import React, { useState, useEffect, useMemo } from 'react';
import { Category, Product, Item, Locale } from '../types/catalog';
import { catalogService } from '../services/catalogService';
import { HeroBanner } from './HeroBanner';
import { CategoryNav } from './CategoryNav';
import { ProductCard } from './ProductCard';
import { ProductDetailModal } from './ProductDetailModal';
import { AlertCircle, SearchX } from 'lucide-react';

interface CatalogViewProps {
  locale: Locale;
  searchQuery: string;
  onAddToCart: (product: Product, item: Item, quantity?: number) => void;
}

export const CatalogView: React.FC<CatalogViewProps> = ({
  locale,
  searchQuery,
  onAddToCart,
}) => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeModalProduct, setActiveModalProduct] = useState<Product | null>(null);

  // Fetch categories when locale changes
  useEffect(() => {
    let isMounted = true;
    catalogService
      .getCategories(locale)
      .then((data) => {
        if (isMounted) {
          setCategories(data);
        }
      })
      .catch((err) => {
        console.error('Failed to load categories', err);
      });

    return () => {
      isMounted = false;
    };
  }, [locale]);

  const handleSelectCategory = (categoryId: string) => {
    setLoading(true);
    setSelectedCategory(categoryId);
  };

  // Fetch products when selectedCategory or locale changes
  useEffect(() => {
    let isMounted = true;

    catalogService
      .getProducts(selectedCategory, undefined, locale)
      .then((data) => {
        if (isMounted) {
          setProducts(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('Failed to load products', err);
          setError(err.message || 'Failed to load catalog products');
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [selectedCategory, locale]);

  // Client-side filtering if search query is entered
  const filteredProducts = useMemo(() => {
    if (!searchQuery.trim()) {
      return products;
    }
    const q = searchQuery.toLowerCase().trim();
    return products.filter((p) => {
      const matchName = p.name.toLowerCase().includes(q);
      const matchDesc = (p.description || '').toLowerCase().includes(q);
      const matchId = p.id.toLowerCase().includes(q);
      const matchCat = p.categoryId.toLowerCase().includes(q);
      const matchItems =
        p.items &&
        p.items.some(
          (item) =>
            item.itemId.toLowerCase().includes(q) ||
            (item.attribute && item.attribute.toLowerCase().includes(q))
        );
      return matchName || matchDesc || matchId || matchCat || matchItems;
    });
  }, [products, searchQuery]);

  return (
    <section>
      {/* Hero Presentation */}
      <HeroBanner locale={locale} totalProducts={products.length} />

      {/* Category Filter Pills */}
      <CategoryNav
        categories={categories}
        selectedCategory={selectedCategory}
        onSelectCategory={handleSelectCategory}
        locale={locale}
      />

      {/* Catalog Status / Result Counter */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          margin: '1.25rem 0 0.5rem 0',
        }}
      >
        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          {loading ? (
            <span>Loading pets...</span>
          ) : (
            <span>
              Showing <strong style={{ color: 'var(--text-primary)' }}>{filteredProducts.length}</strong>{' '}
              {filteredProducts.length === 1 ? 'pet' : 'pets'}
              {selectedCategory !== 'ALL' && (
                <>
                  {' '}
                  in <span style={{ color: '#a5b4fc', fontWeight: 600 }}>{selectedCategory}</span>
                </>
              )}
              {searchQuery && (
                <>
                  {' '}
                  matching &ldquo;<span style={{ color: 'var(--text-primary)' }}>{searchQuery}</span>&rdquo;
                </>
              )}
            </span>
          )}
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '1rem',
            background: 'rgba(244, 63, 94, 0.1)',
            border: '1px solid rgba(244, 63, 94, 0.3)',
            borderRadius: 'var(--radius-md)',
            color: '#fda4af',
            margin: '1.5rem 0',
          }}
        >
          <AlertCircle size={20} />
          <span>Error loading catalog: {error}</span>
        </div>
      )}

      {/* Loading Skeletons */}
      {loading && (
        <div className="loading-skeleton-grid">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div key={idx} className="skeleton-card" />
          ))}
        </div>
      )}

      {/* Products Grid */}
      {!loading && !error && filteredProducts.length > 0 && (
        <div className="product-grid">
          {filteredProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              locale={locale}
              onSelectProduct={setActiveModalProduct}
              onAddToCart={(p, item) => onAddToCart(p, item, 1)}
            />
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && filteredProducts.length === 0 && (
        <div className="empty-state">
          <SearchX size={48} className="empty-state-icon" />
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
            No pets found
          </h3>
          <p style={{ maxWidth: '400px', margin: '0 auto', fontSize: '0.9rem' }}>
            We couldn&apos;t find any pets matching your criteria. Try adjusting your search query or selecting another category.
          </p>
        </div>
      )}

      {/* Product Detail Modal */}
      <ProductDetailModal
        product={activeModalProduct}
        locale={locale}
        onClose={() => setActiveModalProduct(null)}
        onAddToCart={onAddToCart}
      />
    </section>
  );
};
