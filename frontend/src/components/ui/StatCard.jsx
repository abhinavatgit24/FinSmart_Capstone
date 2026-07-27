// ── StatCard ──────────────────────────────────────────────────────────────────
import { TrendingUp, TrendingDown } from 'lucide-react'

export function StatCard({ label, value, sub, accent, icon: Icon, trend }) {
  return (
    <div className="card flex flex-col gap-4 animate-slide-up">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-ink-400 uppercase tracking-wider mb-1">{label}</p>
          <p className="text-2xl font-semibold text-ink-900" style={{ letterSpacing: '-0.02em' }}>
            {value}
          </p>
          {sub && <p className="text-xs text-ink-400 mt-1">{sub}</p>}
        </div>
        {Icon && (
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
            style={{ background: accent + '18' }}
          >
            <Icon size={18} style={{ color: accent }} />
          </div>
        )}
      </div>
      {trend !== undefined && (
        <div className="flex items-center gap-1.5">
          {trend >= 0
            ? <TrendingUp size={13} className="text-emerald-500" />
            : <TrendingDown size={13} className="text-red-400" />
          }
          <span className={`text-xs font-medium ${trend >= 0 ? 'text-emerald-600' : 'text-red-500'}`}>
            {Math.abs(trend).toFixed(1)}% vs last month
          </span>
        </div>
      )}
    </div>
  )
}
