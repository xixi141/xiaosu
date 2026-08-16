import { useCallback, useEffect, useState } from 'react'
import { apiDelete, apiGet } from '../api/client'
import type { DocumentDto, IngestResult } from '../api/types'
import UploadZone from '../components/UploadZone'
import { Badge, Button, Dialog, Table } from '../components/ui'

const STATUS_TONE = { READY: 'green', PARSING: 'amber', FAILED: 'red' } as const

export default function DocumentsPage() {
  const [docs, setDocs] = useState<DocumentDto[]>([])
  const [detail, setDetail] = useState<DocumentDto | null>(null)
  const [message, setMessage] = useState('')

  const refresh = useCallback(() => {
    apiGet<DocumentDto[]>('/documents').then(setDocs).catch((e: Error) => setMessage(e.message))
  }, [])

  useEffect(refresh, [refresh])

  const handleUploaded = (r: IngestResult) => {
    setMessage(r.duplicate
      ? `「${r.filename}」内容未变化，已跳过重复处理`
      : r.status === 'FAILED'
        ? `「${r.filename}」处理失败：${r.errorMessage ?? '未知错误'}`
        : `「${r.filename}」已入库，${r.chunkCount} 个切片`)
    refresh()
  }

  const handleDelete = async (id: number, filename: string) => {
    if (!window.confirm(`确定删除「${filename}」？删除后不再参与问答。`)) return
    await apiDelete(`/documents/${id}`)
    setMessage(`已删除「${filename}」`)
    refresh()
  }

  const openDetail = (doc: DocumentDto) => {
    apiGet<DocumentDto>(`/documents/${doc.id}`).then(setDetail).catch((e: Error) => setMessage(e.message))
  }

  return (
    <div className="space-y-4">
      <UploadZone onUploaded={handleUploaded} />
      {message && <p className="text-sm text-slate-600">{message}</p>}
      <div className="rounded-lg border border-slate-200 bg-white">
        <Table
          columns={[
            { key: 'filename', title: '文件名', render: (d) => <span className="font-medium">{d.filename}</span> },
            { key: 'type', title: '类型', render: (d) => <span className="text-slate-500">{d.fileType}</span> },
            {
              key: 'status',
              title: '状态',
              render: (d) => <Badge tone={STATUS_TONE[d.status] ?? 'gray'}>{d.status}</Badge>,
            },
            { key: 'chunks', title: '切片数', render: (d) => <span>{d.chunkCount}</span> },
            {
              key: 'createdAt',
              title: '上传时间',
              render: (d) => <span className="text-slate-500">{d.createdAt?.replace('T', ' ').slice(0, 19)}</span>,
            },
            {
              key: 'actions',
              title: '操作',
              render: (d) => (
                <span className="flex gap-2">
                  <Button variant="ghost" onClick={() => openDetail(d)}>切片预览</Button>
                  <Button variant="danger" onClick={() => void handleDelete(d.id, d.filename)}>删除</Button>
                </span>
              ),
            },
          ]}
          rows={docs}
        />
      </div>
      <Dialog open={detail !== null} title={detail ? `「${detail.filename}」切片预览` : ''} onClose={() => setDetail(null)}>
        {detail?.chunks.length ? (
          <ol className="space-y-3">
            {detail.chunks.map((c) => (
              <li key={c.index} className="rounded-md bg-slate-50 p-3 text-sm">
                <span className="mr-2 font-mono text-xs text-slate-400">#{c.index}</span>
                {c.preview}
              </li>
            ))}
          </ol>
        ) : (
          <p className="text-sm text-slate-500">无切片（可能解析失败：{detail?.errorMessage ?? '未知原因'}）</p>
        )}
      </Dialog>
    </div>
  )
}
