import React, { useState } from 'react';
import { MigrationParityView } from '../../components/MigrationParityView';
import { MongoDiagnosticsView } from '../../components/MongoDiagnosticsView';
import { Database, ShieldCheck } from 'lucide-react';

export const ParityMonitorPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'parity' | 'mongo'>('parity');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Tab Selector Navigation */}
      <div
        style={{
          display: 'flex',
          background: 'rgba(15, 23, 42, 0.85)',
          padding: '0.35rem',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border-subtle)',
          width: 'fit-content',
          gap: '0.35rem',
        }}
      >
        <button
          type="button"
          onClick={() => setActiveTab('parity')}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.55rem 1.15rem',
            borderRadius: 'var(--radius-md)',
            border: 'none',
            fontSize: '0.875rem',
            fontWeight: activeTab === 'parity' ? 700 : 500,
            background: activeTab === 'parity' ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)' : 'transparent',
            color: activeTab === 'parity' ? '#ffffff' : 'var(--text-secondary)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: activeTab === 'parity' ? '0 2px 10px rgba(16, 185, 129, 0.3)' : 'none',
          }}
        >
          <ShieldCheck size={16} />
          <span>Migration Parity & Reconciliation</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('mongo')}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.55rem 1.15rem',
            borderRadius: 'var(--radius-md)',
            border: 'none',
            fontSize: '0.875rem',
            fontWeight: activeTab === 'mongo' ? 700 : 500,
            background: activeTab === 'mongo' ? 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)' : 'transparent',
            color: activeTab === 'mongo' ? '#ffffff' : 'var(--text-secondary)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: activeTab === 'mongo' ? '0 2px 10px rgba(2, 132, 199, 0.3)' : 'none',
          }}
        >
          <Database size={16} />
          <span>MongoDB Engine & Compass</span>
        </button>
      </div>

      {/* Tab Contents */}
      {activeTab === 'parity' ? <MigrationParityView /> : <MongoDiagnosticsView />}
    </div>
  );
};

