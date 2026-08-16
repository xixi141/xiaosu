import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '../api/client'
import type { HealthDto } from '../api/types'

interface SettingsDto {
  chatModel: string
  embeddingModel: string
  chatBaseUrl: string
  embeddingBaseUrl: string
  chatApiKeyMasked: string
  embeddingApiKeyMasked: string
  topK: number
  threshold: number
  chunkSize: number
  chunkOverlap: number
  dingtalkEnabled: boolean
}

export default function SettingsPage() {
  const [health, setHealth] = useState<HealthDto | null>(null)
  const [settings, setSettings] = useState<SettingsDto | null>(null)
  const [testResult, setTestResult] = useState('')

  useEffect(() => {
    apiGet<HealthDto>('/health').then(setHealth).catch(() => setHealth(null))
    apiGet<SettingsDto>('/settings').then(setSettings).catch(() => setSettings(null))
  }, [])

  const testConnection = async () => {
    setTestResult('测试中…')
    const r = await apiPost<{ ok: boolean; message: string; latencyMs: number }>('/settings/test-connection', {})
    setTestResult(`${r.ok ? '✅' : '❌'} ${r.message}（${r.latencyMs}ms）`)
  }

  return (
    <div className="space-y-4">
      {settings && (
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-semibold">模型与 RAG 配置</h2>
            <button
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
              onClick={() => void testConnection()}
            >
              测试模型连通性
            </button>
          </div>
          {testResult && <p className="mb-2 text-sm">{testResult}</p>}
          <dl className="grid grid-cols-2 gap-3 text-sm md:grid-cols-3">
            <div><dt className="text-slate-500">Chat 模型</dt><dd>{settings.chatModel}</dd></div>
            <div><dt className="text-slate-500">Chat Base URL</dt><dd className="break-all">{settings.chatBaseUrl}</dd></div>
            <div><dt className="text-slate-500">Chat API Key</dt><dd>{settings.chatApiKeyMasked}</dd></div>
            <div><dt className="text-slate-500">Embedding 模型</dt><dd>{settings.embeddingModel}</dd></div>
            <div><dt className="text-slate-500">Embedding Base URL</dt><dd className="break-all">{settings.embeddingBaseUrl}</dd></div>
            <div><dt className="text-slate-500">Embedding API Key</dt><dd>{settings.embeddingApiKeyMasked}</dd></div>
            <div><dt className="text-slate-500">检索 topK</dt><dd>{settings.topK}</dd></div>
            <div><dt className="text-slate-500">相似度阈值</dt><dd>{settings.threshold}</dd></div>
            <div><dt className="text-slate-500">切块大小/重叠</dt><dd>{settings.chunkSize}/{settings.chunkOverlap}</dd></div>
          </dl>
          <p className="mt-3 text-xs text-slate-400">修改模型/密钥请编辑 .env 后重启服务（动态切换见 Roadmap）。</p>
        </div>
      )}
      {health && (
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-base font-semibold">服务健康状态</h2>
          <dl className="grid grid-cols-2 gap-3 text-sm md:grid-cols-3">
            <div><dt className="text-slate-500">状态</dt><dd>{health.status}</dd></div>
            <div><dt className="text-slate-500">数据库</dt><dd>{health.db}</dd></div>
            <div><dt className="text-slate-500">向量库文档数</dt><dd>{health.vectorStoreCount}</dd></div>
            <div><dt className="text-slate-500">Chat 模型</dt><dd>{health.chatModel}</dd></div>
            <div><dt className="text-slate-500">Embedding 模型</dt><dd>{health.embeddingModel}</dd></div>
            <div>
              <dt className="text-slate-500">钉钉</dt>
              <dd>{health.dingtalk.enabled ? '已启用（长连接）' : '未启用（本地开发模式）'}</dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  )
}
