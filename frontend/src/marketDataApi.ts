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

export type Investigation = {
  ticker: string; watchlistId: number; watchlistName: string; status: string
  checkpoint: { reviewedAt: string; status: string } | null
  checkpointMarketState: { observationTimestamp: string; price: number; volume: number; volatility: number; sectorChange: number; source: string; dataQuality: string } | null
  currentMarketState: { observationTimestamp: string; price: number; volume: number; volatility: number; sectorChange: number; source: string; dataQuality: string } | null
  priceChange: { previousPrice: number; currentPrice: number; absoluteChange: number; percentageChange: number | null; status: string; reason?: string } | null
  volumeAnomaly: { currentVolume: number; historicalBaseline: number | null; multiple: number | null; threshold: number | null; signal: string; status: string; reason?: string } | null
  volatilityChange: { previousVolatility: number; currentVolatility: number; relativeChange: number | null; threshold: number | null; signal: string; status: string; reason?: string } | null
  sectorMovement: { stockMovement: number | null; sectorMovement: number | null; relativeMovement: number | null; status: string; interpretation: string } | null
  peerMovement: { status: string; reason: string } | null
  materiality: { score: number; classification: string; contributions: { name: string; intensity: number; weight: number; contribution: number; status: string }[] } | null
  news: { id: number; ticker: string; headline: string; source: string; publishedAt: string; ingestionTimestamp: string; eventType: string; sentimentScore: number | null }[]
  sentiment: { status: string; value: number | null }; narrativeShift: { status: string; previousNarrative: string | null; currentNarrative: string | null; change: string | null }
  eventTypes: { eventType: string; articleCount: number }[]; dataQuality: { checkpoint: string; current: string; news: string; warnings: string[] }
  relationships: { nodes: { id: string; type: string; label: string; metadata: Record<string, string> }[]; edges: { source: string; target: string; relationshipType: string; label: string | null }[] }
}

export type MarketChanges = {
  ticker: string
  observationTimestamp: string
  price: { previousPrice: number | null; currentPrice: number | null; absoluteChange: number | null; percentageChange: number | null; status: string; reason?: string }
  volume: { currentVolume: number | null; historicalBaseline: number | null; multiple: number | null; threshold: number | null; signal: string; status: string; reason?: string }
  volatility: { previousVolatility: number | null; currentVolatility: number | null; relativeChange: number | null; threshold: number | null; signal: string; status: string; reason?: string }
  relativeMovement: { stockMovement: number | null; sectorMovement: number | null; relativeMovement: number | null; status: string; interpretation: string }
  peerComparison: { status: string; reason: string }
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
export function getInvestigation(ticker: string, watchlistId: number) { return request<Investigation>(`/investigations/${encodeURIComponent(ticker)}?watchlistId=${watchlistId}`) }
export function getMarketChanges(ticker: string) { return request<MarketChanges>(`/market/${encodeURIComponent(ticker)}/changes`) }
export function getLatest(ticker: string) { return request<Snapshot>(`/market/${encodeURIComponent(ticker)}/latest`) }
export function getHistory(ticker: string, from?: string, to?: string) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return request<Snapshot[]>(`/market/${encodeURIComponent(ticker)}/history?${params.toString()}`)
}