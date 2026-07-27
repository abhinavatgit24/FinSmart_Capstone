import { useState, useEffect, useCallback } from 'react'
import { analyticsApi } from '../api'

export function useAnalytics(months = 6) {
  const [analytics, setAnalytics] = useState(null)
  const [isLoading, setLoading]   = useState(true)
  const [error,     setError]     = useState(null)

  const fetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await analyticsApi.get(months)
      setAnalytics(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load analytics')
    } finally {
      setLoading(false)
    }
  }, [months])

  useEffect(() => { fetch() }, [fetch])

  return { analytics, isLoading, error, refresh: fetch }
}
