import React from 'react';
import { SupplierNavbar } from './SupplierNavbar';
import { Outlet } from 'react-router-dom';

export const SupplierLayout: React.FC = () => {
  return (
    <div className="app-container" style={{ background: 'radial-gradient(circle at 50% -20%, rgba(217, 119, 6, 0.15), transparent 70%)' }}>
      <SupplierNavbar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};
