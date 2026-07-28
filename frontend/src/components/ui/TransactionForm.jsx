import { useState, useEffect } from 'react'
import { X, Sparkles } from 'lucide-react'
import { CATEGORIES, autoDetectCategory } from '../../utils/categories'

const EMPTY = {
  amount: '', type: 'expense', category: '', description: '',
  date: new Date().toISOString().split('T')[0]
}

export function TransactionForm({ onSubmit, onCancel, initial = null }) {
  const [form, setForm]           = useState(initial ? {
    amount:      initial.amount ?? '',
    type:        initial.type ?? 'expense',
    category:    initial.category ?? '',
    description: initial.description ?? '',
    date:        initial.date ?? EMPTY.date,
  } : EMPTY)
  const [errors, setErrors]       = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [autoCategory, setAutoCategory] = useState(null)

  // Auto-detect category as user types description
  useEffect(() => {
    if (!form.category) {
      const detected = autoDetectCategory(form.description)
      setAutoCategory(detected)
    } else {
      setAutoCategory(null)
    }
  }, [form.description, form.category])

  const set = (k, v) => {
    setForm(f => ({ ...f, [k]: v }))
    if (errors[k]) setErrors(e => ({ ...e, [k]: null }))
  }

  const validate = () => {
    const e = {}
    if (!form.amount || Number(form.amount) <= 0) e.amount = 'Enter a valid amount'
    if (!form.type)    e.type    = 'Type is required'
    if (!form.date)    e.date    = 'Date is required'
    return e
  }

  const handleSubmit = async (ev) => {
    ev.preventDefault()
    const e = validate()
    if (Object.keys(e).length) { setErrors(e); return }
    setSubmitting(true)
    try {
      await onSubmit({
        amount:      Number(form.amount),
        type:        form.type,
        category:    form.category || autoCategory || 'Other',
        description: form.description,
        date:        form.date,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-card-lg animate-slide-up">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-surface-100">
          <h2 className="text-base font-semibold text-ink-900">
            {initial ? 'Edit Transaction' : 'Add Transaction'}
          </h2>
          <button onClick={onCancel} className="text-ink-400 hover:text-ink-700 transition-colors">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Type toggle */}
          <div>
            <label className="label">Type</label>
            <div className="flex gap-2">
              {['expense', 'income'].map(t => (
                <button
                  key={t} type="button"
                  onClick={() => set('type', t)}
                  className={`flex-1 py-2.5 rounded-xl text-sm font-medium transition-all border ${
                    form.type === t
                      ? t === 'expense'
                        ? 'bg-red-50 text-red-600 border-red-200'
                        : 'bg-emerald-50 text-emerald-600 border-emerald-200'
                      : 'bg-white text-ink-400 border-surface-300 hover:bg-surface-50'
                  }`}
                >
                  {t === 'expense' ? '📤 Expense' : '📥 Income'}
                </button>
              ))}
            </div>
          </div>

          {/* Amount */}
          <div>
            <label className="label">Amount (₹)</label>
            <input
              type="number" step="0.01" placeholder="0.00"
              value={form.amount}
              onChange={e => set('amount', e.target.value)}
              className={`input ${errors.amount ? 'border-red-400 focus:ring-red-400' : ''}`}
            />
            {errors.amount && <p className="text-xs text-red-500 mt-1">{errors.amount}</p>}
          </div>

          {/* Description */}
          <div>
            <label className="label">Description</label>
            <input
              type="text" placeholder="e.g. Swiggy lunch order"
              value={form.description}
              onChange={e => set('description', e.target.value)}
              className="input"
            />
            {autoCategory && !form.category && (
              <p className="flex items-center gap-1.5 text-xs text-brand-600 mt-1.5">
                <Sparkles size={11} />
                Auto-detected: <strong>{autoCategory}</strong>
              </p>
            )}
          </div>

          {/* Category */}
          <div>
            <label className="label">Category</label>
            <select
              value={form.category}
              onChange={e => set('category', e.target.value)}
              className="input"
            >
              <option value="">Auto-detect from description</option>
              {CATEGORIES.map(c => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          {/* Date */}
          <div>
            <label className="label">Date</label>
            <input
              type="date"
              value={form.date}
              onChange={e => set('date', e.target.value)}
              className={`input ${errors.date ? 'border-red-400 focus:ring-red-400' : ''}`}
            />
            {errors.date && <p className="text-xs text-red-500 mt-1">{errors.date}</p>}
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onCancel} className="btn-secondary flex-1">
              Cancel
            </button>
            <button type="submit" disabled={submitting} className="btn-primary flex-1">
              {submitting ? 'Saving...' : initial ? 'Save Changes' : 'Add Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
