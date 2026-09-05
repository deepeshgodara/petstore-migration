import React from 'react';
import { AdminNavbar } from './AdminNavbar';
import { Outlet } from 'react-router-dom';

export const AdminLayout: React.FC = () => {
  return (
    <div className="app-container" style={{ background: 'radial-gradient(circle at 50% -20%, rgba(2, 132, 199, 0.15), transparent 70%)' }}>
      <AdminNavbar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};
