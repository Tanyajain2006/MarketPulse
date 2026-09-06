import { request } from './apiClient'

export type WatchlistItem = { ticker: string; companyName?: string; exchange?: string }
export type Watchlist = {
  id: number
  name: string
  items: WatchlistItem[]
  createdAt: string
  updatedAt: string
  reviewedAt?: string
}
export type Checkpoint = { id: number; watchlistId: number; reviewedAt: string }
export type SinceReview = {
  watchlistId: number
  watchlistName: string
  reviewedAt: string | null
  latestObservationTimestamp: string | null
  marketStatus: string
  status: string
  summary: { criticalCount: number; meaningfulCount: number; worthWatchingCount: number; normalCount: number }
  stocks: { ticker: string; previousObservationTimestamp: string | null; currentObservationTimestamp: string | null; status: string; materialityScore: number | null; classification: string | null; warning: string | null; price: { currentPrice: number | null; percentageChange: number | null }; volume: { multiple: number | null; signal: string; status: string }; volatility: { relativeChange: number | null; signal: string }; relativeMovement: { relativeMovement: number | null; interpretation: string }; peerComparison: { status: string }; contributions: { name: string; contribution: number; status: string }[] }[]
}

export type MarketData = {
  ticker: string
  companyName?: string
  exchange?: string
  price: number | null
  volume: number | null
  volatility: number | null
  sectorChange: number | null
  changePercent: number | null
  observationTimestamp: string | null
  ingestionTimestamp: string | null
  source: string
  dataQuality: string
}

export type Instrument = { ticker: string; companyName: string; exchange: string }
export function listWatchlists() { return request<Watchlist[]>('/watchlists') }
export function getWatchlist(id: number) { return request<Watchlist>(`/watchlists/${id}`) }
export function createWatchlist(name: string) { return request<Watchlist>('/watchlists', { method: 'POST', body: { name } }) }
export function renameWatchlist(id: number, name: string) { return request<Watchlist>(`/watchlists/${id}`, { method: 'PUT', body: { name } }) }
export function deleteWatchlist(id: number) { return request<void>(`/watchlists/${id}`, { method: 'DELETE' }) }
export function addTicker(id: number, ticker: string) { return request<Watchlist>(`/watchlists/${id}/items`, { method: 'POST', body: { ticker } }) }
export function removeTicker(id: number, ticker: string) { return request<Watchlist>(`/watchlists/${id}/items/${encodeURIComponent(ticker)}`, { method: 'DELETE' }) }
export function getMarketData(id: number) { return request<MarketData[]>(`/watchlists/${id}/market-data`) }
export function searchInstruments(query: string) { return request<Instrument[]>(`/watchlists/instruments/search?query=${encodeURIComponent(query)}`) }
export function getWatchlistCheckpoint(id: number) { return request<Checkpoint | null>(`/watchlists/${id}/checkpoint`) }
export function markWatchlistReviewed(id: number) { return request<Checkpoint>(`/watchlists/${id}/checkpoint`, { method: 'POST' }) }
export function getSinceLastReview(id: number) { return request<SinceReview>(`/watchlists/${id}/since-last-review`) }
