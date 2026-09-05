import React from 'react';
import { useAuth } from './useAuth';
import { Role } from './types';
import { AccessDeniedGate } from '../components/auth/AccessDeniedGate';

interface ProtectedRouteProps {
  requiredRole: Role;
  portalTitle: string;
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  requiredRole,
  portalTitle,
  children,
}) => {
  const { hasRole } = useAuth();

  if (!hasRole(requiredRole)) {
    return <AccessDeniedGate requiredRole={requiredRole} portalTitle={portalTitle} />;
  }

  return <>{children}</>;
};
