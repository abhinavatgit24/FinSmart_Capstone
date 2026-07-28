import { useState } from 'react'
import { useBudgets } from '../../hooks/useBudgets'
import { CATEGORIES, getCategoryIcon, getCategoryColor } from '../../utils/categories'
import { formatCurrency } from '../../utils/formatCurrency'
import {
  Plus, Trash2, AlertTriangle, CheckCircle2,
  XCircle, RefreshCw, X, ChevronDown
} from 'lucide-react'

function Skeleton({ className = '' }) {
  return <div className={`bg-surface-200 rounded-xl animate-pulse ${className}`} />
}

// ── Utilisation bar ───────────────────────────────────────────────────────────
function UtilBar({ pct, alertLevel }) {
  const clamped = Math.min(pct, 100)
  const color =
    alertLevel === 'exceeded' ? '#f04438' :
    alertLevel === 'warning'  ? '#f59e0b' : '#12b76a'

  return (
    <div className="w-full bg-surface-200 rounded-full h-2 overflow-hidden">
      <div
        className="h-2 rounded-full transition-all duration-500"
        style={{ width: `${clamped}%`, background: color }}
      />
    </div>
  )
}

// ── Alert badge ───────────────────────────────────────────────────────────────
function AlertBadge({ level }) {
  if (level === 'none') return (
    <span className="flex items-center gap-1 text-xs text-emerald-600 font-medium">
      <CheckCircle2 size={12} /> On track
    </span>
  )
  if (level === 'warning') return (
    <span className="flex items-center gap-1 text-xs text-amber-600 font-medium">
      <AlertTriangle size={12} /> 80% reached
    </span>
  )
  return (
    <span className="flex items-center gap-1 text-xs text-red-600 font-medium">
      <XCircle size={12} /> Limit exceeded
    </span>
  )
}

