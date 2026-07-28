import { useState, useEffect, useCallback, useRef } from 'react'
import toast from 'react-hot-toast'
import { transactionApi } from '../api'

export function useTransactions(filters = {}) {
  const [transactions, setTransactions] = useState([])
  const [isLoading, setIsLoading]       = useState(true)
  const [error, setError]               = useState(null)

  // Stable ref for filters to avoid stale closure issues
  const filtersRef = useRef(filters)
  filtersRef.current = filters

  const fetchAll = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await transactionApi.getAll(filtersRef.current)
      setTransactions(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to fetch transactions')
    } finally {
      setIsLoading(false)
    }
  }, []) // stable — uses ref internally

  useEffect(() => { fetchAll() }, [fetchAll])

  const addTransaction = async (formData) => {
    const tempId = `temp_${Date.now()}`
    const optimistic = { ...formData, id: tempId }
    setTransactions(prev => [optimistic, ...prev])
    try {
      const saved = await transactionApi.add(formData)
      setTransactions(prev => prev.map(t => t.id === tempId ? saved : t))
      toast.success('Transaction added')
      return saved
    } catch (e) {
      setTransactions(prev => prev.filter(t => t.id !== tempId))
      const msg = e.response?.data?.message || 'Failed to add transaction'
      toast.error(msg)
      throw e
    }
  }

  const updateTransaction = async (id, formData) => {
    const backup = [...transactions]
    setTransactions(prev => prev.map(t => t.id === id ? { ...t, ...formData } : t))
    try {
      const updated = await transactionApi.update(id, formData)
      setTransactions(prev => prev.map(t => t.id === id ? updated : t))
      toast.success('Transaction updated')
      return updated
    } catch (e) {
      setTransactions(backup)
      const msg = e.response?.data?.message || 'Failed to update transaction'
      toast.error(msg)
      throw e
    }
  }

  const deleteTransaction = async (id) => {
    const backup = [...transactions]
    setTransactions(prev => prev.filter(t => t.id !== id))
    try {
      await transactionApi.remove(id)
      toast.success('Transaction deleted')
    } catch (e) {
      setTransactions(backup)
      const msg = e.response?.data?.message || 'Failed to delete transaction'
      toast.error(msg)
      throw e
    }
  }

  return { transactions, isLoading, error, fetchAll, addTransaction, updateTransaction, deleteTransaction }
}
