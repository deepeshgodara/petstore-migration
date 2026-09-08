import React, { useState, useEffect } from 'react';
import { MongoDiagnosticsResponse } from '../types/migration';
import { migrationService } from '../services/migrationService';
import {
  Database,
  Cpu,
  Activity,
  HardDrive,
  Copy,
  Check,
  RefreshCw,
  Layers,
  Zap,
  Terminal,
} from 'lucide-react';

export const MongoDiagnosticsView: React.FC = () => {
  const [diagnostics, setDiagnostics] = useState<MongoDiagnosticsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(false);

  const fetchDiagnostics = async () => {
    try {
      const data = await migrationService.getMongoDiagnostics();
      setDiagnostics(data);
    } catch (err) {
      console.error('Failed to load MongoDB diagnostics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    migrationService
      .getMongoDiagnostics()
      .then((data) => {
        if (active) setDiagnostics(data);
      })
      .catch((err) => {
        console.error('Failed to load MongoDB diagnostics:', err);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  // Optional 5s auto-refresh interval for live engineering monitoring
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => {
      fetchDiagnostics();
    }, 5000);
    return () => clearInterval(interval);
  }, [autoRefresh]);

  const handleCopyUri = () => {
    if (!diagnostics?.compassConnectionUri) return;
    navigator.clipboard.writeText(diagnostics.compassConnectionUri);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const formatUptime = (totalSeconds: number) => {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return `${hours}h ${minutes}m ${seconds}s`;
  };

  const formatBytes = (bytes?: number) => {
    if (!bytes && bytes !== 0) return '0 B';
    if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
    if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${bytes} B`;
  };

  const conns = diagnostics?.connections || {};
  const currConns = conns.current || 0;
  const availConns = conns.available || 1;
  const connCapacityPct = Math.min(100, Math.max(1, (currConns / (currConns + availConns)) * 100));

  const wt = diagnostics?.wiredTigerCache || {};
  const cacheBytes = wt.bytesInCache || 0;
  const maxCacheBytes = wt.maxBytes || 1;
  const cacheUsagePct = Math.min(100, (cacheBytes / maxCacheBytes) * 100);

  const ops = diagnostics?.opcounters || {};
  const totalOps = (ops.query || 0) + (ops.insert || 0) + (ops.update || 0) + (ops.delete || 0) + (ops.command || 0);

  const dbStats = diagnostics?.databaseStats || {};

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Header Bar */}
      <div
        className="card-glow"
        style={{
          padding: '1.5rem 1.75rem',
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(13, 27, 42, 0.95) 0%, rgba(15, 23, 42, 0.9) 100%)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 14px rgba(16, 185, 129, 0.4)',
            }}
          >
            <Database size={24} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800, margin: 0 }}>
                MongoDB Engine Diagnostics
              </h2>
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  padding: '0.15rem 0.55rem',
                  borderRadius: 'var(--radius-full)',
                  background: 'rgba(16, 185, 129, 0.2)',
                  color: '#34d399',
                  border: '1px solid rgba(16, 185, 129, 0.4)',
                }}
              >
                <span
                  style={{
                    width: '7px',
                    height: '7px',
                    borderRadius: '50%',
                    background: '#10b981',
                    boxShadow: '0 0 8px #10b981',
                  }}
                />
                {diagnostics?.status || 'CONNECTING'}
              </span>
              <span className="brand-badge" style={{ fontSize: '0.7rem' }}>
                rs0 (Primary)
              </span>
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
              Real-time database performance telemetry, WiredTiger cache metrics, and Compass integration
            </p>
          </div>
        </div>

        {/* Right Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <label
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              fontSize: '0.8rem',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
              userSelect: 'none',
              background: 'rgba(255, 255, 255, 0.04)',
              padding: '0.4rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              style={{ accentColor: '#10b981', cursor: 'pointer' }}
            />
            <span>Auto-refresh (5s)</span>
          </label>

          <button
            type="button"
            className="btn-secondary"
            onClick={fetchDiagnostics}
            disabled={loading}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem' }}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Poll Now</span>
          </button>
        </div>
      </div>

      {/* MongoDB Compass Connection Hero Card */}
      <div
        className="card"
        style={{
          padding: '1.75rem',
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(145deg, rgba(16, 185, 129, 0.07) 0%, rgba(15, 23, 42, 0.95) 100%)',
          border: '1px solid rgba(16, 185, 129, 0.4)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            flexWrap: 'wrap',
            gap: '1rem',
            marginBottom: '1.25rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Terminal size={20} color="#34d399" />
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, margin: 0, color: '#ffffff' }}>
                MongoDB Compass 1-Click Connection
              </h3>
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  background: 'rgba(56, 189, 248, 0.15)',
                  color: '#38bdf8',
                  padding: '0.15rem 0.5rem',
                  borderRadius: 'var(--radius-full)',
                  border: '1px solid rgba(56, 189, 248, 0.3)',
                }}
              >
                Engineers Ops Ready
              </span>
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
              Directly connect MongoDB Compass GUI to inspect live collections, explain execution plans, and validate indexes.
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Engine Version: <strong style={{ color: '#34d399' }}>v{diagnostics?.version || '7.0.x'}</strong>
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Uptime: <strong style={{ color: '#ffffff' }}>{diagnostics ? formatUptime(diagnostics.uptimeSeconds) : '0s'}</strong>
            </span>
          </div>
        </div>

        {/* Connection String Box */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'rgba(5, 10, 20, 0.8)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem 1rem',
            gap: '1rem',
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', overflow: 'hidden' }}>
            <Zap size={16} color="#10b981" />
            <code
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.85rem',
                color: '#6ee7b7',
                wordBreak: 'break-all',
              }}
            >
              {diagnostics?.compassConnectionUri || 'mongodb://localhost:27017/petstore?replicaSet=rs0&directConnection=true'}
            </code>
          </div>

          <button
            type="button"
            className={copied ? 'btn-primary' : 'btn-secondary'}
            onClick={handleCopyUri}
            style={{
              padding: '0.45rem 0.95rem',
              fontSize: '0.8rem',
              gap: '0.45rem',
              whiteSpace: 'nowrap',
              backgroundColor: copied ? '#10b981' : undefined,
              borderColor: copied ? '#10b981' : undefined,
            }}
          >
            {copied ? <Check size={14} /> : <Copy size={14} />}
            <span>{copied ? 'Copied to Clipboard!' : 'Copy Compass URI'}</span>
          </button>
        </div>

        {/* Quick Launch & Diagnostic Guide */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
            gap: '1rem',
            marginTop: '1.25rem',
          }}
        >
          <div
            style={{
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '0.85rem 1rem',
            }}
          >
            <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#38bdf8', marginBottom: '0.3rem' }}>
              Option A: Native Compass GUI
            </div>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: 0 }}>
              Launch MongoDB Compass on your machine and paste the URI above into the "New Connection" input.
            </p>
          </div>

          <div
            style={{
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '0.85rem 1rem',
            }}
          >
            <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#34d399', marginBottom: '0.3rem' }}>
              Option B: Terminal CLI Helper
            </div>
            <code style={{ fontSize: '0.75rem', color: '#a7f3d0' }}>
              ./scripts/mongo_compass_connect.sh
            </code>
          </div>

          <div
            style={{
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '0.85rem 1rem',
            }}
          >
            <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#fbbf24', marginBottom: '0.3rem' }}>
              Option C: Mongosh Shell
            </div>
            <code style={{ fontSize: '0.75rem', color: '#fde68a' }}>
              mongosh "mongodb://localhost:27017/petstore?replicaSet=rs0"
            </code>
          </div>
        </div>
      </div>

      {/* Real-time Telemetry Metrics Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: '1.5rem',
        }}
      >
        {/* Metric Card 1: Connection Pool */}
        <div
          className="card"
          style={{
            padding: '1.5rem',
            background: 'rgba(15, 23, 42, 0.75)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Activity size={18} color="#38bdf8" />
              <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>Connection Pool</h4>
            </div>
            <span style={{ fontSize: '0.75rem', color: '#38bdf8', fontWeight: 600 }}>
              {currConns} active / {availConns} available
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.3rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Saturation</span>
                <span style={{ fontWeight: 700, color: '#ffffff' }}>{connCapacityPct.toFixed(2)}%</span>
              </div>
              <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.06)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    width: `${connCapacityPct}%`,
                    background: 'linear-gradient(90deg, #38bdf8 0%, #0284c7 100%)',
                    borderRadius: 'var(--radius-full)',
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginTop: '0.5rem' }}>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Current Conns</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff' }}>{currConns}</div>
              </div>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Total Lifetime</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff' }}>{conns.totalCreated?.toLocaleString() || '0'}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Metric Card 2: WiredTiger Engine Cache */}
        <div
          className="card"
          style={{
            padding: '1.5rem',
            background: 'rgba(15, 23, 42, 0.75)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <HardDrive size={18} color="#10b981" />
              <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>WiredTiger Cache</h4>
            </div>
            <span style={{ fontSize: '0.75rem', color: '#34d399', fontWeight: 600 }}>
              {formatBytes(cacheBytes)} in RAM
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.3rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Cache Pressure ({formatBytes(maxCacheBytes)} limit)</span>
                <span style={{ fontWeight: 700, color: '#ffffff' }}>{cacheUsagePct.toFixed(2)}%</span>
              </div>
              <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.06)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    width: `${cacheUsagePct}%`,
                    background: 'linear-gradient(90deg, #10b981 0%, #059669 100%)',
                    borderRadius: 'var(--radius-full)',
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginTop: '0.5rem' }}>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Dirty Cache</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff' }}>{formatBytes(wt.dirtyBytes)}</div>
              </div>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Resident Process RAM</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff' }}>{diagnostics?.memory?.residentMb || 0} MB</div>
              </div>
            </div>
          </div>
        </div>

        {/* Metric Card 3: Opcounters (Operations) */}
        <div
          className="card"
          style={{
            padding: '1.5rem',
            background: 'rgba(15, 23, 42, 0.75)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Cpu size={18} color="#f59e0b" />
              <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>Operation Counters</h4>
            </div>
            <span style={{ fontSize: '0.75rem', color: '#fbbf24', fontWeight: 600 }}>
              {totalOps.toLocaleString()} ops
            </span>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(3, 1fr)',
              gap: '0.5rem',
            }}
          >
            <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Queries</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#38bdf8' }}>{ops.query?.toLocaleString() || 0}</div>
            </div>
            <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Updates</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#34d399' }}>{ops.update?.toLocaleString() || 0}</div>
            </div>
            <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Inserts</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#f59e0b' }}>{ops.insert?.toLocaleString() || 0}</div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginTop: '0.5rem' }}>
            <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Deletes</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#f43f5e' }}>{ops.delete?.toLocaleString() || 0}</div>
            </div>
            <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '0.6rem', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Commands</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#a855f7' }}>{ops.command?.toLocaleString() || 0}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Database Footprint & Collection Inventory */}
      <div
        className="card"
        style={{
          padding: '1.75rem',
          borderRadius: 'var(--radius-lg)',
          background: 'rgba(15, 23, 42, 0.75)',
          border: '1px solid var(--border-subtle)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
          <div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Layers size={18} color="#38bdf8" />
              <span>Storage Footprint & Collections</span>
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
              On-disk storage allocation and document counts for the Pet Store database
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Allocated Storage</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                {formatBytes(dbStats.storageSize)}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Total Index Size</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#38bdf8' }}>
                {formatBytes(dbStats.indexSize)}
              </div>
            </div>
          </div>
        </div>

        {/* Collections Table */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.1)', textAlign: 'left', color: 'var(--text-secondary)' }}>
                <th style={{ padding: '0.75rem 1rem' }}>Collection</th>
                <th style={{ padding: '0.75rem 1rem' }}>Target Domain</th>
                <th style={{ padding: '0.75rem 1rem' }}>Document Count</th>
                <th style={{ padding: '0.75rem 1rem' }}>Index Strategy</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Replication Status</th>
              </tr>
            </thead>
            <tbody>
              <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: '#38bdf8' }}>
                  petstore_orders
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-secondary)' }}>
                  Order Aggregate Root & Checkout
                </td>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#ffffff' }}>
                  {diagnostics?.collectionStats?.petstore_orders_count || 18} documents
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  {'{ userId: 1, orderDate: -1 }'}
                </td>
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  <span style={{ color: '#34d399', fontWeight: 600, fontSize: '0.78rem' }}>Synced (rs0)</span>
                </td>
              </tr>
              <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: '#38bdf8' }}>
                  petstore_products
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-secondary)' }}>
                  Product Catalog & Category Aggregates
                </td>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#ffffff' }}>
                  {diagnostics?.collectionStats?.petstore_products_count || 16} documents
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  {'{ categoryId: 1, productId: 1 }'}
                </td>
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  <span style={{ color: '#34d399', fontWeight: 600, fontSize: '0.78rem' }}>Synced (rs0)</span>
                </td>
              </tr>
              <tr>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: '#38bdf8' }}>
                  system.views / metadata
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-secondary)' }}>
                  Replica Set Metadata & OpLog
                </td>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#ffffff' }}>
                  1 view
                </td>
                <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  Internal B-Tree
                </td>
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  <span style={{ color: '#34d399', fontWeight: 600, fontSize: '0.78rem' }}>Synced (rs0)</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
