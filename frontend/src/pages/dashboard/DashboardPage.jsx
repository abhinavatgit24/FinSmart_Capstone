import { useDashboard } from '../../hooks/useDashboard'
import { useBudgets } from '../../hooks/useBudgets'
import { useHealthScore } from '../../hooks/useHealthScore'
import { StatCard } from '../../components/ui/StatCard'
import { CategoryBadge } from '../../components/ui/CategoryBadge'
import { formatCurrency, formatCompact } from '../../utils/formatCurrency'
import { getCategoryColor } from '../../utils/categories'
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Legend
} from 'recharts'
import {
  Wallet, TrendingUp, TrendingDown,
  ArrowUpRight, ArrowDownRight, RefreshCw,
  AlertTriangle, XCircle, HeartPulse, ChevronRight
} from 'lucide-react'
import { Link } from 'react-router-dom'

const MONTH_ORDER = [
  'JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE',
  'JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'
]

function Skeleton({ className = '' }) {
  return <div className={`bg-surface-200 rounded-xl animate-pulse ${className}`} />
}

function CustomTooltip({ active, payload }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-surface-200 rounded-xl shadow-card-md px-3 py-2 text-xs">
      <p className="font-medium text-ink-800">{payload[0].name}</p>
      <p className="text-ink-500">{formatCurrency(payload[0].value)}</p>
    </div>
  )
}

