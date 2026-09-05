import React, { useState } from 'react';
import { useAuth, DEMO_ACCOUNTS } from '../../auth';
import { X, Lock, Shield, User as UserIcon, AlertCircle, KeyRound, Sparkles } from 'lucide-react';

export const LoginModal: React.FC = () => {
  const { isLoginModalOpen, closeLoginModal, login, targetRoleHint } = useAuth();

  const defaultUser =
    targetRoleHint === 'ROLE_ADMIN'
      ? DEMO_ACCOUNTS.admin.user.username
      : targetRoleHint === 'ROLE_ENGINEER'
      ? DEMO_ACCOUNTS.engineer.user.username
      : 'j2ee';

  const defaultPass =
    targetRoleHint === 'ROLE_ADMIN'
      ? DEMO_ACCOUNTS.admin.passwordHash
      : targetRoleHint === 'ROLE_ENGINEER'
      ? DEMO_ACCOUNTS.engineer.passwordHash
      : 'j2ee';

  const [username, setUsername] = useState<string>(defaultUser);
  const [password, setPassword] = useState<string>(defaultPass);
  const [error, setError] = useState<string | null>(null);

  // Synchronize state when targetRoleHint changes during render
  const [prevRoleHint, setPrevRoleHint] = useState<typeof targetRoleHint>(targetRoleHint);
  if (targetRoleHint !== prevRoleHint) {
    setPrevRoleHint(targetRoleHint);
    setUsername(defaultUser);
    setPassword(defaultPass);
    setError(null);
  }

  if (!isLoginModalOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const success = login(username, password);
    if (!success) {
      setError('Invalid credentials. Check the password or click one of the demo role presets below.');
    }
  };

  const handleSelectPreset = (key: string) => {
    const acc = DEMO_ACCOUNTS[key];
    if (acc) {
      setUsername(acc.user.username);
      setPassword(acc.passwordHash);
      setError(null);
    }
  };

  return (
    <div className="modal-overlay" onClick={closeLoginModal}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '440px', padding: '2rem' }}
      >
        <button className="modal-close-btn" onClick={closeLoginModal} aria-label="Close modal">
          <X size={18} />
        </button>

        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: 'var(--radius-full)',
              background: 'rgba(99, 102, 241, 0.15)',
              border: '1px solid rgba(99, 102, 241, 0.35)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 0.75rem auto',
            }}
          >
            <Lock size={26} color="#818cf8" />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Account Sign In</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
            {targetRoleHint === 'ROLE_ADMIN'
              ? 'Administrator credentials required to access order management'
              : targetRoleHint === 'ROLE_ENGINEER'
              ? 'Engineer credentials required to access migration telemetry'
              : 'Sign in to access your orders, cart, and pet preferences'}
          </p>
        </div>

        {error && (
          <div
            style={{
              padding: '0.75rem',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(244, 63, 94, 0.15)',
              border: '1px solid rgba(244, 63, 94, 0.4)',
              color: '#fda4af',
              fontSize: '0.825rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '1rem',
            }}
          >
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Username
            </label>
            <div style={{ position: 'relative' }}>
              <UserIcon size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                required
                className="search-input"
                style={{ borderRadius: 'var(--radius-sm)', paddingLeft: '2.2rem' }}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <KeyRound size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="password"
                required
                className="search-input"
                style={{ borderRadius: 'var(--radius-sm)', paddingLeft: '2.2rem' }}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn-primary"
            style={{ padding: '0.75rem', justifyContent: 'center', fontSize: '0.9rem', marginTop: '0.5rem' }}
          >
            Authenticate & Proceed
          </button>
        </form>

        {/* Demo Account Quick Switchers */}
        <div style={{ marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-subtle)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
            <Sparkles size={13} color="#a5b4fc" />
            <span>Quick-Switch Demo Accounts</span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleSelectPreset('j2ee')}
              style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start' }}
            >
              <UserIcon size={13} />
              <span>Customer (j2ee)</span>
            </button>

            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleSelectPreset('admin')}
              style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start', color: '#38bdf8' }}
            >
              <Shield size={13} />
              <span>Admin (admin)</span>
            </button>

            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleSelectPreset('engineer')}
              style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start', color: '#34d399' }}
            >
              <Sparkles size={13} />
              <span>DevOps (engineer)</span>
            </button>

            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleSelectPreset('root')}
              style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start', color: '#c084fc' }}
            >
              <Lock size={13} />
              <span>Superadmin (root)</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
