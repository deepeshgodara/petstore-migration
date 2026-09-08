import React, { useState, useEffect, useMemo } from 'react';
import { AdminAnalyticsResponse, CategorySalesMetric } from '../types/order';
import { orderService } from '../services/orderService';
import {
  DollarSign,
  ShoppingCart,
  Users,
  UserCheck,
  Calendar,
  PieChart as PieChartIcon,
  BarChart3,
  RefreshCw,
  TrendingUp,
  Sparkles,
  ArrowUpRight,
  Filter,
} from 'lucide-react';

const CATEGORY_COLORS: Record<string, { bg: string; fill: string; border: string; glow: string; text: string }> = {
  BIRDS: {
    bg: 'rgba(6, 182, 212, 0.15)',
    fill: '#06b6d4',
    border: '#22d3ee',
    glow: 'rgba(6, 182, 212, 0.4)',
    text: '#67e8f9',
  },
  FISH: {
    bg: 'rgba(16, 185, 129, 0.15)',
    fill: '#10b981',
    border: '#34d399',
    glow: 'rgba(16, 185, 129, 0.4)',
    text: '#6ee7b7',
  },
  DOGS: {
    bg: 'rgba(245, 158, 11, 0.15)',
    fill: '#f59e0b',
    border: '#fbbf24',
    glow: 'rgba(245, 158, 11, 0.4)',
    text: '#fde68a',
  },
  CATS: {
    bg: 'rgba(244, 63, 94, 0.15)',
    fill: '#f43f5e',
    border: '#fb7185',
    glow: 'rgba(244, 63, 94, 0.4)',
    text: '#fda4af',
  },
  REPTILES: {
    bg: 'rgba(168, 85, 247, 0.15)',
    fill: '#a855f7',
    border: '#c084fc',
    glow: 'rgba(168, 85, 247, 0.4)',
    text: '#d8b4fe',
  },
};

const DEFAULT_COLOR = {
  bg: 'rgba(99, 102, 241, 0.15)',
  fill: '#6366f1',
  border: '#818cf8',
  glow: 'rgba(99, 102, 241, 0.4)',
  text: '#a5b4fc',
};

