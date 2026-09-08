import React, { useState, useEffect, useMemo } from 'react';
import { Item } from '../../types/catalog';
import { supplierService } from '../../services/supplierService';
import {
  Truck,
  Package,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Search,
  DollarSign,
  Save,
  Plus,
  Minus,
  Layers,
  Check,
} from 'lucide-react';
import { getProductImageUrl } from '../../utils/imageUtils';
import { formatCurrency } from '../../utils/currencyUtils';

export const SupplierInventoryPage: React.FC = () => {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [stockFilter, setStockFilter] = useState<'ALL' | 'HEALTHY' | 'LOW' | 'DEPLETED'>('ALL');
  
  // Track draft quantities for editing before saving: { [itemId]: number }
  const [draftQuantities, setDraftQuantities] = useState<Record<string, number>>({});
  const [savingItemId, setSavingItemId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const data = await supplierService.getAllItems();
      setItems(data);
      // Initialize draft quantities
      const initialDrafts: Record<string, number> = {};
      data.forEach((item) => {
        initialDrafts[item.itemId] = item.inventoryQuantity;
      });
      setDraftQuantities(initialDrafts);
    } catch (err: unknown) {
      console.error('Failed to fetch inventory items', err);
      const msg = err instanceof Error ? err.message : 'Error fetching inventory';
      setToastMessage({ type: 'error', text: msg });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    supplierService
      .getAllItems()
      .then((data) => {
        if (!active) return;
        setItems(data);
        const initialDrafts: Record<string, number> = {};
        data.forEach((item) => {
          initialDrafts[item.itemId] = item.inventoryQuantity;
        });
        setDraftQuantities(initialDrafts);
      })
      .catch((err: unknown) => {
        if (!active) return;
        console.error('Failed to fetch inventory items', err);
        const msg = err instanceof Error ? err.message : 'Error fetching inventory';
        setToastMessage({ type: 'error', text: msg });
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const handleQuantityChange = (itemId: string, newQty: number) => {
    const validQty = Math.max(0, newQty);
    setDraftQuantities((prev) => ({
      ...prev,
      [itemId]: validQty,
    }));
  };

  const handleSaveStock = async (itemId: string) => {
    const qty = draftQuantities[itemId];
    if (qty === undefined || qty < 0) return;

    setSavingItemId(itemId);
    try {
      const updated = await supplierService.updateInventory(itemId, qty);
      // Update local item list with persisted quantity
      setItems((prev) =>
        prev.map((it) => (it.itemId === itemId ? { ...it, inventoryQuantity: updated.inventoryQuantity } : it))
      );
      setToastMessage({
        type: 'success',
        text: `Stock for [${itemId} - ${updated.productName}] successfully updated to ${updated.inventoryQuantity.toLocaleString()} units`,
      });
      setTimeout(() => setToastMessage(null), 3500);
    } catch (err: unknown) {
      console.error(`Failed to update stock for ${itemId}`, err);
      const msg = err instanceof Error ? err.message : 'Failed to update stock';
      setToastMessage({ type: 'error', text: msg });
    } finally {
      setSavingItemId(null);
    }
  };

  // Derive KPIs
  const totalSkus = items.length;
  const totalUnits = useMemo(() => items.reduce((sum, it) => sum + it.inventoryQuantity, 0), [items]);
  const totalValuation = useMemo(() => items.reduce((sum, it) => sum + (it.inventoryQuantity * Number(it.unitCost || 0)), 0), [items]);
  const lowStockCount = useMemo(() => items.filter((it) => it.inventoryQuantity < 500).length, [items]);

  // Categories list
  const categories = ['ALL', 'FISH', 'DOGS', 'REPTILES', 'CATS', 'BIRDS'];

  // Filter items
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // Category filter
      if (categoryFilter !== 'ALL') {
        const prodPrefix = item.productId.substring(0, 2).toUpperCase();
        const catMap: Record<string, string> = {
          FI: 'FISH',
          K9: 'DOGS',
          RP: 'REPTILES',
          FL: 'CATS',
          AV: 'BIRDS',
        };
        if (catMap[prodPrefix] !== categoryFilter) {
          return false;
        }
      }

      // Stock status filter
      if (stockFilter === 'LOW' && item.inventoryQuantity >= 500) return false;
      if (stockFilter === 'DEPLETED' && item.inventoryQuantity >= 100) return false;
      if (stockFilter === 'HEALTHY' && item.inventoryQuantity < 1000) return false;

      // Keyword query
      if (searchQuery.trim().length > 0) {
        const q = searchQuery.toLowerCase().trim();
        const matchesSku = item.itemId.toLowerCase().includes(q);
        const matchesProduct = item.productName.toLowerCase().includes(q);
        const matchesAttr = (item.attribute || '').toLowerCase().includes(q);
        const matchesLocalizedAttrs =
          item.attributes &&
          Object.values(item.attributes).some((attr) => attr.toLowerCase().includes(q));
        if (!matchesSku && !matchesProduct && !matchesAttr && !matchesLocalizedAttrs) return false;
      }

      return true;
    });
  }, [items, categoryFilter, stockFilter, searchQuery]);

  return (
    <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '2rem 1.5rem' }}>
      {/* Toast Feedback */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: '2rem',
            right: '2rem',
            zIndex: 1000,
            padding: '0.85rem 1.25rem',
            borderRadius: 'var(--radius-md)',
            background: toastMessage.type === 'success' ? '#065f46' : '#881337',
            border: `1px solid ${toastMessage.type === 'success' ? '#10b981' : '#f43f5e'}`,
            color: '#ffffff',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            fontSize: '0.875rem',
            fontWeight: 500,
            animation: 'fadeIn 0.2s ease-out',
          }}
        >
          {toastMessage.type === 'success' ? <CheckCircle2 size={18} color="#34d399" /> : <AlertTriangle size={18} color="#fda4af" />}
          <span>{toastMessage.text}</span>
        </div>
      )}

      {/* Page Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.4rem' }}>
            <div style={{ padding: '0.5rem', borderRadius: 'var(--radius-md)', background: 'rgba(245, 158, 11, 0.15)', border: '1px solid rgba(245, 158, 11, 0.35)' }}>
              <Truck size={24} color="#f59e0b" />
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Supplier & Inventory Management</h1>
            <span className="brand-badge" style={{ borderColor: 'rgba(245, 158, 11, 0.4)', color: '#f59e0b', fontSize: '0.75rem' }}>
              supplier.ear Modernization
            </span>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            Direct inventory control console replacing legacy JSP <code style={{ color: '#f59e0b' }}>/supplier/displayinventory.jsp</code>. Update stock levels with real-time MongoDB persistence.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchInventory}
          className="btn-secondary"
          disabled={loading}
          style={{ gap: '0.5rem', padding: '0.6rem 1rem' }}
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          <span>Refresh Stock</span>
        </button>
      </div>

      {/* KPI Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Tracked SKUs</span>
            <Package size={18} color="#38bdf8" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>{totalSkus}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Catalog line items</span>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Total In-Stock Units</span>
            <Layers size={18} color="#10b981" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--accent-emerald)' }}>
            {totalUnits.toLocaleString()}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Aggregate warehouse stock</span>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Inventory Valuation</span>
            <DollarSign size={18} color="#a855f7" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#c084fc' }}>
            ${totalValuation.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Based on wholesale unit cost</span>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Low Stock Alerts</span>
            <AlertTriangle size={18} color={lowStockCount > 0 ? '#f43f5e' : '#10b981'} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: lowStockCount > 0 ? '#fb7185' : 'var(--text-primary)' }}>
            {lowStockCount}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Items below 500 threshold</span>
        </div>
      </div>

      {/* Filter & Search Toolbar */}
      <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          {/* Category Filter Pills */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setCategoryFilter(cat)}
                className={`tab-btn ${categoryFilter === cat ? 'active' : ''}`}
                style={{ fontSize: '0.8rem', padding: '0.35rem 0.8rem' }}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div style={{ position: 'relative', minWidth: '280px' }}>
            <Search size={15} style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Filter by SKU or Pet Breed..."
              className="search-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ borderRadius: 'var(--radius-sm)', paddingLeft: '2.2rem', fontSize: '0.85rem' }}
            />
          </div>
        </div>

        {/* Status filter bar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.85rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}>
          <span style={{ color: 'var(--text-muted)', marginRight: '0.4rem' }}>Stock Status:</span>
          {(['ALL', 'HEALTHY', 'LOW', 'DEPLETED'] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStockFilter(st)}
              className="btn-secondary"
              style={{
                padding: '0.25rem 0.65rem',
                fontSize: '0.75rem',
                borderColor: stockFilter === st ? '#f59e0b' : 'var(--border-subtle)',
                color: stockFilter === st ? '#f59e0b' : 'var(--text-secondary)',
              }}
            >
              {st === 'ALL' ? 'All Items' : st === 'HEALTHY' ? 'Healthy (>1,000)' : st === 'LOW' ? 'Low Stock (<500)' : 'Critical (<100)'}
            </button>
          ))}
          <span style={{ marginLeft: 'auto', color: 'var(--text-muted)' }}>
            Showing {filteredItems.length} of {items.length} items
          </span>
        </div>
      </div>

      {/* Inventory Table */}
      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ background: 'rgba(255, 255, 255, 0.02)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                <th style={{ padding: '1rem' }}>Pet Item / SKU</th>
                <th style={{ padding: '1rem' }}>Product & Attributes</th>
                <th style={{ padding: '1rem' }}>Pricing</th>
                <th style={{ padding: '1rem' }}>Current Stock</th>
                <th style={{ padding: '1rem', textAlign: 'right' }}>Stock Level Adjuster</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => {
                const draftQty = draftQuantities[item.itemId] ?? item.inventoryQuantity;
                const isModified = draftQty !== item.inventoryQuantity;
                const isSaving = savingItemId === item.itemId;

                const petImage = getProductImageUrl({ id: item.productId }, item);

                const isLow = item.inventoryQuantity < 500;
                const isCritical = item.inventoryQuantity < 100;

                return (
                  <tr
                    key={item.itemId}
                    style={{
                      borderBottom: '1px solid var(--border-subtle)',
                      transition: 'background 0.15s ease',
                      background: isModified ? 'rgba(245, 158, 11, 0.03)' : 'transparent',
                    }}
                  >
                    {/* Item SKU & Image */}
                    <td style={{ padding: '1rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                        <img
                          src={petImage}
                          alt={item.productName}
                          style={{
                            width: '44px',
                            height: '44px',
                            objectFit: 'contain',
                            borderRadius: 'var(--radius-sm)',
                            background: 'rgba(15, 23, 42, 0.6)',
                            padding: '3px',
                            border: '1px solid var(--border-subtle)',
                          }}
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = '/images/banner_logo.gif';
                          }}
                        />
                        <div>
                          <span style={{ fontWeight: 700, color: 'var(--accent-cyan)', fontFamily: 'monospace' }}>
                            {item.itemId}
                          </span>
                          <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            Product: {item.productId}
                          </span>
                        </div>
                      </div>
                    </td>

                    {/* Product Name & Attributes */}
                    <td style={{ padding: '1rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{item.productName}</span>
                        {item.attribute && (
                          <span
                            className="brand-badge"
                            style={{
                              fontSize: '0.68rem',
                              padding: '0.12rem 0.45rem',
                              background: 'rgba(99, 102, 241, 0.15)',
                              color: '#a5b4fc',
                              borderColor: 'rgba(99, 102, 241, 0.35)',
                            }}
                          >
                            Variant: {item.attribute}
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                        {item.description || 'Verified Breed Spec'}
                      </div>
                    </td>

                    {/* Pricing */}
                    <td style={{ padding: '1rem' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                        Retail: {formatCurrency(item.listPrice, 'en_US')}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Wholesale: {formatCurrency(item.unitCost, 'en_US')}
                      </div>
                    </td>

                    {/* Current Stock */}
                    <td style={{ padding: '1rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                        <span style={{ fontWeight: 700, fontSize: '1rem', color: isCritical ? '#f43f5e' : isLow ? '#f59e0b' : 'var(--accent-emerald)' }}>
                          {item.inventoryQuantity.toLocaleString()}
                        </span>
                        <span
                          className="brand-badge"
                          style={{
                            fontSize: '0.65rem',
                            padding: '0.15rem 0.45rem',
                            borderColor: isCritical ? 'rgba(244,63,94,0.4)' : isLow ? 'rgba(245,158,11,0.4)' : 'rgba(16,185,129,0.4)',
                            color: isCritical ? '#fb7185' : isLow ? '#f59e0b' : '#34d399',
                          }}
                        >
                          {isCritical ? 'CRITICAL' : isLow ? 'LOW STOCK' : 'IN STOCK'}
                        </span>
                      </div>
                      {/* Stock Health Bar */}
                      <div style={{ width: '120px', height: '5px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div
                          style={{
                            width: `${Math.min(100, Math.max(5, (item.inventoryQuantity / 10000) * 100))}%`,
                            height: '100%',
                            background: isCritical ? '#f43f5e' : isLow ? '#f59e0b' : '#10b981',
                            borderRadius: '3px',
                          }}
                        />
                      </div>
                    </td>

                    {/* Stock Adjuster */}
                    <td style={{ padding: '1rem', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        {/* Quick -10 button */}
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => handleQuantityChange(item.itemId, draftQty - 10)}
                          style={{ padding: '0.35rem 0.5rem', fontSize: '0.75rem' }}
                          title="Decrease by 10"
                        >
                          -10
                        </button>

                        {/* Decrement by 1 */}
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => handleQuantityChange(item.itemId, draftQty - 1)}
                          style={{ padding: '0.35rem 0.5rem' }}
                          title="Decrease by 1"
                        >
                          <Minus size={13} />
                        </button>

                        {/* Numeric input */}
                        <input
                          type="number"
                          min="0"
                          value={draftQty}
                          onChange={(e) => handleQuantityChange(item.itemId, parseInt(e.target.value, 10) || 0)}
                          style={{
                            width: '80px',
                            padding: '0.35rem 0.5rem',
                            borderRadius: 'var(--radius-sm)',
                            background: 'rgba(15, 23, 42, 0.8)',
                            border: isModified ? '1px solid #f59e0b' : '1px solid var(--border-subtle)',
                            color: isModified ? '#f59e0b' : 'var(--text-primary)',
                            textAlign: 'center',
                            fontSize: '0.85rem',
                            fontWeight: 700,
                          }}
                        />

                        {/* Increment by 1 */}
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => handleQuantityChange(item.itemId, draftQty + 1)}
                          style={{ padding: '0.35rem 0.5rem' }}
                          title="Increase by 1"
                        >
                          <Plus size={13} />
                        </button>

                        {/* Quick +10 button */}
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => handleQuantityChange(item.itemId, draftQty + 10)}
                          style={{ padding: '0.35rem 0.5rem', fontSize: '0.75rem' }}
                          title="Increase by 10"
                        >
                          +10
                        </button>

                        {/* Save Button */}
                        <button
                          type="button"
                          className={isModified ? 'btn-primary' : 'btn-secondary'}
                          disabled={!isModified || isSaving}
                          onClick={() => handleSaveStock(item.itemId)}
                          style={{
                            padding: '0.35rem 0.75rem',
                            fontSize: '0.75rem',
                            gap: '0.35rem',
                            background: isModified ? 'linear-gradient(135deg, #d97706 0%, #b45309 100%)' : undefined,
                            borderColor: isModified ? '#f59e0b' : undefined,
                          }}
                        >
                          {isSaving ? (
                            <RefreshCw size={13} className="animate-spin" />
                          ) : isModified ? (
                            <Save size={13} />
                          ) : (
                            <Check size={13} color="var(--text-muted)" />
                          )}
                          <span>{isSaving ? 'Saving...' : isModified ? 'Save' : 'Synced'}</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}

              {filteredItems.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    No inventory items found matching the selected filter criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
