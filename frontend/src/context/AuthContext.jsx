import { createContext, useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { authApi } from '../api'
import {
  setAccessToken,
  setRefreshFailedCallback
} from '../api/axiosInstance'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser]               = useState(null)
  const [isAuthenticated, setIsAuth]  = useState(false)
  const [isLoading, setIsLoading]     = useState(true)
  const navigate = useNavigate()

  // Called when refresh fails — force logout
  const forceLogout = useCallback(() => {
    setUser(null)
    setIsAuth(false)
    setAccessToken(null)
    navigate('/login')
  }, [navigate])

  useEffect(() => {
    setRefreshFailedCallback(forceLogout)
  }, [forceLogout])

  // On app load: try to restore session from stored refresh token
  useEffect(() => {
    const restore = async () => {
      const rt = localStorage.getItem('fs_rt')
      if (!rt) { setIsLoading(false); return }
      try {
        const data = await authApi.refresh(rt)
        setAccessToken(data.accessToken)
        localStorage.setItem('fs_rt', data.refreshToken)
        setUser(data.user)
        setIsAuth(true)
      } catch {
        localStorage.removeItem('fs_rt')
      } finally {
        setIsLoading(false)
      }
    }
    restore()
  }, [])

  const login = async (email, password) => {
    const data = await authApi.login({ email, password })
    setAccessToken(data.accessToken)
    localStorage.setItem('fs_rt', data.refreshToken)
    setUser(data.user)
    setIsAuth(true)
    return data
  }

  const register = async (name, email, password) => {
    const data = await authApi.register({ name, email, password })
    setAccessToken(data.accessToken)
    localStorage.setItem('fs_rt', data.refreshToken)
    setUser(data.user)
    setIsAuth(true)
    return data
  }

  const logout = () => {
    setAccessToken(null)
    localStorage.removeItem('fs_rt')
    setUser(null)
    setIsAuth(false)
    toast.success('Signed out')
    navigate('/login')
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
