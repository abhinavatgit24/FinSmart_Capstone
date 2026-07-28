import { useHealthScore } from '../../hooks/useHealthScore'
import { reportApi } from '../../api'
import { RefreshCw, TrendingUp, ShieldCheck, BarChart3, Lightbulb, Download, PlusCircle } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

function Skeleton({ className = '' }) {
  return <div className={`bg-surface-200 rounded-xl animate-pulse ${className}`} />
}

// ── No data state ─────────────────────────────────────────────────────────────
function NoDataState({ insight }) {
  return (
    <div className="card flex flex-col items-center text-center py-16 gap-4">
      <div className="w-16 h-16 rounded-2xl bg-brand-50 flex items-center justify-center">
        <BarChart3 size={28} className="text-brand-400" />
      </div>
      <div>
        <h3 className="text-base font-semibold text-ink-800 mb-1">No data yet</h3>
        <p className="text-sm text-ink-400 max-w-sm">{insight}</p>
      </div>
      <div className="flex gap-3 mt-2">
        <Link to="/transactions" className="btn-primary flex items-center gap-2 text-sm">
          <PlusCircle size={15} /> Add Transactions
        </Link>
        <Link to="/import" className="btn-secondary flex items-center gap-2 text-sm">
          Import CSV
        </Link>
      </div>
    </div>
  )
}

// ── Semicircular gauge ────────────────────────────────────────────────────────
function ScoreGauge({ score }) {
  const size   = 220
  const cx     = size / 2
  const cy     = size / 2 + 10
  const r      = 82
  const stroke = 14

  const startAngle = Math.PI
  const totalArc   = Math.PI
  const pct        = Math.min(Math.max(score, 0), 100) / 100
  const filled     = totalArc * pct

  const ptX = (a) => cx + r * Math.cos(a)
  const ptY = (a) => cy - r * Math.sin(a)

  const trackD = `M ${ptX(startAngle)} ${ptY(startAngle)} A ${r} ${r} 0 0 1 ${ptX(0)} ${ptY(0)}`
  const fillEnd   = startAngle - filled
  const largeArc  = filled > Math.PI ? 1 : 0
  const fillD     = `M ${ptX(startAngle)} ${ptY(startAngle)} A ${r} ${r} 0 ${largeArc} 1 ${ptX(fillEnd)} ${ptY(fillEnd)}`

  const bandColor =
    score >= 80 ? '#12b76a' :
    score >= 60 ? '#444ce7' :
    score >= 40 ? '#f59e0b' : '#f04438'

  const band =
    score >= 80 ? 'Excellent' :
    score >= 60 ? 'Good' :
    score >= 40 ? 'Average' : 'Poor'

  return (
    <div className="flex flex-col items-center">
      <svg width={size} height={size / 2 + 30} viewBox={`0 0 ${size} ${size / 2 + 30}`}>
        <path d={trackD} fill="none" stroke="#efefef" strokeWidth={stroke} strokeLinecap="round" />
        {score > 0 && (
          <path d={fillD} fill="none" stroke={bandColor} strokeWidth={stroke} strokeLinecap="round" />
        )}
        <text x={ptX(startAngle) - 2} y={ptY(startAngle) + 20} textAnchor="middle" fill="#aaa" fontSize="10">Poor</text>
        <text x={ptX(0) + 2}          y={ptY(0) + 20}          textAnchor="middle" fill="#aaa" fontSize="10">Excellent</text>
        <text x={cx} y={cy - 6}  textAnchor="middle" fontSize="38" fontWeight="700" fill={bandColor}>{score}</text>
        <text x={cx} y={cy + 18} textAnchor="middle" fontSize="13" fontWeight="600" fill={bandColor}>{band}</text>
      </svg>
    </div>
  )
}

