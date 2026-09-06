import React, { useState, useEffect } from 'react';
import { Role, User, DEMO_ACCOUNTS, UserRegistrationPayload } from './types';
import { AuthContext } from './context';
import { userService } from '../services/userService';

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

  const login = async (username: string, password?: string): Promise<boolean> => {
    const trimmedUser = username.trim().toLowerCase();

    // 1. Try modern backend user authentication first
    try {
      const remoteUser = await userService.login(trimmedUser, password);
      if (remoteUser) {
        setUser(remoteUser);
        setIsLoginModalOpen(false);
        return true;
      }
    } catch (err) {
      console.warn('Backend login attempt failed, trying demo presets...', err);
    }

    // 2. Demo role account fallback
    const account = DEMO_ACCOUNTS[trimmedUser];
    if (account) {
      if (password && account.passwordHash !== password) {
        return false;
      }
      setUser(account.user);
      setIsLoginModalOpen(false);
      return true;
    }

    // 3. Dynamic guest customer session fallback
    if (!password || password === 'password' || password === trimmedUser) {
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
    }

    return false;
  };

  const register = async (payload: UserRegistrationPayload): Promise<{ success: boolean; message?: string }> => {
    try {
      const newUser = await userService.register(payload);
      setUser(newUser);
      setIsLoginModalOpen(false);
      return { success: true };
    } catch (e: any) {
      return { success: false, message: e.message || 'Registration failed' };
    }
  };

  const logout = () => {
    setUser(null);
  };

  const hasRole = (requiredRole: Role): boolean => {
    if (!user) return false;
    if (user.role === 'ROLE_SUPERADMIN') return true;
    if (user.role === 'ROLE_ADMIN' && requiredRole === 'ROLE_SUPPLIER') return true;
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
        register,
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
