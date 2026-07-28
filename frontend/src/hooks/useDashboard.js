import { useState, useEffect, useCallback } from 'react'
import { transactionApi } from '../api'

export function useDashboard() {
  const [summary, setSummary]   = useState(null)
  const [isLoading, setLoading] = useState(true)
  const [error, setError]       = useState(null)

  const fetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await transactionApi.getDashboard()
      setSummary(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load dashboard')
    } finally {
      setLoading(false)
    }
  }, [])  // stable — no external dependencies

  useEffect(() => { fetch() }, [])

  return { summary, isLoading, error, refresh: fetch }
}
