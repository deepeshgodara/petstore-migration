import React from 'react';
import { OpsNavbar } from './OpsNavbar';
import { Outlet } from 'react-router-dom';

export const OpsLayout: React.FC = () => {
  return (
    <div className="app-container" style={{ background: 'radial-gradient(circle at 50% -20%, rgba(5, 150, 105, 0.15), transparent 70%)' }}>
      <OpsNavbar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};
