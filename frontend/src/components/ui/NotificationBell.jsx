import { useState, useRef, useEffect } from 'react'
import { Bell, CheckCheck, AlertTriangle, Target, TrendingUp, X } from 'lucide-react'
import { useNotifications } from '../../hooks/useNotifications'

const TYPE_META = {
  budget_warning:   { icon: AlertTriangle, color: '#f59e0b', bg: '#fef3c7' },
  budget_exceeded:  { icon: AlertTriangle, color: '#f04438', bg: '#fee2e2' },
  goal_completed:   { icon: Target,        color: '#12b76a', bg: '#dcfce7' },
  goal_milestone:   { icon: Target,        color: '#444ce7', bg: '#e8eaff' },
  goal_off_track:   { icon: TrendingUp,    color: '#f59e0b', bg: '#fef3c7' },
  unusual_spending: { icon: TrendingUp,    color: '#f04438', bg: '#fee2e2' },
}

function timeAgo(iso) {
  const d = new Date(iso)
  const diff = Math.floor((Date.now() - d) / 1000)
  if (diff < 60)     return 'just now'
  if (diff < 3600)   return `${Math.floor(diff / 60)}m ago`
  if (diff < 86400)  return `${Math.floor(diff / 3600)}h ago`
  return `${Math.floor(diff / 86400)}d ago`
}

export function NotificationBell() {
  const { notifications, unreadCount, markRead, markAllRead } = useNotifications()
  const [open, setOpen]   = useState(false)
  const panelRef = useRef(null)

  // Close on outside click
  useEffect(() => {
    const handler = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleClick = (n) => {
    if (!n.read) markRead(n.id)
  }

  return (
    <div className="relative" ref={panelRef}>
      {/* Bell button */}
      <button
        onClick={() => setOpen(o => !o)}
        className="relative w-9 h-9 flex items-center justify-center rounded-xl text-ink-400 hover:text-ink-700 hover:bg-surface-100 transition-all"
      >
        <Bell size={18} />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center px-1">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Panel */}
      {open && (
        <div className="absolute right-0 top-12 w-80 bg-white rounded-2xl shadow-card-lg border border-surface-200 z-50 animate-slide-up overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-surface-100">
            <div className="flex items-center gap-2">
              <Bell size={14} className="text-ink-500" />
              <span className="text-sm font-semibold text-ink-900">Notifications</span>
              {unreadCount > 0 && (
                <span className="bg-red-100 text-red-600 text-xs font-semibold px-1.5 py-0.5 rounded-full">
                  {unreadCount}
                </span>
              )}
            </div>
            <div className="flex items-center gap-1">
              {unreadCount > 0 && (
                <button
                  onClick={markAllRead}
                  className="text-xs text-brand-600 hover:text-brand-800 flex items-center gap-1 px-2 py-1 rounded-lg hover:bg-brand-50 transition-colors"
                >
                  <CheckCheck size={12} /> All read
                </button>
              )}
              <button onClick={() => setOpen(false)} className="text-ink-400 hover:text-ink-700 p-1 rounded-lg">
                <X size={14} />
              </button>
            </div>
          </div>

          {/* List */}
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="py-10 text-center">
                <Bell size={28} className="text-ink-200 mx-auto mb-2" />
                <p className="text-sm text-ink-400">No notifications yet</p>
              </div>
            ) : (
              notifications.map(n => {
                const meta = TYPE_META[n.type] || TYPE_META.unusual_spending
                const Icon = meta.icon
                return (
                  <button
                    key={n.id}
                    onClick={() => handleClick(n)}
                    className={`w-full text-left flex gap-3 px-4 py-3 border-b border-surface-50 transition-colors last:border-0 ${
                      n.read ? 'hover:bg-surface-50' : 'bg-brand-50/40 hover:bg-brand-50'
                    }`}
                  >
                    <div
                      className="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5"
                      style={{ background: meta.bg }}
                    >
                      <Icon size={14} style={{ color: meta.color }} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <p className={`text-xs font-semibold text-ink-800 leading-tight ${!n.read ? 'font-bold' : ''}`}>
                          {n.title}
                        </p>
                        {!n.read && (
                          <div className="w-1.5 h-1.5 bg-brand-500 rounded-full flex-shrink-0 mt-1" />
                        )}
                      </div>
                      <p className="text-xs text-ink-500 mt-0.5 line-clamp-2">{n.body}</p>
                      <p className="text-[10px] text-ink-300 mt-1">
                        {timeAgo(n.createdAt)}
                      </p>
                    </div>
                  </button>
                )
              })
            )}
          </div>
        </div>
      )}
    </div>
  )
}
