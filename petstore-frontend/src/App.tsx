import React, { useState, useEffect } from 'react';
import { Navbar, NavTab } from './components/Navbar';
import { CatalogView } from './components/CatalogView';
import { Locale, Product, Item } from './types/catalog';
import { CartLineItem } from './types/cart';
import { Layers, Activity } from 'lucide-react';
import './App.css';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('storefront');
  const [locale, setLocale] = useState<Locale>('en_US');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [cartItems, setCartItems] = useState<CartLineItem[]>(() => {
    try {
      const saved = localStorage.getItem('petstore_cart');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  // Persist cart to localStorage
  useEffect(() => {
    try {
      localStorage.setItem('petstore_cart', JSON.stringify(cartItems));
    } catch (e) {
      console.error('Failed to save cart to localStorage', e);
    }
  }, [cartItems]);

  const totalCartCount = cartItems.reduce((acc, curr) => acc + curr.quantity, 0);

  const handleAddToCart = (product: Product, item: Item, quantity: number = 1) => {
    setCartItems((prev) => {
      const lineId = `${product.id}_${item.itemId}`;
      const existingIdx = prev.findIndex((ci) => ci.id === lineId);
      if (existingIdx >= 0) {
        const updated = [...prev];
        updated[existingIdx] = {
          ...updated[existingIdx],
          quantity: updated[existingIdx].quantity + quantity,
        };
        return updated;
      }
      return [...prev, { id: lineId, product, item, quantity }];
    });
  };

  return (
    <div className="app-container">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        locale={locale}
        setLocale={setLocale}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        cartCount={totalCartCount}
        onOpenCart={() => {
          // Open cart modal (implemented in Task 6.3)
          console.log('Open Cart triggered');
        }}
      />

      <main className="main-content">
        {activeTab === 'storefront' && (
          <CatalogView
            locale={locale}
            searchQuery={searchQuery}
            onAddToCart={handleAddToCart}
          />
        )}

        {activeTab === 'admin' && (
          <div className="empty-state">
            <Layers size={48} className="empty-state-icon" />
            <h2 style={{ fontSize: '1.5rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
              Pet Store Modern Admin Dashboard
            </h2>
            <p style={{ maxWidth: '500px', margin: '0 auto', fontSize: '0.95rem' }}>
              Pending orders list, approval workflows, and revenue summary metrics are scheduled for implementation in Task 6.4.
            </p>
          </div>
        )}

        {activeTab === 'migration' && (
          <div className="empty-state">
            <Activity size={48} className="empty-state-icon" />
            <h2 style={{ fontSize: '1.5rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
              Visual Migration Parity Monitor
            </h2>
            <p style={{ maxWidth: '500px', margin: '0 auto', fontSize: '0.95rem' }}>
              Real-time reconciliation stats, audit drift tracking, and cutover readiness indicators are scheduled for implementation in Task 6.5.
            </p>
          </div>
        )}
      </main>
    </div>
  );
};

export default App;
