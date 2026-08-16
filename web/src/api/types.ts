export interface HealthDto {
  status: string
  db: string
  vectorStoreCount: number
  chatModel: string
  embeddingModel: string
  dingtalk: Record<string, unknown>
  time: string
}

export interface DocumentDto {
  id: number
  filename: string
  fileType: string
  fileSize: number
  status: 'PARSING' | 'READY' | 'FAILED'
  chunkCount: number
  errorMessage: string | null
  createdAt: string
  chunks: ChunkPreview[]
}

export interface ChunkPreview {
  index: number
  preview: string
  charCount: number
}

export interface IngestResult {
  documentId: number | null
  filename: string
  sha256: string
  status: string
  chunkCount: number
  duplicate: boolean
  errorMessage: string | null
}

export interface LogDto {
  id: number
  sessionId: string
  userId: string
  question: string
  answer: string
  model: string
  totalTokens: number
  latencyMs: number
  status: 'SUCCESS' | 'FALLBACK' | 'FAILED' | 'REFUSED'
  isRefused: boolean
  errorMessage: string | null
  toolCalls: ToolCallInfo[]
  citations: Citation[]
  createdAt: string
}

export interface LogPageDto {
  items: LogDto[]
  total: number
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
