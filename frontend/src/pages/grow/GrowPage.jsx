import { useMemo, useState } from 'react'
import { AreaChart, Area, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TrendingUp, Landmark, PieChart, HandCoins, Info, ArrowRight } from 'lucide-react'
import { formatCurrency } from '../../utils/formatCurrency'

const PRODUCTS = [
  { id: 'sip', label: 'SIP', icon: TrendingUp, color: '#12b76a', description: 'Invest a fixed amount every month.' },
  { id: 'mf', label: 'Mutual Fund', icon: PieChart, color: '#7c3aed', description: 'Estimate a one-time investment.' },
  { id: 'fd', label: 'Fixed Deposit', icon: Landmark, color: '#2563eb', description: 'Calculate fixed-return maturity.' },
  { id: 'emi', label: 'EMI / Loan', icon: HandCoins, color: '#f59e0b', description: 'Plan repayments before borrowing.' },
]

function NumberField({ label, value, onChange, suffix, min = 0, step = 1, hint }) {
  return <label className="block">
    <span className="label">{label}</span>
    <div className="relative">
      <input className="input pr-14" type="number" min={min} step={step} value={value}
        onChange={e => onChange(Math.max(min, Number(e.target.value) || 0))} />
      {suffix && <span className="absolute right-3 top-2.5 text-sm text-ink-400">{suffix}</span>}
    </div>
    {hint && <span className="text-xs text-ink-400 mt-1 block">{hint}</span>}
  </label>
}

function Metric({ label, value, tone = 'text-ink-900' }) {
  return <div className="rounded-xl bg-surface-50 border border-surface-200 p-3.5">
    <p className="text-xs text-ink-500 mb-1">{label}</p>
    <p className={`text-base font-semibold ${tone}`}>{value}</p>
  </div>
}

function Projection({ data, color, valueLabel = 'Value' }) {
  return <div className="h-48 mt-5">
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={{ top: 8, right: 4, left: -18, bottom: 0 }}>
        <defs><linearGradient id="growth" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={color} stopOpacity={.25}/><stop offset="100%" stopColor={color} stopOpacity={0}/></linearGradient></defs>
        <XAxis dataKey="year" tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: '#98a2b3' }} />
        <YAxis tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: '#98a2b3' }} tickFormatter={v => `₹${Math.round(v / 1000)}k`} />
        <Tooltip formatter={v => [formatCurrency(v), valueLabel]} labelFormatter={v => `Year ${v}`} />
        <Area type="monotone" dataKey="value" stroke={color} strokeWidth={2.5} fill="url(#growth)" />
      </AreaChart>
    </ResponsiveContainer>
  </div>
}

function SipCalculator() {
  const [monthly, setMonthly] = useState(5000), [rate, setRate] = useState(12), [years, setYears] = useState(10), [stepUp, setStepUp] = useState(0)
  const result = useMemo(() => {
    let value = 0, contribution = 0, payment = monthly, rows = []
    for (let month = 1; month <= years * 12; month++) {
      value = (value + payment) * (1 + rate / 1200); contribution += payment
      if (month % 12 === 0) { rows.push({ year: month / 12, value: Math.round(value) }); payment *= 1 + stepUp / 100 }
    }
    return { value, contribution, rows }
  }, [monthly, rate, years, stepUp])
  return <CalculatorLayout title="SIP Calculator" caption="Estimate the future value of regular monthly investments." color="#12b76a" fields={<>
    <NumberField label="Monthly investment" value={monthly} onChange={setMonthly} suffix="₹" step={500} />
    <NumberField label="Expected annual return" value={rate} onChange={setRate} suffix="%" step={.1} />
    <NumberField label="Investment period" value={years} onChange={setYears} suffix="years" min={1} />
    <NumberField label="Annual SIP increase" value={stepUp} onChange={setStepUp} suffix="%" step={1} hint="Optional yearly step-up" />
  </>} metrics={<><Metric label="Total invested" value={formatCurrency(result.contribution)} /><Metric label="Estimated returns" value={formatCurrency(result.value - result.contribution)} tone="text-[#12b76a]" /><Metric label="Estimated value" value={formatCurrency(result.value)} tone="text-brand-700" /></>} chart={<Projection data={result.rows} color="#12b76a" />} />
}

