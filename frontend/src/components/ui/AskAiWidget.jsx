import { useState, useRef, useEffect } from 'react'
import { Bot, X, Send, Loader2 } from 'lucide-react'
import { useAskAi } from '../../hooks/useAskAi'

const SUGGESTIONS = [
  'How much have I spent on food this month?',
  'What is my income this month?',
  'Am I on track for my savings goals?',
  'Which category am I overspending in?',
]

export function AskAiWidget() {
  const [open, setOpen]   = useState(false)
  const [input, setInput] = useState('')
  const { messages, isLoading, ask, clear } = useAskAi()
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const handleSend = (text) => {
    const q = (text ?? input).trim()
    if (!q || isLoading) return
    ask(q)
    setInput('')
  }

  return (
    <>
      {/* ── Floating bubble ─────────────────────────────────────────────── */}
      {!open && (
        <button
          onClick={() => setOpen(true)}
          aria-label="Ask FinSmart AI"
          className="fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full
                     bg-brand-600 text-white shadow-card-lg
                     flex items-center justify-center
                     hover:bg-brand-700 active:scale-95 transition-all"
        >
          <Bot size={24} />
        </button>
      )}

      {/* ── Chat panel ──────────────────────────────────────────────────── */}
      {open && (
        <>
          {/* Backdrop — mobile full-screen, desktop transparent */}
          <div
            className="fixed inset-0 z-40 sm:hidden bg-black/30"
            onClick={() => setOpen(false)}
          />

          <div className="fixed z-50 bg-white flex flex-col
                          shadow-card-lg border border-surface-200
                          inset-x-0 bottom-0 rounded-t-3xl h-[85vh]
                          sm:inset-auto sm:bottom-6 sm:right-6
                          sm:w-96 sm:h-[560px] sm:rounded-3xl
                          animate-fade-in">

            {/* Drag handle — mobile only */}
            <div className="flex justify-center pt-3 pb-1 sm:hidden flex-shrink-0">
              <div className="w-10 h-1 bg-surface-300 rounded-full" />
            </div>

            {/* Header */}
            <div className="flex items-center gap-3 px-5 py-4
                            border-b border-surface-200 flex-shrink-0">
              <div className="w-9 h-9 rounded-xl bg-brand-100
                              flex items-center justify-center flex-shrink-0">
                <Bot size={17} className="text-brand-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-ink-900">Ask FinSmart</p>
                <p className="text-xs text-ink-400">Powered by Gemini · your data only</p>
              </div>
              {messages.length > 0 && (
                <button
                  onClick={clear}
                  className="text-xs text-ink-400 hover:text-ink-700
                             px-2 py-1 rounded-lg hover:bg-surface-100 transition-colors"
                >
                  Clear
                </button>
              )}
              <button
                onClick={() => setOpen(false)}
                className="text-ink-400 hover:text-ink-700 transition-colors flex-shrink-0"
              >
                <X size={18} />
              </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">

              {/* Suggestions — shown only when no messages yet */}
              {messages.length === 0 && (
                <div className="space-y-2">
                  <div className="bg-brand-50 rounded-2xl px-4 py-3">
                    <p className="text-sm text-brand-800 leading-relaxed">
                      Hi! Ask me anything about your spending, budgets, or goals.
                      I only use your real FinSmart data to answer.
                    </p>
                  </div>
                  {SUGGESTIONS.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => handleSend(s)}
                      className="w-full text-left text-sm text-ink-700
                                 bg-surface-50 hover:bg-surface-100
                                 rounded-xl px-4 py-3 transition-colors"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}

              {/* Chat bubbles */}
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                    m.role === 'user'
                      ? 'bg-brand-600 text-white'
                      : m.isError
                        ? 'bg-red-50 text-red-700'
                        : 'bg-surface-100 text-ink-800'
                  }`}>
                    <p>{m.text}</p>
                    {m.contextSummary && (
                      <p className="text-[10px] mt-1.5 opacity-60">{m.contextSummary}</p>
                    )}
                  </div>
                </div>
              ))}

              {/* Thinking indicator */}
              {isLoading && (
                <div className="flex justify-start">
                  <div className="bg-surface-100 rounded-2xl px-4 py-3
                                  flex items-center gap-2">
                    <Loader2 size={13} className="animate-spin text-ink-400" />
                    <span className="text-xs text-ink-400">Thinking…</span>
                  </div>
                </div>
              )}

              <div ref={bottomRef} />
            </div>

            {/* Input row */}
            <div className="flex items-center gap-2 px-4 py-3
                            border-t border-surface-200 flex-shrink-0">
              <input
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && !e.shiftKey && handleSend()}
                placeholder="Ask about your finances…"
                disabled={isLoading}
                className="input flex-1"
              />
              <button
                onClick={() => handleSend()}
                disabled={isLoading || !input.trim()}
                className="w-10 h-10 rounded-xl bg-brand-600 text-white
                           flex items-center justify-center flex-shrink-0
                           hover:bg-brand-700 disabled:opacity-40
                           active:scale-95 transition-all"
              >
                <Send size={15} />
              </button>
            </div>
          </div>
        </>
      )}
    </>
  )
}
