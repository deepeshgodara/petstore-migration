/**
 * Type definitions for Authentication, Authorization, and Role-Based Access Control.
 */

export type Role = 'ROLE_CUSTOMER' | 'ROLE_ADMIN' | 'ROLE_SUPPLIER' | 'ROLE_ENGINEER' | 'ROLE_SUPERADMIN';

export interface User {
  username: string;
  name: string;
  email: string;
  role: Role;
  avatar?: string;
  token?: string;
}

export interface UserRegistrationPayload {
  username: string;
  password?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  street1?: string;
  street2?: string;
  city?: string;
  state?: string;
  zip?: string;
  country?: string;
  language?: string;
  favoriteCategory?: string;
  bannerOption?: boolean;
  listOption?: boolean;
}

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (username: string, password?: string) => Promise<boolean> | boolean;
  register: (payload: UserRegistrationPayload) => Promise<{ success: boolean; message?: string }>;
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
  supplier: {
    user: {
      username: 'supplier',
      name: 'Acme Pet Supply Co.',
      email: 'supplier@petstore.internal',
      role: 'ROLE_SUPPLIER',
      token: 'jwt_mock_token_supplier_inventory',
    },
    passwordHash: 'supplier',
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
