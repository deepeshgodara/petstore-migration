import React from 'react';
import { Activity, ArrowLeft, LogOut, User as UserIcon, Radio, Shield } from 'lucide-react';
import { useAuth } from '../../auth';
import { Link } from 'react-router-dom';

export const OpsNavbar: React.FC = () => {
  const { user, logout, openLoginModal } = useAuth();

  return (
    <header className="navbar" style={{ borderBottomColor: 'rgba(16, 185, 129, 0.25)', background: 'rgba(8, 20, 16, 0.9)' }}>
      <div className="navbar-inner">
        {/* Brand & Portal Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link to="/" className="brand-logo" style={{ textDecoration: 'none' }}>
            <div className="brand-icon" style={{ background: 'linear-gradient(135deg, #059669 0%, #047857 100%)' }}>
              <Activity size={22} color="#ffffff" />
            </div>
            <div>
              <div className="brand-title">
                PetStore <span className="brand-badge" style={{ borderColor: 'rgba(16, 185, 129, 0.4)', color: '#6ee7b7' }}>Ops Telemetry</span>
              </div>
            </div>
          </Link>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', color: '#6ee7b7', background: 'rgba(16,185,129,0.1)', padding: '2px 8px', borderRadius: 'var(--radius-full)', border: '1px solid rgba(16,185,129,0.3)' }}>
            <Radio size={12} className="animate-pulse" />
            <span>Dual-Write & Shadow Reconciler Stream Active</span>
          </div>
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
            to="/admin"
            className="btn-secondary"
            style={{ textDecoration: 'none', padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem', color: '#38bdf8' }}
          >
            <Shield size={13} />
            <span>Admin Console ↗</span>
          </Link>

          {/* Active Engineer Session Status */}
          {user && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-full)', padding: '0.3rem 0.8rem' }}>
              <UserIcon size={14} color="#34d399" />
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
              onClick={() => openLoginModal('ROLE_ENGINEER')}
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
