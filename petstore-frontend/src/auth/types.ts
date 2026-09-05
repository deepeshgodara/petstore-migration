/**
 * Type definitions for Authentication, Authorization, and Role-Based Access Control.
 */

export type Role = 'ROLE_CUSTOMER' | 'ROLE_ADMIN' | 'ROLE_ENGINEER' | 'ROLE_SUPERADMIN';

export interface User {
  username: string;
  name: string;
  email: string;
  role: Role;
  avatar?: string;
  token?: string;
}

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (username: string, password?: string) => boolean;
  logout: () => void;
  hasRole: (requiredRole: Role) => boolean;
  openLoginModal: (targetRoleHint?: Role) => void;
  closeLoginModal: () => void;
  isLoginModalOpen: boolean;
  targetRoleHint?: Role;
}

export const DEMO_ACCOUNTS: Record<string, { user: User; passwordHash: string }> = {
  j2ee: {
    user: {
      username: 'j2ee',
      name: 'Jane Doe',
      email: 'jane.doe@example.com',
      role: 'ROLE_CUSTOMER',
      token: 'jwt_mock_token_customer_j2ee',
    },
    passwordHash: 'j2ee',
  },
  admin: {
    user: {
      username: 'admin',
      name: 'Store Operations Administrator',
      email: 'admin@petstore.internal',
      role: 'ROLE_ADMIN',
      token: 'jwt_mock_token_admin_ops',
    },
    passwordHash: 'admin123',
  },
  engineer: {
    user: {
      username: 'engineer',
      name: 'Data Reliability Engineer',
      email: 'sre@petstore.internal',
      role: 'ROLE_ENGINEER',
      token: 'jwt_mock_token_sre_parity',
    },
    passwordHash: 'ops123',
  },
  root: {
    user: {
      username: 'root',
      name: 'Platform Superadmin',
      email: 'root@petstore.internal',
      role: 'ROLE_SUPERADMIN',
      token: 'jwt_mock_token_superadmin',
    },
    passwordHash: 'petstore2026',
  },
};