// ── Component score bar ───────────────────────────────────────────────────────
function ComponentBar({ label, icon: Icon, score, weight, detail, color }) {
  const displayScore = score != null ? Math.round(score) : null

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg flex items-center justify-center" style={{ background: color + '18' }}>
            <Icon size={13} style={{ color }} />
          </div>
          <div>
            <p className="text-sm font-medium text-ink-800">{label}</p>
            <p className="text-xs text-ink-400">{weight} weight · {detail}</p>
          </div>
        </div>
        <span className="text-sm font-semibold" style={{ color }}>
          {displayScore != null ? `${displayScore}/100` : '—'}
        </span>
      </div>
      <div className="w-full bg-surface-200 rounded-full h-2 overflow-hidden">
        <div
          className="h-2 rounded-full transition-all duration-700"
          style={{ width: `${Math.min(score ?? 0, 100)}%`, background: color }}
        />
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function HealthScorePage() {
  const { health, isLoading, refresh } = useHealthScore()
  const [exporting, setExporting] = useState(false)

  const downloadReport = async () => {
    setExporting(true)
    try {
      const now = new Date()
      const html = await reportApi.getMonthlyHtml(now.getFullYear(), now.getMonth() + 1)
      const win = window.open('', '_blank')
      win.document.write(html)
      win.document.close()
      setTimeout(() => win.print(), 600)
    } catch { /* silent */ }
    finally { setExporting(false) }
  }

  if (isLoading) return (
    <div className="space-y-4">
      <Skeleton className="h-64" />
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[1,2,3].map(i => <Skeleton key={i} className="h-20" />)}
      </div>
      <Skeleton className="h-48" />
    </div>
  )

  if (!health) return null

  const {
    score, band,
    savingsRatioScore, budgetAdherenceScore, spendingConsistencyScore,
    savingsRatioPct, budgetAdherencePct, spendingConsistencyPct,
    insight
  } = health

  // ── No data state: score is null ─────────────────────────────────────────
  if (score == null) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-ink-900">Financial Health Score</h2>
            <p className="text-sm text-ink-400 mt-0.5">A composite view of your financial wellbeing</p>
          </div>
          <button onClick={refresh} className="btn-ghost flex items-center gap-2 text-sm">
            <RefreshCw size={14} /> Refresh
          </button>
        </div>
        <NoDataState insight={insight} />
      </div>
    )
  }

  const bandColor =
    score >= 80 ? '#12b76a' :
    score >= 60 ? '#444ce7' :
    score >= 40 ? '#f59e0b' : '#f04438'

  const bands = [
    { label: 'Poor',      min: 0,  max: 39,  color: '#f04438' },
    { label: 'Average',   min: 40, max: 59,  color: '#f59e0b' },
    { label: 'Good',      min: 60, max: 79,  color: '#444ce7' },
    { label: 'Excellent', min: 80, max: 100, color: '#12b76a' },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">Financial Health Score</h2>
          <p className="text-sm text-ink-400 mt-0.5">A composite view of your financial wellbeing</p>
        </div>
        <div className="flex gap-2">
          <button onClick={refresh} className="btn-ghost flex items-center gap-2 text-sm">
            <RefreshCw size={14} /> Refresh
          </button>
          <button
            onClick={downloadReport}
            disabled={exporting}
            className="btn-primary flex items-center gap-2 text-sm"
          >
            <Download size={14} /> {exporting ? 'Generating…' : 'Export PDF Report'}
          </button>
        </div>
      </div>

      {/* Gauge + bands */}
      <div className="card flex flex-col items-center gap-6 lg:flex-row lg:items-center lg:gap-10">
        <ScoreGauge score={score} />
        <div className="flex-1 w-full space-y-3">
          <p className="text-sm font-semibold text-ink-700 mb-1">Score Bands</p>
          {bands.map(b => (
            <div key={b.label} className="flex items-center gap-3">
              <div className="w-20 text-xs font-medium" style={{ color: b.color }}>{b.label}</div>
              <div className="flex-1 bg-surface-100 rounded-full h-2 overflow-hidden">
                <div className="h-2 rounded-full" style={{ background: b.color, width: '100%', opacity: 0.3 }} />
              </div>
              <div className="text-xs text-ink-400 w-16 text-right">{b.min}–{b.max}</div>
              {score >= b.min && score <= b.max && (
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full"
                  style={{ background: b.color + '18', color: b.color }}>You</span>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Component scores */}
      <div className="card space-y-5">
        <h3 className="text-sm font-semibold text-ink-800">Score Breakdown</h3>
        <ComponentBar
          label="Savings Ratio" icon={TrendingUp}
          score={savingsRatioScore} weight="40%"
          detail={savingsRatioPct != null
            ? `${savingsRatioPct.toFixed(1)}% of income saved across your history`
            : 'No income recorded yet'}
          color="#12b76a"
        />
        <ComponentBar
          label="Budget Adherence" icon={ShieldCheck}
          score={budgetAdherenceScore} weight="40%"
          detail={budgetAdherencePct != null
            ? `${budgetAdherencePct.toFixed(0)}% of budgets within limit`
            : 'No budgets set'}
          color="#444ce7"
        />
        <ComponentBar
          label="Spending Consistency" icon={BarChart3}
          score={spendingConsistencyScore} weight="20%"
          detail="Variance score (lower month-to-month swings = better)"
          color="#f59e0b"
        />
      </div>

      {/* Insight */}
      <div className="card flex items-start gap-3" style={{ borderColor: bandColor + '40', background: bandColor + '08' }}>
        <div className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{ background: bandColor + '20' }}>
          <Lightbulb size={16} style={{ color: bandColor }} />
        </div>
        <div>
          <p className="text-sm font-semibold text-ink-800 mb-0.5">Personalised Insight</p>
          <p className="text-sm text-ink-600">{insight}</p>
        </div>
      </div>

      {/* Methodology */}
      <div className="card bg-surface-50 border-surface-200">
        <h3 className="text-xs font-semibold text-ink-500 uppercase tracking-wider mb-3">How it's calculated</h3>
        <div className="space-y-2 text-xs text-ink-500">
          <p><strong className="text-ink-700">Savings Ratio (40%)</strong> — (income − expenses) ÷ income across all recorded transactions. Reaching 30%+ gives full marks.</p>
          <p><strong className="text-ink-700">Budget Adherence (40%)</strong> — percentage of set budgets not exceeded in the current period.</p>
          <p><strong className="text-ink-700">Spending Consistency (20%)</strong> — variation across all recorded months of expenses. Lower volatility scores higher.</p>
        </div>
      </div>
    </div>
  )
}
