import React from 'react';
import { useAuth } from '../../auth';
import { Role } from '../../auth/types';
import { ShieldAlert, ArrowLeft, KeyRound } from 'lucide-react';
import { Link } from 'react-router-dom';

interface AccessDeniedGateProps {
  requiredRole: Role;
  portalTitle: string;
}

export const AccessDeniedGate: React.FC<AccessDeniedGateProps> = ({
  requiredRole,
  portalTitle,
}) => {
  const { user, openLoginModal } = useAuth();

  const roleNameMap: Record<Role, string> = {
    ROLE_CUSTOMER: 'Customer',
    ROLE_ADMIN: 'Store Operations Administrator',
    ROLE_ENGINEER: 'Data Reliability / DevOps Engineer',
    ROLE_SUPERADMIN: 'Platform Superadmin',
  };

  return (
    <div
      style={{
        maxWidth: '560px',
        margin: '4rem auto',
        padding: '2.5rem 2rem',
        background: 'var(--bg-card)',
        border: '1px solid rgba(244, 63, 94, 0.3)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.5)',
        textAlign: 'center',
      }}
    >
      <div
        style={{
          width: '68px',
          height: '68px',
          borderRadius: 'var(--radius-full)',
          background: 'rgba(244, 63, 94, 0.15)',
          border: '1px solid rgba(244, 63, 94, 0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 1.25rem auto',
        }}
      >
        <ShieldAlert size={36} color="#f43f5e" />
      </div>

      <span className="brand-badge" style={{ borderColor: 'rgba(244, 63, 94, 0.4)', color: '#fda4af', marginBottom: '0.75rem' }}>
        403 Forbidden • Missing Authorization
      </span>

      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, marginTop: '0.5rem', marginBottom: '0.5rem' }}>
        Restricted Portal: {portalTitle}
      </h2>

      <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.5rem' }}>
        This management portal requires <strong>{roleNameMap[requiredRole]}</strong> privileges.
        {user ? (
          <>
            {' '}You are currently signed in as <code style={{ color: '#a5b4fc' }}>{user.name}</code> with role{' '}
            <code style={{ color: '#fbbf24' }}>{user.role}</code>.
          </>
        ) : (
          ' You are currently unauthenticated.'
        )}
      </p>

      <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
        <Link to="/" className="btn-secondary" style={{ textDecoration: 'none' }}>
          <ArrowLeft size={16} />
          <span>Return to Storefront</span>
        </Link>

        <button
          type="button"
          className="btn-primary"
          onClick={() => openLoginModal(requiredRole)}
          style={{ background: 'var(--brand-primary)' }}
        >
          <KeyRound size={16} />
          <span>Authenticate as {roleNameMap[requiredRole].split(' ')[0]}</span>
        </button>
      </div>
    </div>
  );
};
