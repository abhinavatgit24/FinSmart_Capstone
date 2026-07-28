import { useState } from 'react'
import api from '../api/axiosInstance'

export function useAskAi() {
  const [messages,  setMessages]  = useState([]) // { role: 'user'|'ai', text, contextSummary?, isError? }
  const [isLoading, setLoading]   = useState(false)

  const ask = async (question) => {
    setMessages(prev => [...prev, { role: 'user', text: question }])
    setLoading(true)
    try {
      const res = await api.post('/ai/ask', { question })
      const { answer, contextSummary } = res.data
      setMessages(prev => [...prev, { role: 'ai', text: answer, contextSummary }])
    } catch (e) {
      const msg = e.response?.data?.message || 'Something went wrong. Please try again.'
      setMessages(prev => [...prev, { role: 'ai', text: msg, isError: true }])
    } finally {
      setLoading(false)
    }
  }

  const clear = () => setMessages([])

  return { messages, isLoading, ask, clear }
}
