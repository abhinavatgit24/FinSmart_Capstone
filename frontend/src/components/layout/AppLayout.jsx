import { Outlet, NavLink, useLocation } from 'react-router-dom'
import { useState } from 'react'
import {
  LayoutDashboard, ArrowLeftRight, LogOut,
  Menu, X, TrendingUp, User, Wallet, Target,
  HeartPulse, BarChart2, Upload
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { NotificationBell } from '../ui/NotificationBell'
import { AskAiWidget } from '../ui/AskAiWidget'

const NAV = [
  { to: '/dashboard',    icon: LayoutDashboard, label: 'Dashboard'    },
  { to: '/transactions', icon: ArrowLeftRight,   label: 'Transactions' },
  { to: '/budgets',      icon: Wallet,           label: 'Budgets'      },
  { to: '/goals',        icon: Target,           label: 'Goals'        },
  { to: '/analytics',   icon: BarChart2,         label: 'Analytics'    },
  { to: '/import',      icon: Upload,            label: 'Import CSV'   },
  { to: '/health',       icon: HeartPulse,       label: 'Health Score' },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const [open, setOpen]  = useState(false)
  const location         = useLocation()

  const pageTitle = NAV.find(n => location.pathname.startsWith(n.to))?.label || 'FinSmart'

  return (
    <div className="flex h-screen bg-surface-50 overflow-hidden">

      {/* ── Sidebar ───────────────────────────────────────────────────── */}
      {/* Mobile overlay */}
      {open && (
        <div
          className="fixed inset-0 bg-black/20 backdrop-blur-sm z-20 lg:hidden"
          onClick={() => setOpen(false)}
        />
      )}

      <aside className={`
        fixed lg:static inset-y-0 left-0 z-30
        w-60 flex flex-col bg-white border-r border-surface-200
        transform transition-transform duration-200 ease-out
        ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        {/* Logo */}
        <div className="flex items-center gap-2.5 px-5 h-16 border-b border-surface-100">
          <div className="w-8 h-8 rounded-xl bg-brand-600 flex items-center justify-center flex-shrink-0">
            <TrendingUp size={16} className="text-white" />
          </div>
          <span className="font-semibold text-ink-900 text-lg tracking-tight">FinSmart</span>
          <button onClick={() => setOpen(false)} className="ml-auto lg:hidden text-ink-400 hover:text-ink-700">
            <X size={18} />
          </button>
        </div>

        {/* Nav links */}
        <nav className="flex-1 px-3 py-4 space-y-0.5">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to} to={to}
              onClick={() => setOpen(false)}
              className={({ isActive }) => `
                flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium
                transition-all duration-150 group
                ${isActive
                  ? 'bg-brand-50 text-brand-700'
                  : 'text-ink-500 hover:bg-surface-100 hover:text-ink-800'
                }
              `}
            >
              {({ isActive }) => (
                <>
                  <Icon size={17} className={isActive ? 'text-brand-600' : 'text-ink-400 group-hover:text-ink-600'} />
                  {label}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        {/* User section */}
        <div className="p-3 border-t border-surface-100">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-surface-100 transition-colors">
            <div className="w-8 h-8 rounded-full bg-brand-100 flex items-center justify-center flex-shrink-0">
              <User size={14} className="text-brand-600" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-ink-800 truncate">{user?.name}</p>
              <p className="text-xs text-ink-400 truncate">{user?.email}</p>
            </div>
            <button
              onClick={logout}
              className="text-ink-400 hover:text-red-500 transition-colors"
              title="Sign out"
            >
              <LogOut size={15} />
            </button>
          </div>
        </div>
      </aside>

      {/* ── Main content ──────────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Top bar */}
        <header className="h-16 bg-white border-b border-surface-200 flex items-center px-6 gap-4 flex-shrink-0">
          <button
            onClick={() => setOpen(true)}
            className="lg:hidden text-ink-400 hover:text-ink-700 transition-colors"
          >
            <Menu size={20} />
          </button>
          <h1 className="text-base font-semibold text-ink-900">{pageTitle}</h1>
          <div className="ml-auto">
            <NotificationBell />
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <div className="p-6 max-w-6xl mx-auto animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>

      {/* AI chat widget — fixed position, appears on every page */}
      <AskAiWidget />
    </div>
  )
}
