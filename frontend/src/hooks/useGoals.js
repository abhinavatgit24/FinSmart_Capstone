import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { goalsApi } from '../api'

export function useGoals() {
  const [goals, setGoals]     = useState([])
  const [isLoading, setLoading] = useState(true)
  const [error, setError]       = useState(null)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await goalsApi.getAll()
      setGoals(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load goals')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const createGoal = async (formData) => {
    try {
      const created = await goalsApi.create(formData)
      toast.success('Goal created!')
      await fetchAll()
      return created
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to create goal')
      throw e
    }
  }

  const updateGoal = async (id, formData) => {
    try {
      const updated = await goalsApi.update(id, formData)
      toast.success('Goal updated')
      await fetchAll()
      return updated
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to update goal')
      throw e
    }
  }

  const deleteGoal = async (id) => {
    const backup = [...goals]
    setGoals(prev => prev.filter(g => g.id !== id))
    try {
      await goalsApi.remove(id)
      toast.success('Goal deleted')
    } catch (e) {
      setGoals(backup)
      toast.error(e.response?.data?.message || 'Failed to delete goal')
      throw e
    }
  }

  return { goals, isLoading, error, fetchAll, createGoal, updateGoal, deleteGoal }
}
