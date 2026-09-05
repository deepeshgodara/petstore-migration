import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { LoginModal } from './components/auth/LoginModal';
import { StorefrontLayout } from './routes/storefront/StorefrontLayout';
import { CatalogView } from './components/CatalogView';
import { AccountPage } from './routes/storefront/AccountPage';
import { AdminLayout } from './routes/admin/AdminLayout';
import { AdminOrdersPage } from './routes/admin/AdminOrdersPage';
import { OpsLayout } from './routes/ops/OpsLayout';
import { ParityMonitorPage } from './routes/ops/ParityMonitorPage';
import { CartDrawer } from './components/CartDrawer';
import { CheckoutModal } from './components/CheckoutModal';
import { Locale, Product, Item } from './types/catalog';
import { CartLineItem } from './types/cart';
import { OrderDocument } from './types/order';
import './App.css';

export const AppContent: React.FC = () => {
  const [locale, setLocale] = useState<Locale>('en_US');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isCartOpen, setIsCartOpen] = useState<boolean>(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState<boolean>(false);

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

  const handleUpdateQuantity = (lineId: string, quantity: number) => {
    setCartItems((prev) =>
      prev.map((item) => (item.id === lineId ? { ...item, quantity } : item))
    );
  };

  const handleRemoveItem = (lineId: string) => {
    setCartItems((prev) => prev.filter((item) => item.id !== lineId));
  };

  const handleClearCart = () => {
    setCartItems([]);
  };

  const handleProceedToCheckout = () => {
    setIsCartOpen(false);
    setIsCheckoutOpen(true);
  };

  const handleOrderSuccess = (order: OrderDocument) => {
    console.log('Order successfully placed:', order.id);
    setCartItems([]);
  };

  return (
    <>
      <Routes>
        {/* Customer Storefront Routes */}
        <Route
          path="/"
          element={
            <StorefrontLayout
              locale={locale}
              setLocale={setLocale}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              cartCount={totalCartCount}
              onOpenCart={() => setIsCartOpen(true)}
            >
              <CatalogView
                locale={locale}
                searchQuery={searchQuery}
                onAddToCart={handleAddToCart}
              />
            </StorefrontLayout>
          }
        />

        {/* Customer Account & Order History Route */}
        <Route
          path="/account"
          element={
            <StorefrontLayout
              locale={locale}
              setLocale={setLocale}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              cartCount={totalCartCount}
              onOpenCart={() => setIsCartOpen(true)}
            >
              <AccountPage />
            </StorefrontLayout>
          }
        />

        {/* Restricted Operations / Admin Portal Route Package */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute requiredRole="ROLE_ADMIN" portalTitle="Store Operations Admin">
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminOrdersPage />} />
        </Route>

        {/* Restricted DevOps / Engineering Telemetry Console Route Package */}
        <Route
          path="/ops"
          element={
            <ProtectedRoute requiredRole="ROLE_ENGINEER" portalTitle="SRE & Migration Telemetry">
              <OpsLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<ParityMonitorPage />} />
        </Route>

        {/* Catch-all Redirect */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* Slide-over Shopping Cart Drawer */}
      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
        cartItems={cartItems}
        onUpdateQuantity={handleUpdateQuantity}
        onRemoveItem={handleRemoveItem}
        onClearCart={handleClearCart}
        onProceedToCheckout={handleProceedToCheckout}
        locale={locale}
      />

      {/* Checkout Wizard Modal */}
      <CheckoutModal
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        cartItems={cartItems}
        locale={locale}
        onOrderSuccess={handleOrderSuccess}
      />

      {/* Global Authentication Modal */}
      <LoginModal />
    </>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
