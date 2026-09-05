import React from 'react';
import { Shield, ArrowLeft, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../auth';
import { Link } from 'react-router-dom';

export const AdminNavbar: React.FC = () => {
  const { user, logout, openLoginModal } = useAuth();

  return (
    <header className="navbar" style={{ borderBottomColor: 'rgba(56, 189, 248, 0.25)', background: 'rgba(10, 18, 30, 0.9)' }}>
      <div className="navbar-inner">
        {/* Brand & Portal Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link to="/" className="brand-logo" style={{ textDecoration: 'none' }}>
            <div className="brand-icon" style={{ background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)' }}>
              <Shield size={22} color="#ffffff" />
            </div>
            <div>
              <div className="brand-title">
                PetStore <span className="brand-badge" style={{ borderColor: 'rgba(56, 189, 248, 0.4)', color: '#38bdf8' }}>Admin Console</span>
              </div>
            </div>
          </Link>
        </div>

        {/* Navigation & Breadcrumbs */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link
            to="/"
            className="btn-secondary"
            style={{ textDecoration: 'none', padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem' }}
          >
            <ArrowLeft size={14} />
            <span>Storefront</span>
          </Link>

          <Link
            to="/ops"
            className="btn-secondary"
            style={{ textDecoration: 'none', padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem', color: '#34d399' }}
          >
            <span>Ops Parity ↗</span>
          </Link>

          {/* Active Admin Session Status */}
          {user && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-full)', padding: '0.3rem 0.8rem' }}>
              <UserIcon size={14} color="#38bdf8" />
              <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>{user.name}</span>
              <span className="brand-badge" style={{ fontSize: '0.65rem' }}>{user.role}</span>
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
              onClick={() => openLoginModal('ROLE_ADMIN')}
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
