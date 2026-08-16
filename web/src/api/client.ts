const BASE = '/api'

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    throw new ApiError(res.status, `HTTP ${res.status}: ${await res.text()}`)
  }
  return res.json() as Promise<T>
}

export const apiGet = <T,>(path: string) => apiFetch<T>(path)
export const apiPost = <T,>(path: string, body: unknown) =>
  apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body) })
export const apiDelete = <T,>(path: string) => apiFetch<T>(path, { method: 'DELETE' })
