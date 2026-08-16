import type { Citation, ToolCallInfo } from '../api/types'

export function CitationCard({ citations }: { citations: Citation[] }) {
  if (!citations.length) return null
  return (
    <div className="mt-2 rounded-md bg-blue-50 p-2 text-xs">
      <p className="mb-1 font-medium text-blue-800">📚 来源</p>
      <ul className="space-y-1">
        {citations.map((c, i) => (
          <li key={i} className="text-blue-700">
            [{i + 1}] {c.filename}（切片 #{c.chunkIndex}）
            <p className="text-slate-500">{c.snippet}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function ToolCallTrace({ calls }: { calls: ToolCallInfo[] }) {
  if (!calls.length) return null
  return (
    <details className="mt-2 rounded-md bg-amber-50 p-2 text-xs">
      <summary className="cursor-pointer font-medium text-amber-800">
        🔧 工具调用 × {calls.length}
      </summary>
      {calls.map((c, i) => (
        <div key={i} className="mt-1 text-amber-700">
          <p><b>{c.name}</b>({c.arguments})</p>
          <p className="text-slate-500">{c.resultSummary}</p>
        </div>
      ))}
    </details>
  )
}

export default function ChatBubble({ role, text }: { role: 'user' | 'assistant'; text: string }) {
  return (
    <div className={`flex ${role === 'user' ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[75%] whitespace-pre-wrap rounded-lg px-3 py-2 text-sm ${
          role === 'user' ? 'bg-blue-600 text-white' : 'bg-white text-slate-800 border border-slate-200'
        }`}
      >
        {text}
      </div>
    </div>
  )
}
