import React from 'react';
import { Truck, ArrowLeft, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../auth';
import { Link } from 'react-router-dom';

export const SupplierNavbar: React.FC = () => {
  const { user, logout, openLoginModal } = useAuth();

  return (
    <header className="navbar" style={{ borderBottomColor: 'rgba(245, 158, 11, 0.25)', background: 'rgba(18, 16, 10, 0.9)' }}>
      <div className="navbar-inner">
        {/* Brand & Portal Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link to="/" className="brand-logo" style={{ textDecoration: 'none' }}>
            <div className="brand-icon" style={{ background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)' }}>
              <Truck size={22} color="#ffffff" />
            </div>
            <div>
              <div className="brand-title">
                PetStore <span className="brand-badge" style={{ borderColor: 'rgba(245, 158, 11, 0.4)', color: '#f59e0b' }}>Supplier & Inventory</span>
              </div>
            </div>
          </Link>
        </div>

        {/* Navigation & Breadcrumbs */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link
            to="/"
            className="btn-secondary"
            style={{ textDecoration: 'none', padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem' }}
          >
            <ArrowLeft size={14} />
            <span>Storefront</span>
          </Link>

          <Link
            to="/admin"
            className="btn-secondary"
            style={{ textDecoration: 'none', padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem', color: '#38bdf8' }}
          >
            <span>Admin Portal</span>
          </Link>

          {/* Active Supplier Session Status */}
          {user && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-full)', padding: '0.3rem 0.8rem' }}>
              <UserIcon size={14} color="#f59e0b" />
              <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>{user.name}</span>
              <span className="brand-badge" style={{ fontSize: '0.65rem', borderColor: 'rgba(245, 158, 11, 0.4)', color: '#f59e0b' }}>{user.role}</span>
            </div>
          )}

          {user ? (
            <button
              type="button"
              className="btn-secondary"
              onClick={logout}
              style={{ padding: '0.45rem 0.65rem', color: 'var(--text-muted)' }}
              title="Sign Out"
            >
              <LogOut size={14} />
            </button>
          ) : (
            <button
              type="button"
              className="btn-primary"
              onClick={() => openLoginModal('ROLE_SUPPLIER')}
              style={{ padding: '0.45rem 0.85rem', fontSize: '0.8rem' }}
            >
              Sign In
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
