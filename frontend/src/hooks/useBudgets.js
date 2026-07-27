import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { budgetApi } from '../api'

export function useBudgets() {
  const [utilisations, setUtilisations] = useState([])
  const [isLoading, setLoading]         = useState(true)
  const [error, setError]               = useState(null)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await budgetApi.getUtilisation()
      setUtilisations(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load budgets')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const saveBudget = async (formData) => {
    try {
      await budgetApi.createOrUpdate(formData)
      toast.success('Budget saved')
      await fetchAll()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to save budget')
      throw e
    }
  }

  const deleteBudget = async (id) => {
    const backup = [...utilisations]
    setUtilisations(prev => prev.filter(b => b.id !== id))
    try {
      await budgetApi.remove(id)
      toast.success('Budget removed')
    } catch (e) {
      setUtilisations(backup)
      toast.error(e.response?.data?.message || 'Failed to delete budget')
      throw e
    }
  }

  return { utilisations, isLoading, error, fetchAll, saveBudget, deleteBudget }
}
