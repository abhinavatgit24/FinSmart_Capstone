import { useState, useEffect, useCallback } from 'react'
import { healthApi } from '../api'

export function useHealthScore() {
  const [health, setHealth]     = useState(null)
  const [isLoading, setLoading] = useState(true)
  const [error, setError]       = useState(null)

  const fetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await healthApi.getScore()
      setHealth(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load health score')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetch() }, [fetch])

  return { health, isLoading, error, refresh: fetch }
}
