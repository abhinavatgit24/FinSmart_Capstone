import api from './axiosInstance'

// ── Auth API ──────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data)         => api.post('/auth/register', data).then(r => r.data.data),
  login:    (data)         => api.post('/auth/login',    data).then(r => r.data.data),
  refresh:  (refreshToken) => api.post('/auth/refresh',  { refreshToken }).then(r => r.data.data),
  getMe:    ()             => api.get('/auth/me').then(r => r.data.data),
  updateProfile: (data)    => api.put('/auth/me', data).then(r => r.data.data),
}

// ── Transaction API ───────────────────────────────────────────────────────────
export const transactionApi = {
  getAll:      (params = {}) => api.get('/transactions', { params }).then(r => r.data.data),
  getOne:      (id)          => api.get(`/transactions/${id}`).then(r => r.data.data),
  add:         (data)        => api.post('/transactions', data).then(r => r.data.data),
  update:      (id, data)    => api.put(`/transactions/${id}`, data).then(r => r.data.data),
  remove:      (id)          => api.delete(`/transactions/${id}`).then(r => r.data),
  getDashboard:()            => api.get('/dashboard/summary').then(r => r.data.data),
  getCategories: ()          => api.get('/transactions/categories').then(r => r.data.data),
}

// ── Budget API ────────────────────────────────────────────────────────────────
export const budgetApi = {
  getAll:          ()         => api.get('/budgets').then(r => r.data.data),
  getUtilisation:  ()         => api.get('/budgets/utilisation').then(r => r.data.data),
  createOrUpdate:  (data)     => api.post('/budgets', data).then(r => r.data.data),
  remove:          (id)       => api.delete(`/budgets/${id}`).then(r => r.data),
}

// ── Savings Goals API ─────────────────────────────────────────────────────────
export const goalsApi = {
  getAll:  ()           => api.get('/goals').then(r => r.data.data),
  getOne:  (id)         => api.get(`/goals/${id}`).then(r => r.data.data),
  create:  (data)       => api.post('/goals', data).then(r => r.data.data),
  update:  (id, data)   => api.put(`/goals/${id}`, data).then(r => r.data.data),
  remove:  (id)         => api.delete(`/goals/${id}`).then(r => r.data),
}

// ── Financial Health API ──────────────────────────────────────────────────────
export const healthApi = {
  getScore: () => api.get('/health/score').then(r => r.data.data),
}

// ── Analytics API ─────────────────────────────────────────────────────────────
export const analyticsApi = {
  get:             (months = 6) => api.get(`/analytics?months=${months}`).then(r => r.data.data),
  getSubscriptions:()           => api.get('/analytics/subscriptions').then(r => r.data.data),
}

// ── CSV Import API ────────────────────────────────────────────────────────────
export const importApi = {
  uploadCsv: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/import/csv', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
}

// ── Report API ────────────────────────────────────────────────────────────────
export const reportApi = {
  getMonthlyHtml: (year, month) =>
    api.get(`/report/monthly?year=${year}&month=${month}`, { responseType: 'text' }).then(r => r.data),
}

// ── Notifications API ─────────────────────────────────────────────────────────
export const notificationsApi = {
  getAll:        ()   => api.get('/notifications').then(r => r.data.data),
  getUnreadCount:()   => api.get('/notifications/unread-count').then(r => r.data.data.count),
  markRead:      (id) => api.patch(`/notifications/${id}/read`).then(r => r.data),
  markAllRead:   ()   => api.patch('/notifications/read-all').then(r => r.data),
}
