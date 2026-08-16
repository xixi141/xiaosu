import { useCallback, useEffect, useState } from 'react'
import { apiGet } from '../api/client'
import type { LogDto, LogPageDto } from '../api/types'
import LogDetailDrawer from '../components/LogDetailDrawer'
import { Badge, Table } from '../components/ui'

const STATUS_TONE = { SUCCESS: 'green', FALLBACK: 'amber', FAILED: 'red', REFUSED: 'gray' } as const

export default function LogsPage() {
  const [data, setData] = useState<LogPageDto>({ items: [], total: 0 })
  const [userId, setUserId] = useState('')
  const [status, setStatus] = useState('')
  const [detail, setDetail] = useState<LogDto | null>(null)

  const refresh = useCallback(() => {
    const params = new URLSearchParams()
    if (userId) params.set('userId', userId)
    if (status) params.set('status', status)
    apiGet<LogPageDto>(`/logs?${params.toString()}`)
      .then(setData)
      .catch(() => setData({ items: [], total: 0 }))
  }, [userId, status])

  useEffect(refresh, [refresh])

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <input
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          placeholder="按用户 ID 过滤"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
        <select
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">全部状态</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FALLBACK">FALLBACK</option>
          <option value="FAILED">FAILED</option>
          <option value="REFUSED">REFUSED</option>
        </select>
        <span className="self-center text-xs text-slate-400">共 {data.total} 条</span>
      </div>
      <div className="rounded-lg border border-slate-200 bg-white">
        <Table
          columns={[
            { key: 'user', title: '用户', render: (l) => <span>{l.userId}</span> },
            {
              key: 'question',
              title: '问题',
              render: (l) => <span className="block max-w-md truncate">{l.question}</span>,
            },
            {
              key: 'status',
              title: '状态',
              render: (l) => <Badge tone={STATUS_TONE[l.status] ?? 'gray'}>{l.status}</Badge>,
            },
            { key: 'tokens', title: 'Tokens', render: (l) => <span>{l.totalTokens}</span> },
            { key: 'latency', title: '耗时', render: (l) => <span className="text-slate-500">{l.latencyMs}ms</span> },
            {
              key: 'time',
              title: '时间',
              render: (l) => <span className="text-slate-500">{l.createdAt?.replace('T', ' ').slice(0, 19)}</span>,
            },
          ]}
          rows={data.items}
          onRowClick={(l) => setDetail(l)}
        />
      </div>
      <LogDetailDrawer log={detail} onClose={() => setDetail(null)} />
    </div>
  )
}
