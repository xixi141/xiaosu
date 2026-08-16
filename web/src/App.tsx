import { useState } from 'react'
import DocumentsPage from './pages/DocumentsPage'
import SettingsPage from './pages/SettingsPage'

type Tab = 'chat' | 'documents' | 'logs' | 'settings'

const TABS: { key: Tab; label: string }[] = [
  { key: 'chat', label: '调试聊天' },
  { key: 'documents', label: '文档管理' },
  { key: 'logs', label: '对话日志' },
  { key: 'settings', label: '设置' },
]

export default function App() {
  const [tab, setTab] = useState<Tab>('chat')

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3">
          <h1 className="text-lg font-bold">小苏 · 管理后台</h1>
          <nav className="flex gap-1">
            {TABS.map((t) => (
              <button
                key={t.key}
                onClick={() => setTab(t.key)}
                className={`rounded-md px-3 py-1.5 text-sm ${
                  tab === t.key
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                {t.label}
              </button>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        {tab === 'chat' && <div className="text-slate-500">调试聊天页（Task 14 后接入）</div>}
        {tab === 'documents' && <DocumentsPage />}
        {tab === 'logs' && <div className="text-slate-500">对话日志页（Task 15 后接入）</div>}
        {tab === 'settings' && <SettingsPage />}
      </main>
    </div>
  )
}
