import { request } from './apiClient'

export type Snapshot = {
  observationTimestamp: string
  ticker: string
  price: number
  volume: number
  volatility: number
  sectorChange: number
  source: string
  ingestionTimestamp: string
  dataQuality: string
}

export function getHistoricalData(filters: { ticker?: string; from?: string; to?: string; limit?: number }) {
  const params = new URLSearchParams()
  if (filters.ticker) params.set('ticker', filters.ticker)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  params.set('limit', String(filters.limit || 100))
  return request<Snapshot[]>(`/market-data?${params.toString()}`)
}

export function getAvailableTickers() { return request<string[]>('/market-data/tickers') }
export function getLatest(ticker: string) { return request<Snapshot>(`/market/${encodeURIComponent(ticker)}/latest`) }
export function getHistory(ticker: string, from?: string, to?: string) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return request<Snapshot[]>(`/market/${encodeURIComponent(ticker)}/history?${params.toString()}`)
}