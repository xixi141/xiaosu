import { useCallback, useState } from 'react'
import type { IngestResult } from '../api/types'

export default function UploadZone({ onUploaded }: { onUploaded: (r: IngestResult) => void }) {
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)

  const upload = useCallback(async (file: File) => {
    setUploading(true)
    try {
      const form = new FormData()
      form.append('file', file)
      const res = await fetch('/api/documents', { method: 'POST', body: form })
      const result = (await res.json()) as IngestResult
      onUploaded(result)
    } finally {
      setUploading(false)
    }
  }, [onUploaded])

  return (
    <label
      onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => {
        e.preventDefault()
        setDragging(false)
        const file = e.dataTransfer.files[0]
        if (file) void upload(file)
      }}
      className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-8 text-center transition ${
        dragging ? 'border-blue-500 bg-blue-50' : 'border-slate-300 bg-white hover:border-blue-400'
      }`}
    >
      <input
        type="file"
        accept=".md,.txt,.pdf,.docx"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) void upload(file)
        }}
      />
      <span className="text-sm font-medium text-slate-700">
        {uploading ? '上传处理中…' : '点击或拖拽上传文档（md / txt / pdf / docx）'}
      </span>
      <span className="mt-1 text-xs text-slate-400">同名同内容文件会自动去重；同名不同内容自动替换旧版本</span>
    </label>
  )
}
