import { useEffect, useState } from 'react'
import { apiGet } from '../api/client'
import type { HealthDto } from '../api/types'

export default function SettingsPage() {
  const [health, setHealth] = useState<HealthDto | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    apiGet<HealthDto>('/health')
      .then(setHealth)
      .catch((e: Error) => setError(e.message))
  }, [])

  if (error) return <div className="text-red-600">健康检查失败：{error}</div>
  if (!health) return <div className="text-slate-500">加载中…</div>

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="mb-3 text-base font-semibold">服务健康状态</h2>
      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div><dt className="text-slate-500">状态</dt><dd>{health.status}</dd></div>
        <div><dt className="text-slate-500">数据库</dt><dd>{health.db}</dd></div>
        <div><dt className="text-slate-500">向量库文档数</dt><dd>{health.vectorStoreCount}</dd></div>
        <div><dt className="text-slate-500">Chat 模型</dt><dd>{health.chatModel}</dd></div>
        <div><dt className="text-slate-500">Embedding 模型</dt><dd>{health.embeddingModel}</dd></div>
        <div><dt className="text-slate-500">钉钉</dt><dd>{String(health.dingtalk.enabled ?? false)}</dd></div>
      </dl>
    </div>
  )
}