export const AdminAnalyticsView: React.FC = () => {
  const [data, setData] = useState<AdminAnalyticsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [chartType, setChartType] = useState<'donut' | 'bar'>('donut');
  const [datePreset, setDatePreset] = useState<'all' | '30d' | '7d' | 'today' | 'custom'>('all');
  const [customStart, setCustomStart] = useState<string>('');
  const [customEnd, setCustomEnd] = useState<string>('');
  const [hoveredCategory, setHoveredCategory] = useState<CategorySalesMetric | null>(null);

  const fetchAnalytics = async (start?: string, end?: string) => {
    setLoading(true);
    try {
      const resp = await orderService.getAdminAnalytics(start, end);
      setData(resp);
    } catch (err) {
      console.error('Failed to load admin analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    orderService
      .getAdminAnalytics()
      .then((resp) => {
        if (isMounted) {
          setData(resp);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('Failed to load admin analytics:', err);
          setLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const handlePresetSelect = (preset: 'all' | '30d' | '7d' | 'today' | 'custom') => {
    setDatePreset(preset);
    if (preset === 'all') {
      fetchAnalytics();
      return;
    }
    if (preset === 'custom') {
      return;
    }

    const now = new Date();
    const endStr = now.toISOString().split('T')[0];
    let startStr = '';

    if (preset === 'today') {
      startStr = endStr;
    } else if (preset === '7d') {
      const d = new Date(now);
      d.setDate(d.getDate() - 7);
      startStr = d.toISOString().split('T')[0];
    } else if (preset === '30d') {
      const d = new Date(now);
      d.setDate(d.getDate() - 30);
      startStr = d.toISOString().split('T')[0];
    }

    fetchAnalytics(startStr, endStr);
  };

  const handleApplyCustomDate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customStart && !customEnd) {
      fetchAnalytics();
    } else {
      fetchAnalytics(customStart || undefined, customEnd || undefined);
    }
  };

  // SVG Donut Chart Calculation
  const donutArcs = useMemo(() => {
    if (!data || !data.salesByCategory || data.salesByCategory.length === 0) return [];
    const validCategories = data.salesByCategory.filter((c) => c.totalRevenue > 0);
    const totalRev = validCategories.reduce((acc, c) => acc + c.totalRevenue, 0);

    if (totalRev === 0) return [];

    let accumulatedAngle = 0;
    const radius = 80;
    const center = 100;
    const strokeWidth = 28;

    return validCategories.map((cat) => {
      const fraction = cat.totalRevenue / totalRev;
      const angle = fraction * 360;
      const startAngle = accumulatedAngle;
      const endAngle = accumulatedAngle + angle;
      accumulatedAngle = endAngle;

      const startRad = ((startAngle - 90) * Math.PI) / 180;
      const endRad = ((endAngle - 90) * Math.PI) / 180;

      const x1 = center + radius * Math.cos(startRad);
      const y1 = center + radius * Math.sin(startRad);
      const x2 = center + radius * Math.cos(endRad);
      const y2 = center + radius * Math.sin(endRad);

      const largeArcFlag = angle > 180 ? 1 : 0;
      const pathData =
        validCategories.length === 1
          ? `M ${center} ${center - radius} A ${radius} ${radius} 0 1 1 ${center - 0.001} ${center - radius}`
          : `M ${x1} ${y1} A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2}`;

      const color = CATEGORY_COLORS[cat.categoryId.toUpperCase()] || DEFAULT_COLOR;

      return {
        category: cat,
        pathData,
        color,
        fraction,
        strokeWidth,
      };
    });
  }, [data]);

  // Max daily revenue for trend chart scaling
  const maxDailyRev = useMemo(() => {
    if (!data || !data.salesOverTime || data.salesOverTime.length === 0) return 1;
    return Math.max(...data.salesOverTime.map((d) => d.revenue), 100);
  }, [data]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Top Banner & Date Controls */}
      <div
        className="card-glow"
        style={{
          padding: '1.5rem 1.75rem',
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.9) 100%)',
          border: '1px solid rgba(99, 102, 241, 0.25)',
          display: 'flex',
          flexDirection: 'column',
          gap: '1.25rem',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '10px',
                  background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)',
                }}
              >
                <TrendingUp size={20} color="#ffffff" />
              </div>
              <div>
                <h2 style={{ fontSize: '1.4rem', fontWeight: 800, margin: 0 }}>
                  Sales & Customer Analytics
                </h2>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
                  Modern cloud-native telemetry replacing legacy Java Swing (`petstoreadmin.ear`) charts
                </p>
              </div>
            </div>
          </div>

          {/* Preset Buttons */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <div
              style={{
                display: 'flex',
                background: 'rgba(15, 23, 42, 0.8)',
                padding: '0.25rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
                gap: '0.25rem',
              }}
            >
              {(['all', '30d', '7d', 'today', 'custom'] as const).map((p) => {
                const labels: Record<string, string> = {
                  all: 'All Time',
                  '30d': 'Last 30 Days',
                  '7d': 'Last 7 Days',
                  today: 'Today',
                  custom: 'Custom',
                };
                const active = datePreset === p;
                return (
                  <button
                    key={p}
                    type="button"
                    onClick={() => handlePresetSelect(p)}
                    style={{
                      padding: '0.4rem 0.75rem',
                      fontSize: '0.78rem',
                      fontWeight: active ? 700 : 500,
                      borderRadius: 'var(--radius-sm)',
                      background: active ? 'var(--primary)' : 'transparent',
                      color: active ? '#ffffff' : 'var(--text-secondary)',
                      border: 'none',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                    }}
                  >
                    {labels[p]}
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              className="btn-secondary"
              onClick={() => handlePresetSelect(datePreset)}
              disabled={loading}
              style={{ padding: '0.45rem 0.75rem', fontSize: '0.8rem', gap: '0.4rem' }}
              title="Refresh Analytics"
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              <span>Sync</span>
            </button>
          </div>
        </div>

        {/* Custom Date Form (if Custom preset chosen) */}
        {datePreset === 'custom' && (
          <form
            onSubmit={handleApplyCustomDate}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              flexWrap: 'wrap',
              paddingTop: '0.75rem',
              borderTop: '1px solid rgba(255, 255, 255, 0.07)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Calendar size={15} color="var(--text-muted)" />
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>From:</span>
              <input
                type="date"
                value={customStart}
                onChange={(e) => setCustomStart(e.target.value)}
                style={{
                  padding: '0.4rem 0.6rem',
                  fontSize: '0.8rem',
                  background: 'rgba(15, 23, 42, 0.7)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-sm)',
                  color: 'var(--text-primary)',
                }}
              />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>To:</span>
              <input
                type="date"
                value={customEnd}
                onChange={(e) => setCustomEnd(e.target.value)}
                style={{
                  padding: '0.4rem 0.6rem',
                  fontSize: '0.8rem',
                  background: 'rgba(15, 23, 42, 0.7)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-sm)',
                  color: 'var(--text-primary)',
                }}
              />
            </div>
            <button
              type="submit"
              className="btn-primary"
              style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem', gap: '0.4rem' }}
            >
              <Filter size={13} />
              <span>Apply Filter</span>
            </button>
          </form>
        )}
      </div>

      {/* KPI Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* Card 1: Total Revenue */}
        <div
          className="card"
          style={{
            padding: '1.25rem 1.5rem',
            background: 'linear-gradient(145deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Gross Revenue
            </span>
            <div style={{ padding: '0.4rem', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.15)' }}>
              <DollarSign size={18} color="#38bdf8" />
            </div>
          </div>
          <div style={{ fontSize: '1.85rem', fontWeight: 800, marginTop: '0.5rem', color: '#ffffff' }}>
            ${data ? data.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00'}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.4rem', fontSize: '0.78rem', color: '#38bdf8' }}>
            <Sparkles size={13} />
            <span>Across {data ? data.totalOrders : 0} completed & approved orders</span>
          </div>
        </div>

        {/* Card 2: Average Order Value */}
        <div
          className="card"
          style={{
            padding: '1.25rem 1.5rem',
            background: 'linear-gradient(145deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%)',
            border: '1px solid rgba(129, 140, 248, 0.25)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Avg Order Value (AOV)
            </span>
            <div style={{ padding: '0.4rem', borderRadius: '8px', background: 'rgba(129, 140, 248, 0.15)' }}>
              <ShoppingCart size={18} color="#818cf8" />
            </div>
          </div>
          <div style={{ fontSize: '1.85rem', fontWeight: 800, marginTop: '0.5rem', color: '#ffffff' }}>
            ${data ? data.averageOrderValue.toFixed(2) : '0.00'}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.4rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
            <span>Total orders: {data ? data.totalOrders : 0}</span>
          </div>
        </div>

        {/* Card 3: Unique Customers */}
        <div
          className="card"
          style={{
            padding: '1.25rem 1.5rem',
            background: 'linear-gradient(145deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%)',
            border: '1px solid rgba(52, 211, 153, 0.25)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Unique Customers
            </span>
            <div style={{ padding: '0.4rem', borderRadius: '8px', background: 'rgba(52, 211, 153, 0.15)' }}>
              <Users size={18} color="#34d399" />
            </div>
          </div>
          <div style={{ fontSize: '1.85rem', fontWeight: 800, marginTop: '0.5rem', color: '#ffffff' }}>
            {data ? data.uniqueCustomers : 0}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.4rem', fontSize: '0.78rem', color: '#34d399' }}>
            <ArrowUpRight size={13} />
            <span>Active registered & guest buyers</span>
          </div>
        </div>

        {/* Card 4: Returning Customers & Retention */}
        <div
          className="card"
          style={{
            padding: '1.25rem 1.5rem',
            background: 'linear-gradient(145deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%)',
            border: '1px solid rgba(251, 191, 36, 0.25)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Returning Customers
            </span>
            <div style={{ padding: '0.4rem', borderRadius: '8px', background: 'rgba(251, 191, 36, 0.15)' }}>
              <UserCheck size={18} color="#fbbf24" />
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.6rem', marginTop: '0.5rem' }}>
            <span style={{ fontSize: '1.85rem', fontWeight: 800, color: '#ffffff' }}>
              {data ? data.returningCustomers : 0}
            </span>
            <span
              style={{
                fontSize: '0.85rem',
                fontWeight: 700,
                color: '#fbbf24',
                background: 'rgba(251, 191, 36, 0.15)',
                padding: '0.15rem 0.5rem',
                borderRadius: 'var(--radius-full)',
              }}
            >
              {data ? data.repeatCustomerRate : 0}% Repeat Rate
            </span>
          </div>
          <div style={{ marginTop: '0.4rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
            Customers with &gt; 1 placed order
          </div>
        </div>
      </div>

      {/* Main Visual Section: Category Sales Donut & Bar Graph */}
      <div
        className="card"
        style={{
          padding: '1.75rem',
          borderRadius: 'var(--radius-lg)',
          background: 'rgba(15, 23, 42, 0.75)',
          border: '1px solid var(--border-subtle)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
            marginBottom: '1.75rem',
            paddingBottom: '1rem',
            borderBottom: '1px solid rgba(255, 255, 255, 0.07)',
          }}
        >
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>Sales by Pet Category</span>
              <span className="brand-badge" style={{ fontSize: '0.7rem' }}>Swing Chart 2.0</span>
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
              Revenue distribution and volume across pet species
            </p>
          </div>

          {/* Toggle between Donut / Pie vs Bar Chart */}
          <div
            style={{
              display: 'flex',
              background: 'rgba(15, 23, 42, 0.8)',
              padding: '0.25rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              gap: '0.25rem',
            }}
          >
            <button
              type="button"
              onClick={() => setChartType('donut')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                padding: '0.4rem 0.85rem',
                fontSize: '0.8rem',
                fontWeight: chartType === 'donut' ? 700 : 500,
                borderRadius: 'var(--radius-sm)',
                background: chartType === 'donut' ? 'var(--primary)' : 'transparent',
                color: chartType === 'donut' ? '#ffffff' : 'var(--text-secondary)',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <PieChartIcon size={14} />
              <span>Pie / Donut</span>
            </button>
            <button
              type="button"
              onClick={() => setChartType('bar')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                padding: '0.4rem 0.85rem',
                fontSize: '0.8rem',
                fontWeight: chartType === 'bar' ? 700 : 500,
                borderRadius: 'var(--radius-sm)',
                background: chartType === 'bar' ? 'var(--primary)' : 'transparent',
                color: chartType === 'bar' ? '#ffffff' : 'var(--text-secondary)',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <BarChart3 size={14} />
              <span>Bar Graph</span>
            </button>
          </div>
        </div>

        {/* Chart Content Body */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
            <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 1rem auto' }} />
            <p>Aggregating category metrics...</p>
          </div>
        ) : !data || data.salesByCategory.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
            <p>No order transactions found for the selected date range.</p>
          </div>
        ) : chartType === 'donut' ? (
          /* Interactive SVG Donut View */
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: '2.5rem',
              alignItems: 'center',
            }}
          >
            {/* Donut SVG Canvas */}
            <div style={{ display: 'flex', justifyContent: 'center', position: 'relative' }}>
              <svg viewBox="0 0 200 200" width="280" height="280" style={{ transform: 'rotate(-90deg)', overflow: 'visible' }}>
                <circle cx="100" cy="100" r="80" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="28" />
                {donutArcs.map((arc) => {
                  const isHovered = hoveredCategory?.categoryId === arc.category.categoryId;
                  return (
                    <path
                      key={arc.category.categoryId}
                      d={arc.pathData}
                      fill="none"
                      stroke={arc.color.fill}
                      strokeWidth={isHovered ? arc.strokeWidth + 6 : arc.strokeWidth}
                      strokeLinecap="round"
                      style={{
                        cursor: 'pointer',
                        transition: 'all 0.25s ease',
                        filter: isHovered ? `drop-shadow(0 0 10px ${arc.color.glow})` : 'none',
                      }}
                      onMouseEnter={() => setHoveredCategory(arc.category)}
                      onMouseLeave={() => setHoveredCategory(null)}
                    />
                  );
                })}
              </svg>

              {/* Central Hole Metric Overlay */}
              <div
                style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  textAlign: 'center',
                  pointerEvents: 'none',
                  width: '120px',
                }}
              >
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  {hoveredCategory ? hoveredCategory.categoryName : 'Total Sales'}
                </div>
                <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#ffffff', marginTop: '0.2rem' }}>
                  ${hoveredCategory ? hoveredCategory.totalRevenue.toFixed(0) : data.totalRevenue.toFixed(0)}
                </div>
                <div style={{ fontSize: '0.75rem', color: hoveredCategory ? '#38bdf8' : 'var(--text-secondary)', fontWeight: 600 }}>
                  {hoveredCategory ? `${hoveredCategory.percentageShare}% Share` : `${data.totalOrders} Orders`}
                </div>
              </div>
            </div>

            {/* Category Breakdown & Legend */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {data.salesByCategory.map((cat) => {
                const color = CATEGORY_COLORS[cat.categoryId.toUpperCase()] || DEFAULT_COLOR;
                const isHovered = hoveredCategory?.categoryId === cat.categoryId;
                return (
                  <div
                    key={cat.categoryId}
                    onMouseEnter={() => setHoveredCategory(cat)}
                    onMouseLeave={() => setHoveredCategory(null)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.75rem 1rem',
                      borderRadius: 'var(--radius-md)',
                      background: isHovered ? color.bg : 'rgba(255, 255, 255, 0.02)',
                      border: `1px solid ${isHovered ? color.border : 'rgba(255, 255, 255, 0.05)'}`,
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      boxShadow: isHovered ? `0 4px 15px ${color.glow}` : 'none',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <div
                        style={{
                          width: '12px',
                          height: '12px',
                          borderRadius: '3px',
                          background: color.fill,
                          boxShadow: `0 0 8px ${color.glow}`,
                        }}
                      />
                      <div>
                        <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#ffffff' }}>
                          {cat.categoryName}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                          {cat.unitsSold} units sold ({cat.orderCount} orders)
                        </div>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: color.text }}>
                        ${cat.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {cat.percentageShare}% of total
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          /* Modern Bar Graph View */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {data.salesByCategory.map((cat) => {
              const color = CATEGORY_COLORS[cat.categoryId.toUpperCase()] || DEFAULT_COLOR;
              const barPct = data.totalRevenue > 0 ? (cat.totalRevenue / data.totalRevenue) * 100 : 0;
              return (
                <div key={cat.categoryId} style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ fontWeight: 700, color: '#ffffff' }}>{cat.categoryName}</span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        ({cat.unitsSold} items sold in {cat.orderCount} orders)
                      </span>
                    </div>
                    <div style={{ fontWeight: 700, color: color.text }}>
                      ${cat.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ({cat.percentageShare}%)
                    </div>
                  </div>

                  {/* Visual Bar Track */}
                  <div
                    style={{
                      height: '14px',
                      background: 'rgba(255, 255, 255, 0.05)',
                      borderRadius: 'var(--radius-full)',
                      overflow: 'hidden',
                      position: 'relative',
                    }}
                  >
                    <div
                      style={{
                        height: '100%',
                        width: `${Math.max(barPct, cat.totalRevenue > 0 ? 3 : 0)}%`,
                        background: `linear-gradient(90deg, ${color.fill} 0%, ${color.border} 100%)`,
                        borderRadius: 'var(--radius-full)',
                        boxShadow: `0 0 10px ${color.glow}`,
                        transition: 'width 0.6s cubic-bezier(0.16, 1, 0.3, 1)',
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Daily Sales Over Time Trend Card */}
      {data && data.salesOverTime && data.salesOverTime.length > 0 && (
        <div
          className="card"
          style={{
            padding: '1.75rem',
            borderRadius: 'var(--radius-lg)',
            background: 'rgba(15, 23, 42, 0.75)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0 }}>
              Daily Revenue Trend
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
              Historical daily sales timeline
            </p>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'flex-end',
              gap: '1rem',
              height: '180px',
              paddingTop: '1.5rem',
              paddingBottom: '0.5rem',
              borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
              overflowX: 'auto',
            }}
          >
            {data.salesOverTime.map((d) => {
              const heightPct = Math.min(100, Math.max(10, (d.revenue / maxDailyRev) * 100));
              return (
                <div
                  key={d.date}
                  style={{
                    flex: 1,
                    minWidth: '60px',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    height: '100%',
                    justifyContent: 'flex-end',
                    gap: '0.5rem',
                  }}
                  title={`${d.date}: $${d.revenue.toFixed(2)} (${d.orderCount} orders)`}
                >
                  <div style={{ fontSize: '0.7rem', fontWeight: 700, color: '#38bdf8' }}>
                    ${d.revenue > 999 ? `${(d.revenue / 1000).toFixed(1)}k` : d.revenue.toFixed(0)}
                  </div>
                  <div
                    style={{
                      width: '100%',
                      maxWidth: '36px',
                      height: `${heightPct}%`,
                      background: 'linear-gradient(180deg, #38bdf8 0%, #0284c7 100%)',
                      borderRadius: '4px 4px 0 0',
                      boxShadow: '0 0 10px rgba(56, 189, 248, 0.3)',
                      transition: 'height 0.4s ease',
                    }}
                  />
                  <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                    {d.date.slice(5)}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
