import { useState } from 'react'
import { useGoals } from '../../hooks/useGoals'
import { formatCurrency } from '../../utils/formatCurrency'
import {
  Plus, Trash2, Pencil, RefreshCw, X,
  Target, CalendarDays, TrendingUp, CheckCircle2, AlertTriangle
} from 'lucide-react'

function Skeleton({ className = '' }) {
  return <div className={`bg-surface-200 rounded-xl animate-pulse ${className}`} />
}

// ── Progress ring (SVG) ───────────────────────────────────────────────────────
function ProgressRing({ pct, size = 72, stroke = 6, color = '#444ce7' }) {
  const r      = (size - stroke) / 2
  const circum = 2 * Math.PI * r
  const offset = circum - (Math.min(pct, 100) / 100) * circum

  return (
    <svg width={size} height={size} className="flex-shrink-0 -rotate-90">
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="#efefef" strokeWidth={stroke} />
      <circle
        cx={size/2} cy={size/2} r={r} fill="none"
        stroke={color} strokeWidth={stroke}
        strokeDasharray={circum} strokeDashoffset={offset}
        strokeLinecap="round"
        style={{ transition: 'stroke-dashoffset 0.6s ease' }}
      />
    </svg>
  )
}

// ── Goal form modal ───────────────────────────────────────────────────────────
const minDeadline = () => {
  const d = new Date(); d.setDate(d.getDate() + 1)
  return d.toISOString().split('T')[0]
}

