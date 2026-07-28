export const CATEGORIES = [
  'Food', 'Travel', 'Bills', 'Entertainment',
  'Shopping', 'Health', 'Education', 'Salary', 'Other'
]

export const CATEGORY_META = {
  Food:          { color: '#f97316', bg: '#fff7ed', icon: '🍜' },
  Travel:        { color: '#3b82f6', bg: '#eff6ff', icon: '✈️' },
  Bills:         { color: '#8b5cf6', bg: '#f5f3ff', icon: '📄' },
  Entertainment: { color: '#ec4899', bg: '#fdf2f8', icon: '🎬' },
  Shopping:      { color: '#f59e0b', bg: '#fffbeb', icon: '🛍️' },
  Health:        { color: '#10b981', bg: '#ecfdf5', icon: '❤️' },
  Education:     { color: '#6366f1', bg: '#eef2ff', icon: '📚' },
  Salary:        { color: '#12b76a', bg: '#f0fdf4', icon: '💰' },
  Other:         { color: '#94a3b8', bg: '#f8fafc', icon: '📌' },
}

export function getCategoryColor(cat) {
  return CATEGORY_META[cat]?.color || '#94a3b8'
}

export function getCategoryBg(cat) {
  return CATEGORY_META[cat]?.bg || '#f8fafc'
}

export function getCategoryIcon(cat) {
  return CATEGORY_META[cat]?.icon || '📌'
}

// Auto-categorise client-side (mirrors backend keyword map for instant preview)
const KEYWORD_MAP = {
  swiggy:'Food', zomato:'Food', bigbasket:'Food', dunzo:'Food',
  restaurant:'Food', cafe:'Food', grocery:'Food', blinkit:'Food',
  uber:'Travel', ola:'Travel', rapido:'Travel', irctc:'Travel',
  metro:'Travel', fuel:'Travel', petrol:'Travel', flight:'Travel', train:'Travel',
  rent:'Bills', electricity:'Bills', internet:'Bills', jio:'Bills',
  airtel:'Bills', wifi:'Bills', emi:'Bills', insurance:'Bills',
  netflix:'Entertainment', spotify:'Entertainment', hotstar:'Entertainment',
  prime:'Entertainment', movie:'Entertainment', cinema:'Entertainment',
  amazon:'Shopping', flipkart:'Shopping', myntra:'Shopping', nykaa:'Shopping',
  hospital:'Health', pharmacy:'Health', doctor:'Health', medicine:'Health', gym:'Health',
  udemy:'Education', coursera:'Education', college:'Education', fees:'Education',
  salary:'Salary', freelance:'Salary', stipend:'Salary', neft:'Salary', imps:'Salary',
}

export function autoDetectCategory(description) {
  if (!description) return null
  const lower = description.toLowerCase()
  for (const [kw, cat] of Object.entries(KEYWORD_MAP)) {
    if (lower.includes(kw)) return cat
  }
  return null
}
