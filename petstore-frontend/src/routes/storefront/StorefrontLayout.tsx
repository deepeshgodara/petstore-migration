import React from 'react';
import { StorefrontNavbar } from './StorefrontNavbar';
import { Locale } from '../../types/catalog';

interface StorefrontLayoutProps {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  cartCount: number;
  onOpenCart: () => void;
  children: React.ReactNode;
}

export const StorefrontLayout: React.FC<StorefrontLayoutProps> = ({
  locale,
  setLocale,
  searchQuery,
  setSearchQuery,
  cartCount,
  onOpenCart,
  children,
}) => {
  return (
    <div className="app-container">
      <StorefrontNavbar
        locale={locale}
        setLocale={setLocale}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        cartCount={cartCount}
        onOpenCart={onOpenCart}
      />
      <main className="main-content">{children}</main>
    </div>
  );
};