export default function DashboardPage() {
  const { summary, isLoading, refresh } = useDashboard()
  const { utilisations }                = useBudgets()
  const { health }                      = useHealthScore()

  const alerts = utilisations.filter(u => u.alertLevel !== 'none')

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {[1,2,3].map(i => <Skeleton key={i} className="h-32" />)}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-72" />
          <Skeleton className="h-72" />
        </div>
        <Skeleton className="h-64" />
      </div>
    )
  }

  if (!summary) return null

  const {
    totalIncome, totalExpense, balance,
    categoryBreakdown, recentTransactions, monthlyTrend
  } = summary

  // Pie chart data
  const pieData = Object.entries(categoryBreakdown || {})
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)

  // Line chart data — sorted by month order
  const lineData = Object.entries(monthlyTrend || {})
    .sort(([a], [b]) => MONTH_ORDER.indexOf(a) - MONTH_ORDER.indexOf(b))
    .map(([month, amount]) => ({
      month: month.charAt(0) + month.slice(1, 3).toLowerCase(),
      amount
    }))

  const savingsRate = totalIncome > 0
    ? (((totalIncome - totalExpense) / totalIncome) * 100).toFixed(1)
    : 0

  return (
    <div className="space-y-6">
      {/* Header row */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">Overview</h2>
          <p className="text-sm text-ink-400 mt-0.5">Your financial summary at a glance</p>
        </div>
        <button
          onClick={refresh}
          className="btn-ghost flex items-center gap-2 text-sm"
        >
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          label="Total Income"
          value={formatCompact(totalIncome)}
          sub={formatCurrency(totalIncome)}
          accent="#12b76a"
          icon={TrendingUp}
        />
        <StatCard
          label="Total Expenses"
          value={formatCompact(totalExpense)}
          sub={formatCurrency(totalExpense)}
          accent="#f04438"
          icon={TrendingDown}
        />
        <StatCard
          label="Net Balance"
          value={formatCompact(balance)}
          sub={`${savingsRate}% savings rate`}
          accent="#444ce7"
          icon={Wallet}
        />
      </div>

      {/* Budget alert banner */}
      {alerts.length > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4">
          <p className="text-sm font-semibold text-amber-800 flex items-center gap-2 mb-1">
            <AlertTriangle size={15} /> Budget Alerts
          </p>
          <div className="flex flex-wrap gap-2 mt-2">
            {alerts.map(a => (
              <span key={a.id} className={`flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full ${
                a.alertLevel === 'exceeded'
                  ? 'bg-red-100 text-red-700'
                  : 'bg-amber-100 text-amber-700'
              }`}>
                {a.alertLevel === 'exceeded' ? <XCircle size={11} /> : <AlertTriangle size={11} />}
                {a.category} — {a.utilisationPct.toFixed(0)}% used
              </span>
            ))}
          </div>
          <Link to="/budgets" className="text-xs text-amber-700 underline mt-2 inline-block font-medium">
            Manage budgets →
          </Link>
        </div>
      )}

      {/* Health Score mini card */}
      {health && (
        <Link to="/health" className="block">
          <div className="card flex items-center justify-between hover:shadow-card-md transition-shadow cursor-pointer group">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{
                  background: (health.score >= 80 ? '#12b76a' : health.score >= 60 ? '#444ce7' : health.score >= 40 ? '#f59e0b' : '#f04438') + '18'
                }}>
                <HeartPulse size={18} style={{
                  color: health.score >= 80 ? '#12b76a' : health.score >= 60 ? '#444ce7' : health.score >= 40 ? '#f59e0b' : '#f04438'
                }} />
              </div>
              <div>
                <p className="text-xs text-ink-400 uppercase tracking-wider font-medium">Financial Health Score</p>
                <p className="text-sm text-ink-500 mt-0.5">{health.insight}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 flex-shrink-0">
              <div className="text-right">
                <p className="text-2xl font-semibold" style={{
                  color: health.score >= 80 ? '#12b76a' : health.score >= 60 ? '#444ce7' : health.score >= 40 ? '#f59e0b' : '#f04438'
                }}>{health.score}</p>
                <p className="text-xs text-ink-400">{health.band}</p>
              </div>
              <ChevronRight size={16} className="text-ink-300 group-hover:text-ink-600 transition-colors" />
            </div>
          </div>
        </Link>
      )}

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Category breakdown pie */}
        <div className="card">
          <h3 className="text-sm font-semibold text-ink-800 mb-4">Spending by Category</h3>
          {pieData.length === 0 ? (
            <div className="h-56 flex items-center justify-center text-sm text-ink-400">
              No expense data yet
            </div>
          ) : (
            <div className="flex gap-4 items-center">
              <ResponsiveContainer width="55%" height={200}>
                <PieChart>
                  <Pie
                    data={pieData} cx="50%" cy="50%"
                    innerRadius={55} outerRadius={85}
                    paddingAngle={2} dataKey="value"
                  >
                    {pieData.map((entry, i) => (
                      <Cell key={i} fill={getCategoryColor(entry.name)} strokeWidth={0} />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex-1 space-y-2">
                {pieData.slice(0, 5).map(({ name, value }) => (
                  <div key={name} className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <div
                        className="w-2 h-2 rounded-full flex-shrink-0"
                        style={{ background: getCategoryColor(name) }}
                      />
                      <span className="text-xs text-ink-600 truncate">{name}</span>
                    </div>
                    <span className="text-xs font-medium text-ink-800 flex-shrink-0">
                      {formatCompact(value)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Monthly trend line */}
        <div className="card">
          <h3 className="text-sm font-semibold text-ink-800 mb-4">Monthly Expense Trend</h3>
          {lineData.length === 0 ? (
            <div className="h-56 flex items-center justify-center text-sm text-ink-400">
              No trend data yet
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={lineData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="month"
                  tick={{ fontSize: 11, fill: '#888' }}
                  axisLine={false} tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: '#888' }}
                  axisLine={false} tickLine={false}
                  tickFormatter={v => `₹${v >= 1000 ? `${(v/1000).toFixed(0)}k` : v}`}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone" dataKey="amount" name="Expenses"
                  stroke="#444ce7" strokeWidth={2}
                  dot={{ fill: '#444ce7', r: 3, strokeWidth: 0 }}
                  activeDot={{ r: 5, strokeWidth: 0 }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Recent transactions */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-ink-800">Recent Transactions</h3>
          <a href="/transactions" className="text-xs text-brand-600 hover:underline font-medium">
            View all →
          </a>
        </div>

        {!recentTransactions?.length ? (
          <div className="py-10 text-center text-sm text-ink-400">
            No transactions yet — add your first one!
          </div>
        ) : (
          <div className="divide-y divide-surface-100">
            {recentTransactions.map(txn => (
              <div key={txn.id} className="flex items-center gap-4 py-3">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-ink-800 truncate">
                    {txn.description || txn.category}
                  </p>
                  <p className="text-xs text-ink-400 mt-0.5">{txn.date}</p>
                </div>
                <CategoryBadge category={txn.category} size="xs" />
                <div className="text-right flex-shrink-0">
                  <p className={`text-sm font-semibold flex items-center gap-1 justify-end ${
                    txn.type === 'income' ? 'text-emerald-600' : 'text-red-500'
                  }`}>
                    {txn.type === 'income'
                      ? <ArrowUpRight size={13} />
                      : <ArrowDownRight size={13} />
                    }
                    {formatCurrency(txn.amount)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
