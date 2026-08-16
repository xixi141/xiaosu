import { useState } from 'react'
import { useSSE } from '../hooks/useSSE'
import ChatBubble, { CitationCard, ToolCallTrace } from '../components/ChatBubble'
import type { StreamEvent } from '../api/types'

const SCENARIOS = [
  { label: '7.1 年假问答', question: '员工每年有几天年假？' },
  { label: '7.1 报销材料', question: '报销发票需要什么材料？' },
  { label: '7.2 员工部门', question: '员工 001 是哪个部门的？' },
  { label: '7.2 订单统计', question: '上周一共多少订单？' },
  { label: '7.2 当前时间', question: '现在几点？' },
  { label: '7.3 多轮指代', question: '他上周来上班几天？' },
  { label: '7.4 拒答', question: '我们公司 CEO 的家庭住址是？' },
]

interface Turn {
  question: string
  answer: string
  citations: Extract<StreamEvent, { type: 'meta' }>['citations']
  toolCalls: NonNullable<Extract<StreamEvent, { type: 'done' }>['toolCalls']>
  usage: Extract<StreamEvent, { type: 'done' }>['usage'] | null
  status: string
}

export default function ChatPage() {
  const [userId, setUserId] = useState('tester')
  const [sessionId, setSessionId] = useState('web-' + Date.now())
  const [input, setInput] = useState('')
  const [turns, setTurns] = useState<Turn[]>([])
  const { events, streaming, send } = useSSE('/api/chat/stream')

  const ask = (question: string) => {
    if (!question.trim() || streaming) return
    setTurns((t) => [
      ...t,
      { question, answer: '', citations: [], toolCalls: [], usage: null, status: 'streaming' },
    ])
    send({ sessionId, userId, question })
  }

  // 把本次 SSE 事件累积到最后一个 turn
  const applyEvents = (turns: Turn[], events: StreamEvent[]): Turn[] => {
    if (!turns.length) return turns
    const rest = turns.slice(0, -1)
    const last = { ...turns[turns.length - 1] }
    let answer = ''
    let meta: StreamEvent | null = null
    let done: StreamEvent | null = null
    for (const ev of events) {
      if (ev.type === 'token') answer += ev.delta
      if (ev.type === 'meta') meta = ev
      if (ev.type === 'done') done = ev
      if (ev.type === 'error') last.status = 'error'
    }
    last.answer = answer || last.answer
    if (meta) last.citations = meta.citations
    if (done && 'toolCalls' in done && done.toolCalls) last.toolCalls = done.toolCalls
    if (done && 'usage' in done) last.usage = done.usage
    if (done && 'status' in done && done.status) last.status = done.status
    return [...rest, last]
  }

  const renderedTurns = applyEvents(turns, events)

  return (
    <div className="flex h-[calc(100vh-8rem)] gap-4">
      <div className="flex w-56 shrink-0 flex-col gap-2">
        <label className="text-xs text-slate-500">
          模拟用户（验证会话隔离）
          <input
            className="mt-1 w-full rounded-md border border-slate-300 px-2 py-1 text-sm"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
          />
        </label>
        <button
          className="rounded-md border border-slate-300 px-2 py-1 text-xs text-slate-600 hover:bg-slate-100"
          onClick={() => setSessionId('web-' + Date.now())}
        >
          新会话（清上下文）
        </button>
        <div className="mt-2">
          <p className="mb-1 text-xs font-medium text-slate-500">场景速测</p>
          {SCENARIOS.map((s) => (
            <button
              key={s.label}
              className="mb-1 block w-full rounded-md bg-slate-100 px-2 py-1 text-left text-xs text-slate-700 hover:bg-slate-200"
              onClick={() => ask(s.question)}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-1 flex-col rounded-lg border border-slate-200 bg-slate-50">
        <div className="flex-1 space-y-3 overflow-auto p-4">
          {renderedTurns.map((t, i) => (
            <div key={i} className="space-y-2">
              <ChatBubble role="user" text={t.question} />
              <div className="space-y-2">
                <ChatBubble role="assistant" text={t.answer || (i === renderedTurns.length - 1 && streaming ? '思考中…' : '')} />
                <CitationCard citations={t.citations} />
                <ToolCallTrace calls={t.toolCalls} />
                {t.status !== 'streaming' && (
                  <p className="text-xs text-slate-400">
                    status: {t.status} · tokens: {t.usage?.totalTokens ?? '-'}
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
        <div className="flex gap-2 border-t border-slate-200 bg-white p-3">
          <input
            className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
            placeholder="问小苏任何问题…（回车发送）"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !streaming) {
                ask(input)
                setInput('')
              }
            }}
          />
          <button
            className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
            disabled={streaming}
            onClick={() => {
              ask(input)
              setInput('')
            }}
          >
            发送
          </button>
        </div>
      </div>
    </div>
  )
}