function MfCalculator() {
  const [amount, setAmount] = useState(100000), [rate, setRate] = useState(12), [years, setYears] = useState(8), [inflation, setInflation] = useState(6)
  const result = useMemo(() => {
    const rows = Array.from({ length: years }, (_, i) => ({ year: i + 1, value: Math.round(amount * Math.pow(1 + rate / 100, i + 1)) }))
    const value = rows.at(-1)?.value || amount
    return { value, realValue: value / Math.pow(1 + inflation / 100, years), rows }
  }, [amount, rate, years, inflation])
  return <CalculatorLayout title="Mutual Fund Calculator" caption="Estimate a one-time investment; returns are illustrative, not guaranteed." color="#7c3aed" fields={<>
    <NumberField label="One-time investment" value={amount} onChange={setAmount} suffix="₹" step={1000} />
    <NumberField label="Expected annual return" value={rate} onChange={setRate} suffix="%" step={.1} />
    <NumberField label="Investment period" value={years} onChange={setYears} suffix="years" min={1} />
    <NumberField label="Expected inflation" value={inflation} onChange={setInflation} suffix="%" step={.1} />
  </>} metrics={<><Metric label="Amount invested" value={formatCurrency(amount)} /><Metric label="Estimated gains" value={formatCurrency(result.value - amount)} tone="text-[#12b76a]" /><Metric label="Future value" value={formatCurrency(result.value)} tone="text-brand-700" /><Metric label="Value in today's money" value={formatCurrency(result.realValue)} /></>} chart={<Projection data={result.rows} color="#7c3aed" />} />
}

function FdCalculator() {
  const [amount, setAmount] = useState(100000), [rate, setRate] = useState(7), [years, setYears] = useState(3), [frequency, setFrequency] = useState(4)
  const result = useMemo(() => {
    const valueAt = year => amount * Math.pow(1 + rate / (100 * frequency), frequency * year)
    const rows = Array.from({ length: years }, (_, i) => ({ year: i + 1, value: Math.round(valueAt(i + 1)) }))
    const value = valueAt(years)
    return { value, interest: value - amount, rows }
  }, [amount, rate, years, frequency])
  return <CalculatorLayout title="Fixed Deposit Calculator" caption="Estimate cumulative FD maturity using your selected compounding frequency." color="#2563eb" fields={<>
    <NumberField label="Deposit amount" value={amount} onChange={setAmount} suffix="₹" step={1000} />
    <NumberField label="Annual interest rate" value={rate} onChange={setRate} suffix="%" step={.1} />
    <NumberField label="Tenure" value={years} onChange={setYears} suffix="years" min={1} />
    <label><span className="label">Compounding frequency</span><select className="input" value={frequency} onChange={e => setFrequency(Number(e.target.value))}><option value={1}>Yearly</option><option value={2}>Half-yearly</option><option value={4}>Quarterly</option><option value={12}>Monthly</option></select></label>
  </>} metrics={<><Metric label="Principal" value={formatCurrency(amount)} /><Metric label="Interest earned" value={formatCurrency(result.interest)} tone="text-[#12b76a]" /><Metric label="Maturity value" value={formatCurrency(result.value)} tone="text-brand-700" /></>} chart={<Projection data={result.rows} color="#2563eb" />} />
}

