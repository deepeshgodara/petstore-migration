import React from 'react';
import { ShoppingBag, Search, X, Layers, Activity, Store, Globe } from 'lucide-react';
import { Locale, SUPPORTED_LOCALES } from '../types/catalog';

export type NavTab = 'storefront' | 'admin' | 'migration';

interface NavbarProps {
  activeTab: NavTab;
  setActiveTab: (tab: NavTab) => void;
  locale: Locale;
  setLocale: (locale: Locale) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  cartCount: number;
  onOpenCart: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  locale,
  setLocale,
  searchQuery,
  setSearchQuery,
  cartCount,
  onOpenCart,
}) => {
  return (
    <header className="navbar">
      <div className="navbar-inner">
        {/* Brand Logo */}
        <div 
          className="brand-logo" 
          role="button"
          tabIndex={0}
          onClick={() => setActiveTab('storefront')}
          onKeyDown={(e) => { if (e.key === 'Enter') setActiveTab('storefront'); }}
          style={{ cursor: 'pointer' }}
        >
          <div className="brand-icon">
            <Store size={22} color="#ffffff" />
          </div>
          <div>
            <div className="brand-title">
              PetStore
              <span className="brand-badge">Cloud Native</span>
            </div>
          </div>
        </div>

        {/* Global Search Bar */}
        <div className="search-container">
          <Search size={16} className="search-icon-left" />
          <input
            type="text"
            className="search-input"
            placeholder={
              locale === 'ja_JP'
                ? 'ペットや品種を検索...'
                : locale === 'zh_CN'
                ? '搜索宠物或品种...'
                : 'Search pets, breeds, SKUs...'
            }
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button
              className="search-clear-btn"
              onClick={() => setSearchQuery('')}
              title="Clear search"
            >
              <X size={16} />
            </button>
          )}
        </div>

        {/* Navigation Tabs */}
        <div className="tab-nav">
          <button
            className={`tab-btn ${activeTab === 'storefront' ? 'active' : ''}`}
            onClick={() => setActiveTab('storefront')}
          >
            <Store size={15} />
            <span>Storefront</span>
          </button>
          <button
            className={`tab-btn ${activeTab === 'admin' ? 'active' : ''}`}
            onClick={() => setActiveTab('admin')}
          >
            <Layers size={15} />
            <span>Admin</span>
          </button>
          <button
            className={`tab-btn ${activeTab === 'migration' ? 'active' : ''}`}
            onClick={() => setActiveTab('migration')}
          >
            <Activity size={15} />
            <span>Migration Parity</span>
          </button>
        </div>

        {/* Locale Switcher & Cart */}
        <div className="nav-actions">
          <div className="locale-select-wrap">
            <Globe size={15} color="#94a3b8" />
            <select
              className="locale-select"
              value={locale}
              onChange={(e) => setLocale(e.target.value as Locale)}
              aria-label="Select Language"
            >
              {SUPPORTED_LOCALES.map((loc) => (
                <option key={loc.code} value={loc.code}>
                  {loc.flag} {loc.nativeLabel}
                </option>
              ))}
            </select>
          </div>

          <button className="cart-btn" onClick={onOpenCart} aria-label="Open Shopping Cart">
            <ShoppingBag size={18} color="#6366f1" />
            <span>Cart</span>
            {cartCount > 0 && <span className="cart-count-badge">{cartCount}</span>}
          </button>
        </div>
      </div>
    </header>
  );
};
