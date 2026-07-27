import axios from 'axios'

// Module-level token store — avoids circular imports with AuthContext
let _accessToken = null
let _onRefreshFailed = null

export const setAccessToken  = (token)  => { _accessToken = token }
export const setRefreshFailedCallback = (cb) => { _onRefreshFailed = cb }

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
})

// ── Request interceptor — attach JWT ──────────────────────────────────────────
api.interceptors.request.use(config => {
  if (_accessToken) {
    config.headers.Authorization = `Bearer ${_accessToken}`
  }
  return config
})

// ── Response interceptor — silent refresh on 401 ─────────────────────────────
api.interceptors.response.use(
  res => res,
  async err => {
    const original = err.config
    if (err.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = localStorage.getItem('fs_rt')
        if (!refreshToken) throw new Error('No refresh token')

        // Use full base URL — not relative path — so this works in production
        // where frontend and backend are on different domains
        const baseURL = import.meta.env.VITE_API_URL || '/api'
        const { data } = await axios.post(`${baseURL}/auth/refresh`, { refreshToken })
        const newToken = data.data.accessToken
        setAccessToken(newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      } catch {
        setAccessToken(null)
        localStorage.removeItem('fs_rt')
        if (_onRefreshFailed) _onRefreshFailed()
      }
    }
    return Promise.reject(err)
  }
)

export default api
