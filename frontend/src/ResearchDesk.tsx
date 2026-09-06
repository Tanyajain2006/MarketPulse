import { useEffect, useState, type FormEvent } from 'react'
import { getAvailableTickers, getHistoricalData, type Snapshot } from './marketDataApi'
import { WorkspaceLayout } from './Watchlists'

function formatDate(value: string) { return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) }
function formatNumber(value: number) { return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value) }

export default function ResearchDesk() {
  const [ticker, setTicker] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [tickers, setTickers] = useState<string[]>([])
  const [rows, setRows] = useState<Snapshot[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { getAvailableTickers().then(setTickers).catch(() => setTickers([])) }, [])
  const search = async (event: FormEvent) => { event.preventDefault(); if (!ticker.trim()) { setError('Choose a ticker from the historical dataset.'); return } setLoading(true); setError(''); try { setRows(await getHistoricalData({ ticker: ticker.trim().toUpperCase(), from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined, to: to ? new Date(`${to}T23:59:59`).toISOString() : undefined })) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to retrieve historical observations.') } finally { setLoading(false) } }
  return <WorkspaceLayout><main className="mp-content"><div className="mp-intro"><p className="mp-eyebrow">Tools / historical data</p><h1>Research desk</h1><p>Inspect imported historical observations. Values are timestamped records, not live market data.</p></div><form className="mp-research-form" onSubmit={search}><label>Ticker<select value={ticker} onChange={(event) => setTicker(event.target.value)}><option value="">Select ticker</option>{tickers.map((item) => <option key={item} value={item}>{item}</option>)}</select></label><label>From<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label><label>To<input type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label><button type="submit" disabled={loading}>{loading ? 'Searching...' : 'Search'}</button></form>{error && <div className="mp-alert" role="alert">{error}</div>}<section className="mp-panel"><div className="mp-panel-head"><div><p className="mp-eyebrow">Historical observations</p><h2>{rows.length ? `${rows.length} records` : 'Results'}</h2></div></div>{loading ? <p className="mp-muted">Loading observations...</p> : rows.length ? <div className="mp-table-wrap"><table className="mp-table"><thead><tr><th>Observation</th><th>Price</th><th>Volume</th><th>Volatility</th><th>Sector change</th><th>Ingested</th><th>Quality</th></tr></thead><tbody>{rows.map((row) => <tr key={`${row.ticker}-${row.timestamp}`}><td>{formatDate(row.timestamp)}</td><td>{formatNumber(row.price)}</td><td>{formatNumber(row.volume)}</td><td>{row.volatility.toFixed(3)}</td><td>{row.sectorChange.toFixed(3)}</td><td>{formatDate(row.ingestionTimestamp)}</td><td>{row.dataQuality}</td></tr>)}</tbody></table></div> : <div className="mp-empty"><h2>{ticker ? 'No observations found' : 'Choose a ticker'}</h2><p>Select a database-backed ticker and optional date range to inspect historical records.</p></div>}</section></main></WorkspaceLayout>
}