// ── Add / Edit modal ──────────────────────────────────────────────────────────
function BudgetModal({ onSave, onClose }) {
  const [form, setForm]       = useState({ category: '', limitAmount: '', period: 'monthly' })
  const [errors, setErrors]   = useState({})
  const [saving, setSaving]   = useState(false)

  const set = (k, v) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => ({ ...e, [k]: null })) }

  const validate = () => {
    const e = {}
    if (!form.category)                          e.category    = 'Select a category'
    if (!form.limitAmount || form.limitAmount <= 0) e.limitAmount = 'Enter a valid amount'
    return e
  }

  const handleSubmit = async () => {
    const e = validate()
    if (Object.keys(e).length) { setErrors(e); return }
    setSaving(true)
    try {
      await onSave({ ...form, limitAmount: Number(form.limitAmount) })
      onClose()
    } catch { /* toast shown by hook */ }
    finally { setSaving(false) }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-sm bg-white rounded-3xl shadow-card-lg animate-slide-up">
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-surface-100">
          <h2 className="text-base font-semibold text-ink-900">Set Budget</h2>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-700"><X size={18} /></button>
        </div>

        <div className="p-6 space-y-4">
          {/* Category */}
          <div>
            <label className="label">Category</label>
            <div className="relative">
              <select
                value={form.category}
                onChange={e => set('category', e.target.value)}
                className={`input appearance-none pr-8 ${errors.category ? 'border-red-400' : ''}`}
              >
                <option value="">Choose category…</option>
                {CATEGORIES.filter(c => c !== 'Salary').map(c => (
                  <option key={c} value={c}>{getCategoryIcon(c)} {c}</option>
                ))}
              </select>
              <ChevronDown size={14} className="absolute right-3 top-3.5 text-ink-400 pointer-events-none" />
            </div>
            {errors.category && <p className="text-xs text-red-500 mt-1">{errors.category}</p>}
          </div>

          {/* Period */}
          <div>
            <label className="label">Period</label>
            <div className="flex gap-2">
              {['monthly', 'weekly'].map(p => (
                <button
                  key={p} type="button"
                  onClick={() => set('period', p)}
                  className={`flex-1 py-2.5 rounded-xl text-sm font-medium border transition-all ${
                    form.period === p
                      ? 'bg-brand-50 text-brand-700 border-brand-300'
                      : 'bg-white text-ink-400 border-surface-300 hover:bg-surface-50'
                  }`}
                >
                  {p.charAt(0).toUpperCase() + p.slice(1)}
                </button>
              ))}
            </div>
          </div>

          {/* Limit */}
          <div>
            <label className="label">Limit Amount (₹)</label>
            <input
              type="number" min="1" placeholder="e.g. 5000"
              value={form.limitAmount}
              onChange={e => set('limitAmount', e.target.value)}
              className={`input ${errors.limitAmount ? 'border-red-400' : ''}`}
            />
            {errors.limitAmount && <p className="text-xs text-red-500 mt-1">{errors.limitAmount}</p>}
          </div>

          <div className="flex gap-3 pt-2">
            <button onClick={onClose} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleSubmit} disabled={saving} className="btn-primary flex-1">
              {saving ? 'Saving…' : 'Save Budget'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function BudgetsPage() {
  const { utilisations, isLoading, fetchAll, saveBudget, deleteBudget } = useBudgets()
  const [showModal, setShowModal] = useState(false)

  const alerts = utilisations.filter(u => u.alertLevel !== 'none')

  if (isLoading) return (
    <div className="space-y-4">
      {[1,2,3].map(i => <Skeleton key={i} className="h-28" />)}
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">Budgets</h2>
          <p className="text-sm text-ink-400 mt-0.5">Set limits and track spending in real time</p>
        </div>
        <div className="flex gap-2">
          <button onClick={fetchAll} className="btn-ghost flex items-center gap-2 text-sm">
            <RefreshCw size={14} /> Refresh
          </button>
          <button onClick={() => setShowModal(true)} className="btn-primary flex items-center gap-2 text-sm">
            <Plus size={15} /> Add Budget
          </button>
        </div>
      </div>

      {/* Alert banner */}
      {alerts.length > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 space-y-1">
          <p className="text-sm font-semibold text-amber-800 flex items-center gap-2">
            <AlertTriangle size={15} /> Budget Alerts ({alerts.length})
          </p>
          <ul className="text-sm text-amber-700 space-y-0.5 pl-5 list-disc">
            {alerts.map(a => (
              <li key={a.id}>
                <strong>{a.category}</strong> ({a.period}): {a.utilisationPct.toFixed(0)}% used
                {a.alertLevel === 'exceeded' ? ' — limit exceeded!' : ' — approaching limit'}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Budget cards */}
      {utilisations.length === 0 ? (
        <div className="card py-16 text-center">
          <p className="text-4xl mb-3">💰</p>
          <p className="text-sm font-medium text-ink-700">No budgets set yet</p>
          <p className="text-sm text-ink-400 mt-1">Click "Add Budget" to start tracking your spending limits.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {utilisations.map(u => (
            <div
              key={u.id}
              className={`card relative transition-all ${
                u.alertLevel === 'exceeded' ? 'border-red-200 bg-red-50/30' :
                u.alertLevel === 'warning'  ? 'border-amber-200 bg-amber-50/30' : ''
              }`}
            >
              {/* Delete */}
              <button
                onClick={() => deleteBudget(u.id)}
                className="absolute top-4 right-4 text-ink-300 hover:text-red-500 transition-colors"
              >
                <Trash2 size={14} />
              </button>

              {/* Category header */}
              <div className="flex items-center gap-3 mb-4">
                <div
                  className="w-10 h-10 rounded-xl flex items-center justify-center text-lg flex-shrink-0"
                  style={{ background: getCategoryColor(u.category) + '18' }}
                >
                  {getCategoryIcon(u.category)}
                </div>
                <div>
                  <p className="text-sm font-semibold text-ink-900">{u.category}</p>
                  <p className="text-xs text-ink-400 capitalize">{u.period}</p>
                </div>
              </div>

              {/* Amounts */}
              <div className="flex items-end justify-between mb-2">
                <div>
                  <p className="text-xs text-ink-400 mb-0.5">Spent</p>
                  <p className="text-xl font-semibold text-ink-900">{formatCurrency(u.spent)}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-ink-400 mb-0.5">Limit</p>
                  <p className="text-sm font-medium text-ink-500">{formatCurrency(u.limitAmount)}</p>
                </div>
              </div>

              {/* Bar */}
              <UtilBar pct={u.utilisationPct} alertLevel={u.alertLevel} />

              <div className="flex items-center justify-between mt-2">
                <AlertBadge level={u.alertLevel} />
                <span className="text-xs text-ink-400">{u.utilisationPct.toFixed(1)}% used</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <BudgetModal
          onSave={saveBudget}
          onClose={() => setShowModal(false)}
        />
      )}
    </div>
  )
}
