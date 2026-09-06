import React, { useState } from 'react';
import { useAuth, DEMO_ACCOUNTS } from '../../auth';
import {
  X,
  Lock,
  Shield,
  User as UserIcon,
  AlertCircle,
  KeyRound,
  Sparkles,
  Truck,
  UserPlus,
} from 'lucide-react';

export const LoginModal: React.FC = () => {
  const { isLoginModalOpen, closeLoginModal, login, register, targetRoleHint } = useAuth();

  const [activeTab, setActiveTab] = useState<'signin' | 'register'>('signin');

  const defaultUser =
    targetRoleHint === 'ROLE_ADMIN'
      ? DEMO_ACCOUNTS.admin.user.username
      : targetRoleHint === 'ROLE_SUPPLIER'
      ? DEMO_ACCOUNTS.supplier.user.username
      : targetRoleHint === 'ROLE_ENGINEER'
      ? DEMO_ACCOUNTS.engineer.user.username
      : 'j2ee';

  const defaultPass =
    targetRoleHint === 'ROLE_ADMIN'
      ? DEMO_ACCOUNTS.admin.passwordHash
      : targetRoleHint === 'ROLE_SUPPLIER'
      ? DEMO_ACCOUNTS.supplier.passwordHash
      : targetRoleHint === 'ROLE_ENGINEER'
      ? DEMO_ACCOUNTS.engineer.passwordHash
      : 'j2ee';

  const [username, setUsername] = useState<string>(defaultUser);
  const [password, setPassword] = useState<string>(defaultPass);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Registration Form State
  const [regData, setRegData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    street1: '',
    city: '',
    state: '',
    zip: '',
    country: 'USA',
    favoriteCategory: 'FISH',
    language: 'en_US',
    bannerOption: true,
    listOption: true,
  });

  // Synchronize state when targetRoleHint changes
  const [prevRoleHint, setPrevRoleHint] = useState<typeof targetRoleHint>(targetRoleHint);
  if (targetRoleHint !== prevRoleHint) {
    setPrevRoleHint(targetRoleHint);
    setUsername(defaultUser);
    setPassword(defaultPass);
    setError(null);
  }

  if (!isLoginModalOpen) return null;

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const success = await login(username, password);
      if (!success) {
        setError('Invalid credentials. Check your password or use one of the demo presets below.');
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (regData.password !== regData.confirmPassword) {
      setError('Passwords do not match. Please verify.');
      return;
    }

    if (regData.password.length < 4) {
      setError('Password must be at least 4 characters long.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await register({
        username: regData.username,
        password: regData.password,
        email: regData.email,
        firstName: regData.firstName,
        lastName: regData.lastName,
        phone: regData.phone,
        street1: regData.street1,
        city: regData.city,
        state: regData.state,
        zip: regData.zip,
        country: regData.country,
        favoriteCategory: regData.favoriteCategory,
        language: regData.language,
        bannerOption: regData.bannerOption,
        listOption: regData.listOption,
      });

      if (!result.success) {
        setError(result.message || 'Registration failed. Please check your details.');
      }
    } catch (err: any) {
      setError(err.message || 'Registration failed.');
    } finally {
      setSubmitting(false);
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
        style={{
          maxWidth: activeTab === 'register' ? '560px' : '440px',
          padding: '2rem',
          maxHeight: '90vh',
          overflowY: 'auto',
          transition: 'max-width 0.2s ease',
        }}
      >
        <button className="modal-close-btn" onClick={closeLoginModal} aria-label="Close modal">
          <X size={18} />
        </button>

        {/* Tab Navigation */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-subtle)',
            marginBottom: '1.5rem',
            gap: '1rem',
          }}
        >
          <button
            type="button"
            onClick={() => {
              setActiveTab('signin');
              setError(null);
            }}
            style={{
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'signin' ? '2px solid var(--accent-indigo)' : '2px solid transparent',
              color: activeTab === 'signin' ? 'var(--text-primary)' : 'var(--text-muted)',
              fontWeight: 700,
              fontSize: '0.95rem',
              padding: '0.5rem 0.25rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            <Lock size={15} />
            <span>Sign In</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveTab('register');
              setError(null);
            }}
            style={{
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'register' ? '2px solid var(--accent-indigo)' : '2px solid transparent',
              color: activeTab === 'register' ? 'var(--text-primary)' : 'var(--text-muted)',
              fontWeight: 700,
              fontSize: '0.95rem',
              padding: '0.5rem 0.25rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            <UserPlus size={15} />
            <span>Create Account</span>
          </button>
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

        {activeTab === 'signin' ? (
          <div>
            <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
              <div
                style={{
                  width: '52px',
                  height: '52px',
                  borderRadius: 'var(--radius-full)',
                  background: 'rgba(99, 102, 241, 0.15)',
                  border: '1px solid rgba(99, 102, 241, 0.35)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 0.75rem auto',
                }}
              >
                <Lock size={24} color="#818cf8" />
              </div>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Account Sign In</h2>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', marginTop: '0.2rem' }}>
                {targetRoleHint === 'ROLE_ADMIN'
                  ? 'Administrator credentials required to access order management'
                  : targetRoleHint === 'ROLE_SUPPLIER'
                  ? 'Supplier credentials required to manage inventory & supply stock'
                  : targetRoleHint === 'ROLE_ENGINEER'
                  ? 'Engineer credentials required to access migration telemetry'
                  : 'Sign in to access your orders, cart, and pet preferences'}
              </p>
            </div>

            <form onSubmit={handleSignIn} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
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
                disabled={submitting}
                className="btn-primary"
                style={{ padding: '0.75rem', justifyContent: 'center', fontSize: '0.9rem', marginTop: '0.5rem' }}
              >
                {submitting ? 'Authenticating...' : 'Authenticate & Proceed'}
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
                  onClick={() => handleSelectPreset('supplier')}
                  style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start', color: '#f59e0b' }}
                >
                  <Truck size={13} />
                  <span>Supplier (supplier)</span>
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
                  style={{ fontSize: '0.75rem', padding: '0.45rem 0.6rem', justifyContent: 'flex-start', color: '#c084fc', gridColumn: 'span 2' }}
                >
                  <Lock size={13} />
                  <span>Superadmin (root)</span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div>
            <div style={{ textAlign: 'center', marginBottom: '1.25rem' }}>
              <div
                style={{
                  width: '52px',
                  height: '52px',
                  borderRadius: 'var(--radius-full)',
                  background: 'rgba(16, 185, 129, 0.15)',
                  border: '1px solid rgba(16, 185, 129, 0.35)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 0.75rem auto',
                }}
              >
                <UserPlus size={24} color="#34d399" />
              </div>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Create New Customer Account</h2>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', marginTop: '0.2rem' }}>
                Join the modern Pet Store platform. Your account is saved to MongoDB and synced via Kafka.
              </p>
            </div>

            <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Username *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. petlover99"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.username}
                    onChange={(e) => setRegData({ ...regData, username: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Email Address *
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="user@example.com"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.email}
                    onChange={(e) => setRegData({ ...regData, email: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Password *
                  </label>
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.password}
                    onChange={(e) => setRegData({ ...regData, password: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Confirm Password *
                  </label>
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.confirmPassword}
                    onChange={(e) => setRegData({ ...regData, confirmPassword: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    First Name
                  </label>
                  <input
                    type="text"
                    placeholder="John"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.firstName}
                    onChange={(e) => setRegData({ ...regData, firstName: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Last Name
                  </label>
                  <input
                    type="text"
                    placeholder="Smith"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.lastName}
                    onChange={(e) => setRegData({ ...regData, lastName: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Phone Number
                  </label>
                  <input
                    type="tel"
                    placeholder="555-123-4567"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.phone}
                    onChange={(e) => setRegData({ ...regData, phone: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Favorite Pet Category
                  </label>
                  <select
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)', background: 'var(--bg-card)' }}
                    value={regData.favoriteCategory}
                    onChange={(e) => setRegData({ ...regData, favoriteCategory: e.target.value })}
                  >
                    <option value="FISH">Fish (Fresh & Saltwater)</option>
                    <option value="DOGS">Dogs</option>
                    <option value="REPTILES">Reptiles & Amphibians</option>
                    <option value="CATS">Cats</option>
                    <option value="BIRDS">Birds</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  Street Address
                </label>
                <input
                  type="text"
                  placeholder="1234 Main Street"
                  className="search-input"
                  style={{ borderRadius: 'var(--radius-sm)' }}
                  value={regData.street1}
                  onChange={(e) => setRegData({ ...regData, street1: e.target.value })}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '0.5rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    City
                  </label>
                  <input
                    type="text"
                    placeholder="San Francisco"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.city}
                    onChange={(e) => setRegData({ ...regData, city: e.target.value })}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    State
                  </label>
                  <input
                    type="text"
                    placeholder="CA"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.state}
                    onChange={(e) => setRegData({ ...regData, state: e.target.value })}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Zip
                  </label>
                  <input
                    type="text"
                    placeholder="94105"
                    className="search-input"
                    style={{ borderRadius: 'var(--radius-sm)' }}
                    value={regData.zip}
                    onChange={(e) => setRegData({ ...regData, zip: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.25rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={regData.bannerOption}
                    onChange={(e) => setRegData({ ...regData, bannerOption: e.target.checked })}
                  />
                  <span>Enable banner announcements</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={regData.listOption}
                    onChange={(e) => setRegData({ ...regData, listOption: e.target.checked })}
                  />
                  <span>Show personalized list</span>
                </label>
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="btn-primary"
                style={{
                  padding: '0.75rem',
                  justifyContent: 'center',
                  fontSize: '0.9rem',
                  marginTop: '0.75rem',
                  background: 'var(--accent-emerald)',
                }}
              >
                {submitting ? 'Creating Account & Syncing...' : 'Create Account & Sign In'}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};
