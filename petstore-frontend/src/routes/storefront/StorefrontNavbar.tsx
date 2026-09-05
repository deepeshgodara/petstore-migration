import React from 'react';
import { ShoppingBag, Search, X, Store, Globe, User as UserIcon, LogOut, Shield, Activity, Package } from 'lucide-react';
import { Locale, SUPPORTED_LOCALES } from '../../types/catalog';
import { useAuth } from '../../auth';
import { Link, useNavigate } from 'react-router-dom';

interface StorefrontNavbarProps {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  cartCount: number;
  onOpenCart: () => void;
}

export const StorefrontNavbar: React.FC<StorefrontNavbarProps> = ({
  locale,
  setLocale,
  searchQuery,
  setSearchQuery,
  cartCount,
  onOpenCart,
}) => {
  const { user, isAuthenticated, logout, openLoginModal } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        {/* Brand Logo */}
        <Link to="/" className="brand-logo" style={{ textDecoration: 'none' }}>
          <div className="brand-icon">
            <Store size={22} color="#ffffff" />
          </div>
          <div>
            <div className="brand-title">
              PetStore
              <span className="brand-badge">Cloud Native</span>
            </div>
          </div>
        </Link>

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

        {/* Staff Portal Jump Links for Demonstration */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem' }}>
          <Link
            to="/admin"
            className="tab-btn"
            style={{ textDecoration: 'none', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-subtle)', padding: '0.35rem 0.65rem' }}
            title="Restricted Store Operations Portal"
          >
            <Shield size={13} color="#38bdf8" />
            <span style={{ color: '#38bdf8' }}>Admin</span>
          </Link>

          <Link
            to="/ops"
            className="tab-btn"
            style={{ textDecoration: 'none', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-subtle)', padding: '0.35rem 0.65rem' }}
            title="Restricted DevOps & Shadow Reconciliation Console"
          >
            <Activity size={13} color="#34d399" />
            <span style={{ color: '#34d399' }}>Ops Parity</span>
          </Link>
        </div>

        {/* Locale, Cart & Authentication Actions */}
        <div className="nav-actions">
          {/* Multi-lingual Locale Switcher */}
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

          {/* Cart Button */}
          <button className="cart-btn" onClick={onOpenCart} aria-label="Open Shopping Cart">
            <ShoppingBag size={18} color="#6366f1" />
            <span>Cart</span>
            {cartCount > 0 && <span className="cart-count-badge">{cartCount}</span>}
          </button>

          {/* User Account / Auth Controls */}
          {isAuthenticated && user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => navigate('/account')}
                style={{ padding: '0.45rem 0.75rem', gap: '0.4rem', fontSize: '0.8rem' }}
                title="View Customer Profile & Order History"
              >
                <Package size={14} color="#a5b4fc" />
                <span>{user.name.split(' ')[0]}</span>
              </button>

              <button
                type="button"
                className="btn-secondary"
                onClick={logout}
                style={{ padding: '0.45rem 0.6rem', color: 'var(--text-muted)' }}
                title="Sign Out"
              >
                <LogOut size={14} />
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn-primary"
              onClick={() => openLoginModal()}
              style={{ padding: '0.45rem 0.9rem', fontSize: '0.8rem', gap: '0.4rem' }}
            >
              <UserIcon size={14} />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
