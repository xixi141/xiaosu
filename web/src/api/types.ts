export interface HealthDto {
  status: string
  db: string
  vectorStoreCount: number
  chatModel: string
  embeddingModel: string
  dingtalk: Record<string, unknown>
  time: string
}

export interface Citation {
  documentId: string
  filename: string
  chunkIndex: number
  snippet: string
}

export interface UsageInfo {
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

export interface ChatRequest {
  sessionId: string
  userId: string
  question: string
}

export interface ChatResponse {
  answer: string
  citations: Citation[]
  toolCalls: ToolCallInfo[]
  usage: UsageInfo
  status: string
}

export interface ToolCallInfo {
  name: string
  arguments: string
  resultSummary: string
}

export type StreamEvent =
  | { type: 'meta'; citations: Citation[] }
  | { type: 'token'; delta: string }
  | { type: 'tool'; name: string; status: string }
  | { type: 'done'; usage: UsageInfo; status: string; toolCalls?: ToolCallInfo[] }
  | { type: 'error'; message: string }
