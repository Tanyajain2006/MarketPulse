import { request } from './apiClient'

export type Change = {
  ticker: string
  companyName?: string
  exchange?: string
  observedAt: string
  price: number
  priceChangePercent: number
  volume: number
  volumeMultiple: number
  volatility: number
  sectorChange: number
  classification: string
  materiality: number
  headline?: string
  eventType?: string
  sentimentScore?: number
}
export type Overview = { watchlistName?: string; tickerCount: number; checkpoint: string; latestObservation?: string; summary: { needsAttention: number; worthWatching: number; noMaterialChange: number; unacknowledged: number }; changes: Change[] }
export type Preference = { userId: number; displayName: string; email: string; readingDensity: 'COMPACT' | 'COMFORTABLE' | 'EXPANDED'; updatedAt: string }
export type Health = { status: string; service: string; database: string }

export function getOverview() { return request<Overview>('/dashboard/overview') }
export function updateCheckpoint() { return request<string>('/dashboard/checkpoint', { method: 'POST' }) }
export function getPreferences() { return request<Preference>('/preferences') }
export function updatePreferences(input: { displayName?: string; readingDensity?: Preference['readingDensity'] }) { return request<Preference>('/preferences', { method: 'PATCH', body: input }) }
export function getHealth() { return request<Health>('/health') }
export function getCurrentUser() { return request<{ id: number; name: string; email: string }>('/auth/me') }