function EmiCalculator() {
  const [amount, setAmount] = useState(500000), [rate, setRate] = useState(10), [years, setYears] = useState(5), [fee, setFee] = useState(1)
  const result = useMemo(() => {
    const months = years * 12, monthlyRate = rate / 1200
    const emi = monthlyRate ? amount * monthlyRate * Math.pow(1 + monthlyRate, months) / (Math.pow(1 + monthlyRate, months) - 1) : amount / months
    let balance = amount, rows = []
    for (let month = 1; month <= months; month++) {
      const interest = balance * monthlyRate; balance = Math.max(0, balance + interest - emi)
      if (month % 12 === 0 || month === months) rows.push({ year: Math.ceil(month / 12), value: Math.round(balance) })
    }
    return { emi, total: emi * months, interest: emi * months - amount, fee: amount * fee / 100, rows }
  }, [amount, rate, years, fee])
  return <CalculatorLayout title="EMI / Loan Calculator" caption="Plan the cost of borrowing before you take a loan." color="#f59e0b" fields={<>
    <NumberField label="Loan amount" value={amount} onChange={setAmount} suffix="₹" step={5000} />
    <NumberField label="Annual interest rate" value={rate} onChange={setRate} suffix="%" step={.1} />
    <NumberField label="Loan tenure" value={years} onChange={setYears} suffix="years" min={1} />
    <NumberField label="Processing fee" value={fee} onChange={setFee} suffix="%" step={.1} />
  </>} metrics={<><Metric label="Monthly EMI" value={formatCurrency(result.emi)} tone="text-brand-700" /><Metric label="Total interest" value={formatCurrency(result.interest)} tone="text-[#f04438]" /><Metric label="Total payable" value={formatCurrency(result.total + result.fee)} /><Metric label="Processing fee" value={formatCurrency(result.fee)} /></>} chart={<Projection data={result.rows} color="#f59e0b" valueLabel="Outstanding balance" />} />
}

function CalculatorLayout({ title, caption, color, fields, metrics, chart }) {
  return <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
    <div className="card lg:col-span-2"><h2 className="text-lg font-semibold text-ink-900">{title}</h2><p className="text-sm text-ink-400 mt-1 mb-6">{caption}</p><div className="space-y-4">{fields}</div></div>
    <div className="card lg:col-span-3"><p className="text-sm font-semibold text-ink-800">Illustrative projection</p><div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4">{metrics}</div>{chart}</div>
  </div>
}

export default function GrowPage() {
  const [active, setActive] = useState('sip')
  const Calculator = active === 'sip' ? SipCalculator : active === 'mf' ? MfCalculator : active === 'fd' ? FdCalculator : EmiCalculator
  return <div className="space-y-6">
    <div className="flex items-start gap-4"><div className="w-11 h-11 rounded-2xl bg-brand-100 text-brand-600 flex items-center justify-center"><TrendingUp size={22} /></div><div><h2 className="text-xl font-semibold text-ink-900">Grow your money</h2><p className="text-sm text-ink-400 mt-1">Explore scenarios before you invest or borrow. These are calculators, not financial advice.</p></div></div>
    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">{PRODUCTS.map(product => { const Icon = product.icon; const selected = active === product.id; return <button key={product.id} onClick={() => setActive(product.id)} className={`text-left rounded-2xl border p-4 transition-all ${selected ? 'bg-white border-brand-400 shadow-card ring-1 ring-brand-200' : 'bg-white border-surface-200 hover:border-brand-200 hover:shadow-card'}`}><div className="flex items-center justify-between"><div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: product.color + '18', color: product.color }}><Icon size={18} /></div>{selected && <ArrowRight size={17} className="text-brand-600" />}</div><p className="font-semibold text-ink-800 mt-4">{product.label}</p><p className="text-xs text-ink-400 mt-1 leading-5">{product.description}</p></button> })}</div>
    <Calculator />
    <div className="flex items-start gap-3 rounded-2xl border border-brand-100 bg-brand-50/50 p-4 text-sm text-ink-600"><Info size={18} className="text-brand-600 shrink-0 mt-0.5" /><p>All projections use the values you enter and are for planning only. Investment returns are not guaranteed; verify rates, tax treatment, and terms independently before making a decision.</p></div>
  </div>
}
