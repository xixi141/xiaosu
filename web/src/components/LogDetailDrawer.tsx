import type { LogDto } from '../api/types'
import { CitationCard, ToolCallTrace } from './ChatBubble'
import { Dialog } from './ui'

export default function LogDetailDrawer({ log, onClose }: { log: LogDto | null; onClose: () => void }) {
  return (
    <Dialog open={log !== null} title={log ? `对话详情 #${log.id}` : ''} onClose={onClose}>
      {log && (
        <div className="space-y-3 text-sm">
          <p><b>用户：</b>{log.userId} <span className="text-slate-400">（会话 {log.sessionId}）</span></p>
          <div className="rounded-md bg-slate-50 p-3"><b>问题：</b>{log.question}</div>
          <div className="rounded-md bg-slate-50 p-3"><b>回答：</b>{log.answer}</div>
          <CitationCard citations={log.citations} />
          <ToolCallTrace calls={log.toolCalls} />
          <p className="text-xs text-slate-400">
            模型 {log.model} · {log.totalTokens} tokens · {log.latencyMs}ms · {log.createdAt?.replace('T', ' ').slice(0, 19)}
            {log.errorMessage && <span className="text-red-500"> · 错误：{log.errorMessage}</span>}
          </p>
        </div>
      )}
    </Dialog>
  )
}