function GoalModal({ initial = null, onSave, onClose }) {
  const isEdit = !!initial
  const [form, setForm]     = useState({
    name:         initial?.name         ?? '',
    targetAmount: initial?.targetAmount ?? '',
    savedAmount:  initial?.savedAmount  ?? '',
    deadline:     initial?.deadline     ?? '',
  })
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const set = (k, v) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => ({ ...e, [k]: null })) }

  const validate = () => {
    const e = {}
    if (!form.name.trim())                          e.name         = 'Goal name is required'
    if (!form.targetAmount || form.targetAmount <= 0) e.targetAmount = 'Enter a valid target'
    if (form.savedAmount < 0)                       e.savedAmount  = 'Cannot be negative'
    if (!form.deadline)                             e.deadline     = 'Deadline is required'
    return e
  }

  const handleSubmit = async () => {
    const e = validate()
    if (Object.keys(e).length) { setErrors(e); return }
    setSaving(true)
    try {
      await onSave({
        name:         form.name.trim(),
        targetAmount: Number(form.targetAmount),
        savedAmount:  Number(form.savedAmount) || 0,
        deadline:     form.deadline,
      })
      onClose()
    } catch { /* toast shown by hook */ }
    finally { setSaving(false) }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-sm bg-white rounded-3xl shadow-card-lg animate-slide-up">
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-surface-100">
          <h2 className="text-base font-semibold text-ink-900">{isEdit ? 'Edit Goal' : 'New Savings Goal'}</h2>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-700"><X size={18} /></button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="label">Goal Name</label>
            <input type="text" placeholder="e.g. Emergency Fund, Vacation"
              value={form.name} onChange={e => set('name', e.target.value)}
              className={`input ${errors.name ? 'border-red-400' : ''}`}
            />
            {errors.name && <p className="text-xs text-red-500 mt-1">{errors.name}</p>}
          </div>

          <div>
            <label className="label">Target Amount (₹)</label>
            <input type="number" min="1" placeholder="e.g. 50000"
              value={form.targetAmount} onChange={e => set('targetAmount', e.target.value)}
              className={`input ${errors.targetAmount ? 'border-red-400' : ''}`}
            />
            {errors.targetAmount && <p className="text-xs text-red-500 mt-1">{errors.targetAmount}</p>}
          </div>

          <div>
            <label className="label">Already Saved (₹)</label>
            <input type="number" min="0" placeholder="0"
              value={form.savedAmount} onChange={e => set('savedAmount', e.target.value)}
              className={`input ${errors.savedAmount ? 'border-red-400' : ''}`}
            />
            {errors.savedAmount && <p className="text-xs text-red-500 mt-1">{errors.savedAmount}</p>}
          </div>

          <div>
            <label className="label">Deadline</label>
            <input type="date" min={minDeadline()}
              value={form.deadline} onChange={e => set('deadline', e.target.value)}
              className={`input ${errors.deadline ? 'border-red-400' : ''}`}
            />
            {errors.deadline && <p className="text-xs text-red-500 mt-1">{errors.deadline}</p>}
          </div>

          <div className="flex gap-3 pt-2">
            <button onClick={onClose} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleSubmit} disabled={saving} className="btn-primary flex-1">
              {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Goal'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Individual goal card ──────────────────────────────────────────────────────
function GoalCard({ goal, onEdit, onDelete }) {
  const isCompleted = goal.status === 'completed'
  const ringColor   = isCompleted ? '#12b76a' : goal.onTrack === false ? '#f04438' : '#444ce7'

  const daysToDeadline = goal.deadline
    ? Math.ceil((new Date(goal.deadline) - new Date()) / 86400000)
    : null

  return (
    <div className={`card relative animate-slide-up ${isCompleted ? 'border-emerald-200 bg-emerald-50/20' : ''}`}>
      {/* Actions */}
      <div className="absolute top-4 right-4 flex gap-2">
        {!isCompleted && (
          <button onClick={() => onEdit(goal)} className="text-ink-300 hover:text-brand-500 transition-colors">
            <Pencil size={13} />
          </button>
        )}
        <button onClick={() => onDelete(goal.id)} className="text-ink-300 hover:text-red-500 transition-colors">
          <Trash2 size={13} />
        </button>
      </div>

      {/* Top row: ring + name */}
      <div className="flex items-center gap-4 mb-4">
        <div className="relative flex-shrink-0">
          <ProgressRing pct={goal.progressPct} color={ringColor} />
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-xs font-semibold text-ink-700">{Math.round(goal.progressPct)}%</span>
          </div>
        </div>
        <div className="flex-1 min-w-0 pr-8">
          <div className="flex items-center gap-1.5 mb-0.5">
            {isCompleted
              ? <CheckCircle2 size={13} className="text-emerald-500 flex-shrink-0" />
              : <Target size={13} className="text-brand-500 flex-shrink-0" />
            }
            <p className="text-sm font-semibold text-ink-900 truncate">{goal.name}</p>
          </div>
          <p className="text-xs text-ink-400">
            {formatCurrency(goal.savedAmount)} of {formatCurrency(goal.targetAmount)}
          </p>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 gap-3 mb-4 text-xs">
        <div className="bg-surface-50 rounded-xl p-3">
          <p className="text-ink-400 mb-0.5 flex items-center gap-1"><CalendarDays size={10} /> Deadline</p>
          <p className="font-medium text-ink-800">{goal.deadline}</p>
          {!isCompleted && daysToDeadline !== null && (
            <p className={`mt-0.5 ${daysToDeadline < 30 ? 'text-amber-600' : 'text-ink-400'}`}>
              {daysToDeadline > 0 ? `${daysToDeadline}d left` : 'Overdue'}
            </p>
          )}
        </div>
        <div className="bg-surface-50 rounded-xl p-3">
          <p className="text-ink-400 mb-0.5 flex items-center gap-1"><TrendingUp size={10} /> Remaining</p>
          <p className="font-medium text-ink-800">{formatCurrency(goal.remaining)}</p>
        </div>
      </div>

      {/* Prediction note */}
      {goal.predictionNote && (
        <div className={`rounded-xl px-3 py-2.5 text-xs flex items-start gap-2 ${
          isCompleted                  ? 'bg-emerald-50 text-emerald-700' :
          goal.onTrack === false       ? 'bg-red-50 text-red-700' :
          goal.onTrack === true        ? 'bg-brand-50 text-brand-700' :
                                         'bg-surface-100 text-ink-500'
        }`}>
          {goal.onTrack === false && !isCompleted
            ? <AlertTriangle size={12} className="flex-shrink-0 mt-0.5" />
            : <TrendingUp size={12} className="flex-shrink-0 mt-0.5" />
          }
          <span>{goal.predictionNote}</span>
        </div>
      )}

      {/* Predicted completion */}
      {goal.predictedCompletion && !isCompleted && (
        <p className="text-xs text-ink-400 mt-2 text-right">
          Predicted: <strong className="text-ink-600">{goal.predictedCompletion}</strong>
        </p>
      )}
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function GoalsPage() {
  const { goals, isLoading, fetchAll, createGoal, updateGoal, deleteGoal } = useGoals()
  const [modal, setModal] = useState(null) // null | 'new' | goal-object (edit)

  const activeGoals    = goals.filter(g => g.status !== 'completed')
  const completedGoals = goals.filter(g => g.status === 'completed')

  const handleSave = async (data) => {
    if (modal === 'new') await createGoal(data)
    else                 await updateGoal(modal.id, data)
  }

  if (isLoading) return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      {[1,2,3,4].map(i => <Skeleton key={i} className="h-56" />)}
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">Savings Goals</h2>
          <p className="text-sm text-ink-400 mt-0.5">Track progress and predict completion dates</p>
        </div>
        <div className="flex gap-2">
          <button onClick={fetchAll} className="btn-ghost flex items-center gap-2 text-sm">
            <RefreshCw size={14} /> Refresh
          </button>
          <button onClick={() => setModal('new')} className="btn-primary flex items-center gap-2 text-sm">
            <Plus size={15} /> New Goal
          </button>
        </div>
      </div>

      {/* Active goals */}
      {activeGoals.length === 0 && completedGoals.length === 0 ? (
        <div className="card py-16 text-center">
          <p className="text-4xl mb-3">🎯</p>
          <p className="text-sm font-medium text-ink-700">No savings goals yet</p>
          <p className="text-sm text-ink-400 mt-1">Create your first goal and start tracking progress.</p>
        </div>
      ) : (
        <>
          {activeGoals.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-ink-400 uppercase tracking-wider mb-3">Active</p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {activeGoals.map(g => (
                  <GoalCard key={g.id} goal={g} onEdit={setModal} onDelete={deleteGoal} />
                ))}
              </div>
            </div>
          )}

          {completedGoals.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-ink-400 uppercase tracking-wider mb-3">Completed</p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {completedGoals.map(g => (
                  <GoalCard key={g.id} goal={g} onEdit={setModal} onDelete={deleteGoal} />
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {modal && (
        <GoalModal
          initial={modal === 'new' ? null : modal}
          onSave={handleSave}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  )
}
