import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute, PublicRoute } from './components/ui/ProtectedRoute'
import LoginPage        from './pages/auth/LoginPage'
import RegisterPage     from './pages/auth/RegisterPage'
import DashboardPage    from './pages/dashboard/DashboardPage'
import TransactionsPage from './pages/transactions/TransactionsPage'
import BudgetsPage      from './pages/budgets/BudgetsPage'
import GoalsPage        from './pages/goals/GoalsPage'
import HealthScorePage  from './pages/health/HealthScorePage'
import AnalyticsPage    from './pages/analytics/AnalyticsPage'
import ImportPage       from './pages/import/ImportPage'
import AppLayout        from './components/layout/AppLayout'

export default function App() {
  return (
    <Routes>
      <Route path="/login"    element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

      <Route path="/" element={
        <ProtectedRoute>
          <AppLayout />
        </ProtectedRoute>
      }>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard"    element={<DashboardPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="budgets"      element={<BudgetsPage />} />
        <Route path="goals"        element={<GoalsPage />} />
        <Route path="analytics"    element={<AnalyticsPage />} />
        <Route path="import"       element={<ImportPage />} />
        <Route path="health"       element={<HealthScorePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
