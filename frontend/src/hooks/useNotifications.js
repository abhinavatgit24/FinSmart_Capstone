import { useState, useEffect, useCallback, useRef } from 'react'
import { notificationsApi } from '../api'

const POLL_INTERVAL = 60_000 // 60s

export function useNotifications() {
  const [notifications, setNotifications] = useState([])
  const [unreadCount,   setUnreadCount]   = useState(0)
  const [isLoading,     setLoading]       = useState(true)
  const timerRef = useRef(null)

  const fetchAll = useCallback(async () => {
    try {
      const data = await notificationsApi.getAll()
      setNotifications(data)
      setUnreadCount(data.filter(n => !n.read).length)
    } catch { /* silent */ }
    finally { setLoading(false) }
  }, [])

  useEffect(() => {
    fetchAll()
    timerRef.current = setInterval(fetchAll, POLL_INTERVAL)
    return () => clearInterval(timerRef.current)
  }, [fetchAll])

  const markRead = async (id) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n))
    setUnreadCount(prev => Math.max(0, prev - 1))
    await notificationsApi.markRead(id)
  }

  const markAllRead = async () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    setUnreadCount(0)
    await notificationsApi.markAllRead()
  }

  return { notifications, unreadCount, isLoading, fetchAll, markRead, markAllRead }
}
