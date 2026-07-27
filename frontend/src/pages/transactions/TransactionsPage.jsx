import { useState } from 'react'
import { useTransactions } from '../../hooks/useTransactions'
import { TransactionForm } from '../../components/ui/TransactionForm'
import { CategoryBadge } from '../../components/ui/CategoryBadge'
import { formatCurrency } from '../../utils/formatCurrency'
import { CATEGORIES } from '../../utils/categories'
import {
  Plus, Search, Filter, Pencil, Trash2,
  ArrowUpRight, ArrowDownRight, ChevronDown, X
} from 'lucide-react'

function ConfirmDialog({ message, onConfirm, onCancel }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-card-lg p-6 max-w-sm w-full animate-slide-up">
        <h3 className="text-base font-semibold text-ink-900 mb-2">Delete Transaction</h3>
        <p className="text-sm text-ink-500 mb-5">{message}</p>
        <div className="flex gap-3">
          <button onClick={onCancel} className="btn-secondary flex-1">Cancel</button>
          <button
            onClick={onConfirm}
            className="flex-1 bg-red-500 text-white font-medium px-5 py-2.5 rounded-xl
                       hover:bg-red-600 active:bg-red-700 transition-all duration-150
                       focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-2"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  )
}

export default function TransactionsPage() {
  const { transactions, isLoading, addTransaction, updateTransaction, deleteTransaction } = useTransactions()

  const [showForm, setShowForm]       = useState(false)
  const [editing, setEditing]         = useState(null)
  const [deleting, setDeleting]       = useState(null)
  const [search, setSearch]           = useState('')
  const [filterType, setFilterType]   = useState('')
  const [filterCat, setFilterCat]     = useState('')

  const handleAdd = async (data) => {
    await addTransaction(data)
    setShowForm(false)
  }

  const handleEdit = async (data) => {
    await updateTransaction(editing.id, data)
    setEditing(null)
  }

  const handleDelete = async () => {
    await deleteTransaction(deleting.id)
    setDeleting(null)
  }

  // Client-side filter
  const filtered = transactions.filter(t => {
    const matchSearch = !search ||
      (t.description || '').toLowerCase().includes(search.toLowerCase()) ||
      (t.category || '').toLowerCase().includes(search.toLowerCase())
    const matchType = !filterType || t.type === filterType
    const matchCat  = !filterCat  || t.category === filterCat
    return matchSearch && matchType && matchCat
  })

  const hasFilters = search || filterType || filterCat
  const clearFilters = () => { setSearch(''); setFilterType(''); setFilterCat('') }

  // Totals for filtered set
  const filteredIncome  = filtered.filter(t => t.type === 'income').reduce((s, t) => s + t.amount, 0)
  const filteredExpense = filtered.filter(t => t.type === 'expense').reduce((s, t) => s + t.amount, 0)

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-ink-900">All Transactions</h2>
          <p className="text-sm text-ink-400 mt-0.5">
            {filtered.length} record{filtered.length !== 1 ? 's' : ''}
            {hasFilters && ' (filtered)'}
          </p>
        </div>
        <button onClick={() => setShowForm(true)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Add
        </button>
      </div>

      {/* Quick totals bar */}
      {filtered.length > 0 && (
        <div className="flex gap-3 flex-wrap">
          <div className="flex items-center gap-2 bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-lg text-xs font-medium">
            <ArrowUpRight size={13} /> Income: {formatCurrency(filteredIncome)}
          </div>
          <div className="flex items-center gap-2 bg-red-50 text-red-600 px-3 py-1.5 rounded-lg text-xs font-medium">
            <ArrowDownRight size={13} /> Expenses: {formatCurrency(filteredExpense)}
          </div>
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium
            ${filteredIncome - filteredExpense >= 0
              ? 'bg-brand-50 text-brand-700'
              : 'bg-orange-50 text-orange-700'
            }`}>
            Net: {formatCurrency(filteredIncome - filteredExpense)}
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="card !p-4">
        <div className="flex flex-wrap gap-3">
          {/* Search */}
          <div className="relative flex-1 min-w-48">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              type="text" placeholder="Search description or category..."
              value={search} onChange={e => setSearch(e.target.value)}
              className="input pl-8 text-sm"
            />
          </div>

          {/* Type filter */}
          <div className="relative">
            <select
              value={filterType} onChange={e => setFilterType(e.target.value)}
              className="input text-sm pr-8 appearance-none cursor-pointer min-w-32"
            >
              <option value="">All types</option>
              <option value="income">Income</option>
              <option value="expense">Expense</option>
            </select>
            <ChevronDown size={13} className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 pointer-events-none" />
          </div>

          {/* Category filter */}
          <div className="relative">
            <select
              value={filterCat} onChange={e => setFilterCat(e.target.value)}
              className="input text-sm pr-8 appearance-none cursor-pointer min-w-36"
            >
              <option value="">All categories</option>
              {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
            <ChevronDown size={13} className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 pointer-events-none" />
          </div>

          {hasFilters && (
            <button onClick={clearFilters} className="btn-ghost flex items-center gap-1.5 text-sm text-ink-500">
              <X size={14} /> Clear
            </button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="card !p-0 overflow-hidden">
        {isLoading ? (
          <div className="p-8 space-y-3">
            {[1,2,3,4,5].map(i => (
              <div key={i} className="h-12 bg-surface-100 rounded-xl animate-pulse" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 flex flex-col items-center gap-3 text-center">
            <div className="w-12 h-12 rounded-2xl bg-surface-100 flex items-center justify-center text-2xl">
              {hasFilters ? '🔍' : '💳'}
            </div>
            <div>
              <p className="text-sm font-medium text-ink-700">
                {hasFilters ? 'No matching transactions' : 'No transactions yet'}
              </p>
              <p className="text-xs text-ink-400 mt-1">
                {hasFilters ? 'Try adjusting your filters' : 'Click Add to record your first one'}
              </p>
            </div>
          </div>
        ) : (
          <>
            {/* Table header */}
            <div className="hidden sm:grid grid-cols-[1fr_100px_110px_80px_80px] gap-4
                            px-5 py-3 border-b border-surface-100 bg-surface-50">
              {['Description', 'Category', 'Date', 'Amount', ''].map((h, i) => (
                <span key={i} className="text-xs font-semibold text-ink-400 uppercase tracking-wider">{h}</span>
              ))}
            </div>

            {/* Rows */}
            <div className="divide-y divide-surface-100">
              {filtered.map(txn => (
                <div
                  key={txn.id}
                  className="grid grid-cols-[1fr_auto] sm:grid-cols-[1fr_100px_110px_80px_80px]
                             gap-3 sm:gap-4 px-5 py-4 hover:bg-surface-50 transition-colors group items-center"
                >
                  {/* Description */}
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-ink-800 truncate">
                      {txn.description || '—'}
                    </p>
                    <div className="sm:hidden flex items-center gap-2 mt-1">
                      <CategoryBadge category={txn.category} size="xs" />
                      <span className="text-xs text-ink-400">{txn.date}</span>
                    </div>
                  </div>

                  {/* Category — desktop */}
                  <div className="hidden sm:block">
                    <CategoryBadge category={txn.category} size="xs" />
                  </div>

                  {/* Date — desktop */}
                  <span className="hidden sm:block text-sm text-ink-500">{txn.date}</span>

                  {/* Amount */}
                  <div className="text-right sm:text-left">
                    <span className={`text-sm font-semibold flex items-center justify-end sm:justify-start gap-0.5 ${
                      txn.type === 'income' ? 'text-emerald-600' : 'text-red-500'
                    }`}>
                      {txn.type === 'income'
                        ? <ArrowUpRight size={13} />
                        : <ArrowDownRight size={13} />
                      }
                      {formatCurrency(txn.amount)}
                    </span>
                  </div>

                  {/* Actions */}
                  <div className="hidden sm:flex items-center justify-end gap-1
                                  opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => setEditing(txn)}
                      className="p-1.5 rounded-lg text-ink-400 hover:text-brand-600 hover:bg-brand-50 transition-all"
                      title="Edit"
                    >
                      <Pencil size={13} />
                    </button>
                    <button
                      onClick={() => setDeleting(txn)}
                      className="p-1.5 rounded-lg text-ink-400 hover:text-red-500 hover:bg-red-50 transition-all"
                      title="Delete"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>

                  {/* Mobile actions */}
                  <div className="flex sm:hidden items-center gap-1">
                    <button onClick={() => setEditing(txn)} className="p-1.5 text-ink-400">
                      <Pencil size={13} />
                    </button>
                    <button onClick={() => setDeleting(txn)} className="p-1.5 text-ink-400">
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      {/* Modals */}
      {showForm && (
        <TransactionForm
          onSubmit={handleAdd}
          onCancel={() => setShowForm(false)}
        />
      )}

      {editing && (
        <TransactionForm
          initial={editing}
          onSubmit={handleEdit}
          onCancel={() => setEditing(null)}
        />
      )}

      {deleting && (
        <ConfirmDialog
          message={`Delete "${deleting.description || deleting.category}"? This cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  )
}
