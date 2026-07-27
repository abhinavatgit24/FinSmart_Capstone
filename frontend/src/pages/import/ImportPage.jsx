import { useState, useRef } from 'react'
import { importApi } from '../../api'
import { formatCurrency } from '../../utils/formatCurrency'
import { getCategoryIcon } from '../../utils/categories'
import {
  Upload, FileText, CheckCircle2, XCircle,
  AlertTriangle, ArrowRight, Download
} from 'lucide-react'

const SAMPLE_CSV = `date,amount,type,description,category
01/05/2025,4500,expense,Swiggy order,Food
02/05/2025,50000,income,Salary May,Salary
03/05/2025,1299,expense,Netflix subscription,Entertainment
05/05/2025,800,expense,Uber to office,Travel
10/05/2025,2300,expense,Big Basket groceries,Food`

function downloadSample() {
  const blob = new Blob([SAMPLE_CSV], { type: 'text/csv' })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href = url; a.download = 'finsmart_sample.csv'; a.click()
  URL.revokeObjectURL(url)
}

export default function ImportPage() {
  const [file,       setFile]       = useState(null)
  const [uploading,  setUploading]  = useState(false)
  const [result,     setResult]     = useState(null)
  const [error,      setError]      = useState(null)
  const [dragging,   setDragging]   = useState(false)
  const inputRef = useRef(null)

  const handleFile = (f) => {
    if (!f) return
    if (!f.name.endsWith('.csv') && f.type !== 'text/csv') {
      setError('Please upload a .csv file')
      return
    }
    setFile(f)
    setResult(null)
    setError(null)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    const f = e.dataTransfer.files[0]
    handleFile(f)
  }

  const handleUpload = async () => {
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      const res = await importApi.uploadCsv(file)
      setResult(res)
      if (!res.success) setError(res.message || 'Import failed')
    } catch (e) {
      setError(e.response?.data?.message || 'Upload failed. Check your CSV format.')
    } finally {
      setUploading(false)
    }
  }

  const reset = () => { setFile(null); setResult(null); setError(null) }

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h2 className="text-lg font-semibold text-ink-900">Import CSV</h2>
        <p className="text-sm text-ink-400 mt-0.5">Upload your bank statement to bulk-import transactions</p>
      </div>

      {/* Format guide */}
      <div className="card bg-surface-50 border-surface-200 space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm font-semibold text-ink-800">Accepted CSV Format</p>
          <button
            onClick={downloadSample}
            className="btn-ghost text-xs flex items-center gap-1.5"
          >
            <Download size={12} /> Sample CSV
          </button>
        </div>
        <div className="text-xs text-ink-500 space-y-1">
          <p><strong>Required columns:</strong> <code className="bg-surface-200 px-1 rounded">date</code>, <code className="bg-surface-200 px-1 rounded">amount</code>, <code className="bg-surface-200 px-1 rounded">type</code> (income/expense)</p>
          <p><strong>Optional columns:</strong> <code className="bg-surface-200 px-1 rounded">description</code>, <code className="bg-surface-200 px-1 rounded">category</code></p>
          <p><strong>Bank exports:</strong> <code className="bg-surface-200 px-1 rounded">debit</code> + <code className="bg-surface-200 px-1 rounded">credit</code> columns instead of amount+type are supported</p>
          <p><strong>Date formats:</strong> dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd, dd MMM yyyy</p>
        </div>
      </div>

      {/* Drop zone */}
      {!result && (
        <div
          onDragOver={e => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          onClick={() => inputRef.current?.click()}
          className={`border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer transition-all ${
            dragging
              ? 'border-brand-400 bg-brand-50'
              : file
              ? 'border-emerald-300 bg-emerald-50'
              : 'border-surface-300 hover:border-brand-300 hover:bg-brand-50/30'
          }`}
        >
          <input
            ref={inputRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={e => handleFile(e.target.files?.[0])}
          />

          {file ? (
            <div className="space-y-2">
              <FileText size={32} className="mx-auto text-emerald-500" />
              <p className="text-sm font-semibold text-emerald-700">{file.name}</p>
              <p className="text-xs text-emerald-600">{(file.size / 1024).toFixed(1)} KB · Click to change</p>
            </div>
          ) : (
            <div className="space-y-2">
              <Upload size={32} className="mx-auto text-ink-300" />
              <p className="text-sm font-semibold text-ink-600">Drop your CSV here or click to browse</p>
              <p className="text-xs text-ink-400">Supports bank statement exports from most Indian banks</p>
            </div>
          )}
        </div>
      )}

      {error && (
        <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-700">
          <AlertTriangle size={15} className="flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Action buttons */}
      {file && !result && (
        <div className="flex gap-3">
          <button onClick={reset} className="btn-secondary">Cancel</button>
          <button
            onClick={handleUpload}
            disabled={uploading}
            className="btn-primary flex items-center gap-2 flex-1 justify-center"
          >
            {uploading ? (
              <>Importing…</>
            ) : (
              <>Import Transactions <ArrowRight size={14} /></>
            )}
          </button>
        </div>
      )}

      {/* Result */}
      {result?.data && (
        <div className="space-y-4 animate-slide-up">
          {/* Summary */}
          <div className="grid grid-cols-3 gap-4">
            {[
              { label: 'Total Rows',  value: result.data.totalRows,  color: '#444ce7' },
              { label: 'Imported',    value: result.data.imported,   color: '#12b76a' },
              { label: 'Skipped',     value: result.data.skipped,    color: '#f59e0b' },
            ].map(s => (
              <div key={s.label} className="card text-center">
                <p className="text-xs text-ink-400 mb-1">{s.label}</p>
                <p className="text-2xl font-semibold" style={{ color: s.color }}>{s.value}</p>
              </div>
            ))}
          </div>

          {/* Row errors */}
          {result.data.errors?.length > 0 && (
            <div className="card bg-amber-50 border-amber-200 space-y-1">
              <p className="text-xs font-semibold text-amber-800 flex items-center gap-1.5">
                <AlertTriangle size={12} /> Skipped rows
              </p>
              <ul className="text-xs text-amber-700 space-y-0.5 pl-4 list-disc">
                {result.data.errors.slice(0, 5).map((e, i) => <li key={i}>{e}</li>)}
                {result.data.errors.length > 5 && (
                  <li>…and {result.data.errors.length - 5} more</li>
                )}
              </ul>
            </div>
          )}

          {/* Imported transactions preview */}
          {result.data.transactions?.length > 0 && (
            <div className="card">
              <p className="text-sm font-semibold text-ink-800 mb-3">
                ✅ {result.data.imported} transactions imported
              </p>
              <div className="divide-y divide-surface-100">
                {result.data.transactions.slice(0, 10).map(t => (
                  <div key={t.id} className="flex items-center gap-3 py-2.5">
                    <span className="text-base">{getCategoryIcon(t.category)}</span>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium text-ink-800 truncate">
                        {t.description || t.category}
                      </p>
                      <p className="text-[10px] text-ink-400">{t.date} · {t.category}</p>
                    </div>
                    <p className={`text-xs font-semibold flex-shrink-0 ${
                      t.type === 'income' ? 'text-emerald-600' : 'text-red-500'
                    }`}>
                      {t.type === 'income' ? '+' : '-'}{formatCurrency(t.amount)}
                    </p>
                  </div>
                ))}
                {result.data.imported > 10 && (
                  <p className="text-xs text-ink-400 pt-2 text-center">
                    …and {result.data.imported - 10} more in Transactions
                  </p>
                )}
              </div>
            </div>
          )}

          <button onClick={reset} className="btn-secondary w-full">Import Another File</button>
        </div>
      )}
    </div>
  )
}
