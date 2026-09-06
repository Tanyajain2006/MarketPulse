import { request } from './apiClient'

export type Snapshot = {
  timestamp: string
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