import { request } from './apiClient'
import type { Snapshot } from './marketDataApi'

export type ResearchResponse = { instruments: { ticker: string; companyName: string; exchange: string }[]; market: Snapshot[]; news: unknown[] }
export function searchResearch(query: string) { return request<ResearchResponse>(`/research/search?q=${encodeURIComponent(query)}`) }