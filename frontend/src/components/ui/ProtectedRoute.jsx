import { Navigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

function FullPageLoader() {
  return (
    <div className="min-h-screen bg-surface-50 flex items-center justify-center">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm text-ink-400 font-medium">Loading FinSmart...</p>
      </div>
    </div>
  )
}

/**
 * Wraps a route that requires authentication.
 * Redirects to /login if the user is not authenticated.
 * Shows a full-page loader while auth state is being restored on mount.
 */
export function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth()
  if (isLoading) return <FullPageLoader />
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

/**
 * Wraps a route that should only be accessible when NOT authenticated.
 * Redirects to /dashboard if the user is already logged in.
 */
export function PublicRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth()
  if (isLoading) return <FullPageLoader />
  return !isAuthenticated ? children : <Navigate to="/dashboard" replace />
}
