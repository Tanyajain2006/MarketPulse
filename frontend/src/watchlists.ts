import { getAuthToken } from './auth'

export type WatchlistItem = { ticker: string }
export type Watchlist = {
  id: number
  name: string
  items: WatchlistItem[]
  createdAt: string
  updatedAt: string
}

type RequestOptions = { method?: string; body?: object }

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getAuthToken()
  const response = await fetch(`${API_URL}${path}`, {
    method: options.method || 'GET',
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(options.body ? { body: JSON.stringify(options.body) } : {}),
  })

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(payload?.error || 'The request could not be completed.')
  }
  return payload as T
}

export function listWatchlists() {
  return request<Watchlist[]>('/watchlists')
}

export function createWatchlist(name: string) {
  return request<Watchlist>('/watchlists', { method: 'POST', body: { name } })
}

export function renameWatchlist(id: number, name: string) {
  return request<Watchlist>(`/watchlists/${id}`, { method: 'PUT', body: { name } })
}

export function deleteWatchlist(id: number) {
  return request<void>(`/watchlists/${id}`, { method: 'DELETE' })
}

export function addTicker(id: number, ticker: string) {
  return request<Watchlist>(`/watchlists/${id}/items`, { method: 'POST', body: { ticker } })
}

export function removeTicker(id: number, ticker: string) {
  return request<Watchlist>(`/watchlists/${id}/items/${encodeURIComponent(ticker)}`, { method: 'DELETE' })
}
