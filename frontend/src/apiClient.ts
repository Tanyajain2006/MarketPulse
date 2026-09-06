import { getAuthToken } from './auth'

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

type RequestOptions = { method?: string; body?: object }

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getAuthToken()
  const response = await fetch(`${API_URL}${path}`, {
    method: options.method || 'GET',
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(options.body ? { body: JSON.stringify(options.body) } : {}),
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok) throw new Error(payload?.error || 'The request could not be completed.')
  return payload as T
}

export { API_URL }