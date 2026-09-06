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
export function markWatchlistReviewed(id: number) { return request<Watchlist>(`/watchlists/${id}/checkpoint`, { method: 'POST' }) }
