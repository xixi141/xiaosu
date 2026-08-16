import { ReactNode } from 'react'

export function Button({ children, onClick, variant = 'primary', disabled = false }: {
  children: ReactNode
  onClick?: () => void
  variant?: 'primary' | 'danger' | 'ghost'
  disabled?: boolean
}) {
  const styles = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    ghost: 'text-slate-600 hover:bg-slate-100',
  }
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      className={`rounded-md px-3 py-1.5 text-sm transition disabled:opacity-50 ${styles[variant]}`}
    >
      {children}
    </button>
  )
}

export function Badge({ tone, children }: { tone: 'green' | 'red' | 'amber' | 'gray'; children: ReactNode }) {
  const styles = {
    green: 'bg-green-100 text-green-700',
    red: 'bg-red-100 text-red-700',
    amber: 'bg-amber-100 text-amber-700',
    gray: 'bg-slate-100 text-slate-600',
  }
  return <span className={`inline-block rounded-full px-2 py-0.5 text-xs ${styles[tone]}`}>{children}</span>
}

export function Table<T extends { id: string | number }>({ columns, rows, onRowClick }: {
  columns: { key: string; title: string; render: (row: T) => ReactNode }[]
  rows: T[]
  onRowClick?: (row: T) => void
}) {
  return (
    <table className="w-full border-collapse text-sm">
      <thead>
        <tr className="border-b border-slate-200 text-left text-slate-500">
          {columns.map((c) => (
            <th key={c.key} className="px-3 py-2 font-medium">{c.title}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr
            key={row.id}
            onClick={() => onRowClick?.(row)}
            className={`border-b border-slate-100 ${onRowClick ? 'cursor-pointer hover:bg-slate-50' : ''}`}
          >
            {columns.map((c) => (
              <td key={c.key} className="px-3 py-2">{c.render(row)}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function Dialog({ open, title, onClose, children }: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30" onClick={onClose}>
      <div
        className="max-h-[80vh] w-full max-w-2xl overflow-auto rounded-lg bg-white p-4 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}
