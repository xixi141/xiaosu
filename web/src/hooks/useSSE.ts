import { useCallback, useRef, useState } from 'react'
import type { StreamEvent } from '../api/types'

export interface SSEState {
  events: StreamEvent[]
  streaming: boolean
}

/** POST + SSE 流式读取：用 fetch ReadableStream 解析（EventSource 不支持 POST） */
export function useSSE(url: string) {
  const [state, setState] = useState<SSEState>({ events: [], streaming: false })
  const abortRef = useRef<AbortController | null>(null)

  const send = useCallback((body: unknown) => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setState({ events: [], streaming: true })

    void (async () => {
      try {
        const res = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
          signal: controller.signal,
        })
        if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`)
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        for (;;) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const parts = buffer.split('\n\n')
          buffer = parts.pop() ?? ''
          for (const part of parts) {
            const dataLine = part.split('\n').find((l) => l.startsWith('data:'))
            if (!dataLine) continue
            const json = dataLine.slice(5).trim()
            if (!json) continue
            const ev = JSON.parse(json) as StreamEvent
            setState((s) => ({ ...s, events: [...s.events, ev] }))
          }
        }
      } catch (e) {
        if ((e as Error).name !== 'AbortError') {
          setState((s) => ({
            ...s,
            events: [...s.events, { type: 'error', message: (e as Error).message } as StreamEvent],
          }))
        }
      } finally {
        setState((s) => ({ ...s, streaming: false }))
      }
    })()
  }, [url])

  return { ...state, send }
}
