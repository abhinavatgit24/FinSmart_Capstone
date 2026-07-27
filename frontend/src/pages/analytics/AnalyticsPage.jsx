import { useState } from 'react'
import { useAnalytics } from '../../hooks/useAnalytics'
import { analyticsApi } from '../../api'
import { formatCurrency } from '../../utils/formatCurrency'
import { getCategoryColor, getCategoryIcon } from '../../utils/categories'
import {
  BarChart, Bar, PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  XAxis, YAxis, CartesianGrid, Legend, RadarChart, Radar,
  PolarGrid, PolarAngleAxis, LineChart, Line
} from 'recharts'
import {
  RefreshCw, TrendingUp, TrendingDown, AlertTriangle,
  CheckCircle2, Info, RotateCcw, CalendarDays
} from 'lucide-react'
import { useEffect } from 'react'

function Skeleton({ className = '' }) {
  return <div className={`bg-surface-200 rounded-xl animate-pulse ${className}`} />
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-surface-200 rounded-xl shadow-card-md px-3 py-2 text-xs">
      {label && <p className="font-semibold text-ink-700 mb-1">{label}</p>}
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }}>
          {p.name}: {formatCurrency(p.value)}
        </p>
      ))}
    </div>
  )
}

// ── Insight card ──────────────────────────────────────────────────────────────
function InsightCard({ insight }) {
  const meta = {
    warning:  { icon: AlertTriangle, bg: 'bg-amber-50', border: 'border-amber-200', text: 'text-amber-700', iconColor: '#f59e0b' },
    positive: { icon: CheckCircle2,  bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-700', iconColor: '#12b76a' },
    info:     { icon: Info,          bg: 'bg-brand-50', border: 'border-brand-200', text: 'text-brand-700', iconColor: '#444ce7' },
  }[insight.type] || { icon: Info, bg: 'bg-surface-50', border: 'border-surface-200', text: 'text-ink-600', iconColor: '#888' }

  const Icon = meta.icon

  return (
    <div className={`flex items-start gap-3 px-4 py-3 rounded-xl border ${meta.bg} ${meta.border}`}>
      <Icon size={15} style={{ color: meta.iconColor }} className="flex-shrink-0 mt-0.5" />
      <div>
        <p className={`text-xs font-semibold ${meta.text}`}>{insight.title}</p>
        <p className="text-xs text-ink-600 mt-0.5">{insight.body}</p>
      </div>
    </div>
  )
}

// ── Subscription card ─────────────────────────────────────────────────────────
function SubCard({ sub }) {
  const freqColor = { monthly: '#444ce7', weekly: '#f59e0b', annual: '#12b76a', irregular: '#888' }[sub.frequency] || '#888'
  return (
    <div className="card flex items-center gap-4">
      <div className="w-10 h-10 rounded-xl bg-brand-50 flex items-center justify-center flex-shrink-0 text-lg font-bold text-brand-600">
        {sub.merchant.charAt(0).toUpperCase()}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-ink-800 truncate">{sub.merchant}</p>
        <p className="text-xs text-ink-400 mt-0.5">
          {sub.occurrences}× detected · Next: {sub.nextExpected || '—'}
        </p>
      </div>
      <div className="text-right flex-shrink-0">
        <p className="text-sm font-semibold text-ink-900">{formatCurrency(sub.amount)}</p>
        <span
          className="text-[10px] font-semibold px-2 py-0.5 rounded-full"
          style={{ background: freqColor + '18', color: freqColor }}
        >{sub.frequency}</span>
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function AnalyticsPage() {
  const [months, setMonths]       = useState(6)
  const { analytics, isLoading, refresh } = useAnalytics(months)
  const [subs, setSubs]           = useState(null)
  const [subsLoading, setSubsLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('overview')

  useEffect(() => {
    setSubsLoading(true)
    analyticsApi.getSubscriptions()
      .then(setSubs)
      .catch(() => setSubs([]))
      .finally(() => setSubsLoading(false))
  }, [])

  const tabs = [
    { id: 'overview',       label: 'Overview'       },
    { id: 'categories',     label: 'Categories'     },
    { id: 'monthly',        label: 'Month-over-Month'},
    { id: 'insights',       label: 'Insights'       },
    { id: 'subscriptions',  label: 'Subscriptions'  },
  ]

  if (isLoading) return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[1,2,3].map(i => <Skeleton key={i} className="h-28" />)}
      </div>
      <Skeleton className="h-72" />
      <Skeleton className="h-56" />
    </div>
  )

  if (!analytics) return null

  const {
    categoryExpenses, categoryIncome, monthlyComparison,
    dayOfWeekExpenses, weekendExpensePct, topSpendingDays, insights
  } = analytics

  // Category pie data
  const pieData = Object.entries(categoryExpenses || {})
    .map(([name, value]) => ({ name, value }))
    .sort((a,b) => b.value - a.value)

  // Bar chart: category comparison
  const catBarData = Object.entries(categoryExpenses || {})
    .map(([name, value]) => ({ name: name.substring(0,7), full: name, value }))
    .sort((a,b) => b.value - a.value)
    .slice(0, 8)

  // Day of week bar
  const dowData = Object.entries(dayOfWeekExpenses || {})
    .map(([day, value]) => ({ day: day.substring(0,3), value }))

  const totalExp = pieData.reduce((s, d) => s + d.value, 0)

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">Analytics</h2>
          <p className="text-sm text-ink-400 mt-0.5">Deep dive into your financial patterns</p>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={months}
            onChange={e => setMonths(Number(e.target.value))}
            className="input py-1.5 text-sm w-36"
          >
            <option value={1}>This month</option>
            <option value={3}>Last 3 months</option>
            <option value={6}>Last 6 months</option>
            <option value={12}>Last 12 months</option>
          </select>
          <button onClick={refresh} className="btn-ghost flex items-center gap-2 text-sm">
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-100 p-1 rounded-xl overflow-x-auto">
        {tabs.map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`flex-1 py-2 px-3 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
              activeTab === t.id
                ? 'bg-white text-ink-900 shadow-sm'
                : 'text-ink-500 hover:text-ink-700'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Overview tab ─────────────────────────────────────────────── */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Weekend split cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              { label: 'Weekend Spend', value: weekendExpensePct + '%', color: '#f59e0b' },
              { label: 'Weekday Spend', value: (100 - weekendExpensePct) + '%', color: '#444ce7' },
              { label: 'Categories', value: pieData.length, color: '#12b76a' },
              { label: 'Insights', value: insights?.length || 0, color: '#ec4899' },
            ].map(s => (
              <div key={s.label} className="card text-center">
                <p className="text-xs text-ink-400 mb-1">{s.label}</p>
                <p className="text-2xl font-semibold" style={{ color: s.color }}>{s.value}</p>
              </div>
            ))}
          </div>

          {/* Day-of-week heatmap */}
          <div className="card">
            <h3 className="text-sm font-semibold text-ink-800 mb-4 flex items-center gap-2">
              <CalendarDays size={14} className="text-brand-500" /> Spending by Day of Week
            </h3>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={dowData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="day" tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false}
                  tickFormatter={v => `₹${v >= 1000 ? (v/1000).toFixed(0)+'k' : v}`} />
                <Tooltip content={<ChartTooltip />} />
                <Bar dataKey="value" name="Expenses" radius={[4,4,0,0]}>
                  {dowData.map((_, i) => (
                    <Cell key={i} fill={i >= 5 ? '#f59e0b' : '#444ce7'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
            <div className="flex items-center gap-4 mt-2 justify-center text-xs text-ink-400">
              <span className="flex items-center gap-1"><span className="w-3 h-2 rounded bg-brand-500 inline-block"/> Weekday</span>
              <span className="flex items-center gap-1"><span className="w-3 h-2 rounded bg-amber-400 inline-block"/> Weekend</span>
            </div>
          </div>

          {/* Top spending days */}
          {topSpendingDays?.length > 0 && (
            <div className="card">
              <h3 className="text-sm font-semibold text-ink-800 mb-3">Top Spending Days</h3>
              <div className="space-y-2">
                {topSpendingDays.map((d, i) => (
                  <div key={d.date} className="flex items-center gap-3">
                    <span className="text-xs text-ink-400 w-4 text-right">{i+1}</span>
                    <span className="text-xs text-ink-700 w-24">{d.date}</span>
                    <div className="flex-1 bg-surface-100 rounded-full h-2 overflow-hidden">
                      <div
                        className="h-2 rounded-full bg-brand-500"
                        style={{ width: `${Math.min(100, (d.total / topSpendingDays[0].total) * 100)}%` }}
                      />
                    </div>
                    <span className="text-xs font-semibold text-ink-800 w-20 text-right">
                      {formatCurrency(d.total)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── Categories tab ─────────────────────────────────────────────── */}
      {activeTab === 'categories' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Pie */}
            <div className="card">
              <h3 className="text-sm font-semibold text-ink-800 mb-4">Expense Distribution</h3>
              {pieData.length === 0 ? (
                <div className="h-56 flex items-center justify-center text-sm text-ink-400">No data</div>
              ) : (
                <div className="flex gap-4 items-center">
                  <ResponsiveContainer width="55%" height={200}>
                    <PieChart>
                      <Pie data={pieData} cx="50%" cy="50%" innerRadius={55} outerRadius={85}
                        paddingAngle={2} dataKey="value">
                        {pieData.map((e,i) => <Cell key={i} fill={getCategoryColor(e.name)} strokeWidth={0} />)}
                      </Pie>
                      <Tooltip content={<ChartTooltip />} />
                    </PieChart>
                  </ResponsiveContainer>
                  <div className="flex-1 space-y-2">
                    {pieData.slice(0,6).map(({ name, value }) => {
                      const pct = totalExp > 0 ? ((value/totalExp)*100).toFixed(1) : 0
                      return (
                        <div key={name} className="flex items-center gap-2">
                          <span className="text-base flex-shrink-0">{getCategoryIcon(name)}</span>
                          <div className="flex-1 min-w-0">
                            <div className="flex justify-between text-xs mb-0.5">
                              <span className="text-ink-600 truncate">{name}</span>
                              <span className="font-medium text-ink-800">{pct}%</span>
                            </div>
                            <div className="h-1.5 bg-surface-100 rounded-full overflow-hidden">
                              <div className="h-1.5 rounded-full" style={{
                                width: `${pct}%`, background: getCategoryColor(name)
                              }} />
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Bar */}
            <div className="card">
              <h3 className="text-sm font-semibold text-ink-800 mb-4">Expense by Category</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={catBarData} layout="vertical" margin={{ left: 8, right: 12 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false} />
                  <XAxis type="number" tick={{ fontSize: 10, fill: '#888' }} axisLine={false} tickLine={false}
                    tickFormatter={v => `₹${v >= 1000 ? (v/1000).toFixed(0)+'k' : v}`} />
                  <YAxis dataKey="name" type="category" tick={{ fontSize: 11, fill: '#555' }}
                    axisLine={false} tickLine={false} width={52} />
                  <Tooltip content={<ChartTooltip />} />
                  <Bar dataKey="value" name="Amount" radius={[0,4,4,0]}>
                    {catBarData.map((e,i) => <Cell key={i} fill={getCategoryColor(e.full)} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {/* ── Month-over-Month tab ──────────────────────────────────────── */}
      {activeTab === 'monthly' && (
        <div className="space-y-6">
          <div className="card">
            <h3 className="text-sm font-semibold text-ink-800 mb-4">Income vs Expense by Month</h3>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={monthlyComparison} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false}
                  tickFormatter={v => `₹${v >= 1000 ? (v/1000).toFixed(0)+'k' : v}`} />
                <Tooltip content={<ChartTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="income"  name="Income"  fill="#12b76a" radius={[3,3,0,0]} />
                <Bar dataKey="expense" name="Expense" fill="#f04438" radius={[3,3,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <h3 className="text-sm font-semibold text-ink-800 mb-4">Monthly Savings Trend</h3>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={monthlyComparison} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#888' }} axisLine={false} tickLine={false}
                  tickFormatter={v => `₹${v >= 1000 ? (v/1000).toFixed(0)+'k' : v}`} />
                <Tooltip content={<ChartTooltip />} />
                <Line type="monotone" dataKey="savings" name="Savings" stroke="#444ce7" strokeWidth={2.5}
                  dot={{ fill: '#444ce7', r: 3, strokeWidth: 0 }}
                  activeDot={{ r: 5, strokeWidth: 0 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* ── Insights tab ─────────────────────────────────────────────── */}
      {activeTab === 'insights' && (
        <div className="space-y-3">
          {!insights?.length ? (
            <div className="card py-14 text-center">
              <p className="text-3xl mb-2">💡</p>
              <p className="text-sm text-ink-500">No insights yet — keep logging transactions!</p>
            </div>
          ) : (
            insights.map((ins, i) => <InsightCard key={i} insight={ins} />)
          )}
        </div>
      )}

      {/* ── Subscriptions tab ─────────────────────────────────────────── */}
      {activeTab === 'subscriptions' && (
        <div className="space-y-4">
          {subsLoading ? (
            [1,2,3].map(i => <Skeleton key={i} className="h-20" />)
          ) : !subs?.length ? (
            <div className="card py-14 text-center">
              <p className="text-3xl mb-2">🔄</p>
              <p className="text-sm text-ink-500">No recurring transactions detected yet.</p>
              <p className="text-xs text-ink-400 mt-1">Log transactions for 2+ months to see subscriptions.</p>
            </div>
          ) : (
            <>
              <div className="rounded-2xl bg-brand-50 border border-brand-200 px-5 py-3 text-sm text-brand-700">
                <strong>{subs.length}</strong> recurring charge{subs.length !== 1 ? 's' : ''} detected ·{' '}
                Total annualised cost: <strong>
                  {formatCurrency(subs.reduce((s, sub) => s + (sub.annualisedCost || 0), 0))}
                </strong>
              </div>
              {subs.map((sub, i) => <SubCard key={i} sub={sub} />)}
            </>
          )}
        </div>
      )}
    </div>
  )
}
