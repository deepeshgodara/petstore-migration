import React, { useState, useEffect } from 'react';
import { Role, User, DEMO_ACCOUNTS } from './types';
import { AuthContext } from './context';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const saved = localStorage.getItem('petstore_auth_user');
      return saved ? JSON.parse(saved) : DEMO_ACCOUNTS.j2ee.user; // default to customer session
    } catch {
      return DEMO_ACCOUNTS.j2ee.user;
    }
  });

  const [isLoginModalOpen, setIsLoginModalOpen] = useState<boolean>(false);
  const [targetRoleHint, setTargetRoleHint] = useState<Role | undefined>(undefined);

  useEffect(() => {
    try {
      if (user) {
        localStorage.setItem('petstore_auth_user', JSON.stringify(user));
      } else {
        localStorage.removeItem('petstore_auth_user');
      }
    } catch (e) {
      console.error('Failed to update auth in localStorage', e);
    }
  }, [user]);

  const login = (username: string, password?: string): boolean => {
    const trimmedUser = username.trim().toLowerCase();
    const account = DEMO_ACCOUNTS[trimmedUser];

    if (account) {
      if (password && account.passwordHash !== password) {
        return false;
      }
      setUser(account.user);
      setIsLoginModalOpen(false);
      return true;
    }

    // Default dynamic user creation as customer
    const newUser: User = {
      username: trimmedUser,
      name: username,
      email: `${trimmedUser}@example.com`,
      role: 'ROLE_CUSTOMER',
      token: `jwt_mock_token_${trimmedUser}`,
    };
    setUser(newUser);
    setIsLoginModalOpen(false);
    return true;
  };

  const logout = () => {
    setUser(null);
  };

  const hasRole = (requiredRole: Role): boolean => {
    if (!user) return false;
    if (user.role === 'ROLE_SUPERADMIN') return true;
    if (user.role === requiredRole) return true;
    return false;
  };

  const openLoginModal = (hint?: Role) => {
    setTargetRoleHint(hint);
    setIsLoginModalOpen(true);
  };

  const closeLoginModal = () => {
    setIsLoginModalOpen(false);
    setTargetRoleHint(undefined);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        login,
        logout,
        hasRole,
        openLoginModal,
        closeLoginModal,
        isLoginModalOpen,
        targetRoleHint,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
