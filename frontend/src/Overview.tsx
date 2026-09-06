import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSinceLastReview, listWatchlists, markWatchlistReviewed, type SinceReview, type Watchlist } from './watchlistApi'
import { WorkspaceLayout } from './Watchlists'

function formatDate(value: string | null) { return value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : 'Not available' }
function number(value: number | null) { return value === null ? 'Unavailable' : new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value) }
function label(value: string | null) { return value === 'WORTH_WATCHING' ? 'WORTH WATCHING' : value || 'UNAVAILABLE' }

function StockCard({ stock, watchlistId, compact = false }: { stock: SinceReview['stocks'][number]; watchlistId: number; compact?: boolean }) {
  const navigate = useNavigate()
  return (
    <article className={compact ? 'mp-stock-card mp-stock-card-compact' : 'mp-stock-card'}>
      <div className="mp-stock-card-head"><div><h3>{stock.ticker}</h3><p>{stock.currentObservationTimestamp ? `Observed ${formatDate(stock.currentObservationTimestamp)}` : stock.warning || 'No observation available'}</p></div>{stock.classification && <span className={`mp-dashboard-classification ${stock.classification}`}>{label(stock.classification)}</span>}</div>
      {stock.materialityScore === null ? <p className="mp-muted">{stock.warning || 'No comparable historical market data.'}</p> : <>
        <div className="mp-score"><strong>{stock.materialityScore.toFixed(0)}</strong><span>/ 100 materiality</span></div>
        <div className="mp-stock-metrics"><span>Current price<strong>{number(stock.price.currentPrice)}</strong></span><span>Change<strong>{stock.price.percentageChange === null ? 'Unavailable' : `${number(stock.price.percentageChange)}%`}</strong></span><span>Volume<strong>{stock.volume.multiple === null ? stock.volume.status : `${number(stock.volume.multiple)}× baseline`}</strong><small>{stock.volume.signal}</small></span><span>Volatility<strong>{stock.volatility.relativeChange === null ? 'Unavailable' : `${number(stock.volatility.relativeChange)}%`}</strong><small>{stock.volatility.signal}</small></span><span>Sector divergence<strong>{stock.relativeMovement.relativeMovement === null ? 'Unavailable' : `${number(stock.relativeMovement.relativeMovement)}%`}</strong><small>{stock.relativeMovement.interpretation}</small></span></div>
        <p className="mp-contributions">{stock.contributions.filter((item) => item.contribution > 0).map((item) => `${item.name}: ${item.contribution.toFixed(1)}`).join(' · ') || 'No material contribution'}</p>
        {stock.warning && <p className="mp-warning">{stock.warning}</p>}
        <div className="mp-stock-actions"><button type="button" onClick={() => navigate(`/investigations/${encodeURIComponent(stock.ticker)}?watchlistId=${watchlistId}`)}>Investigate</button><button type="button" disabled>Acknowledge</button></div>
      </>}
    </article>
  )
}

export default function OverviewPage() {
  const [watchlists, setWatchlists] = useState<Watchlist[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [result, setResult] = useState<SinceReview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const loadWatchlists = async () => { const next = await listWatchlists(); setWatchlists(next); setSelectedId((current) => current && next.some((item) => item.id === current) ? current : next[0]?.id || null); return next }
  const loadResult = async (id: number) => { setLoading(true); setError(''); try { setResult(await getSinceLastReview(id)) } catch (reason) { setResult(null); setError(reason instanceof Error ? reason.message : 'Unable to load dashboard intelligence.') } finally { setLoading(false) } }
  useEffect(() => { loadWatchlists().then((next) => next[0] && loadResult(next[0].id)).catch((reason) => { setError(reason instanceof Error ? reason.message : 'Unable to load watchlists.'); setLoading(false) }) }, [])
  useEffect(() => { if (selectedId && result?.watchlistId !== selectedId) void loadResult(selectedId) }, [selectedId])
  const refresh = () => { if (selectedId) void loadResult(selectedId) }
  const review = async () => { if (!selectedId) return; setActionMessage(''); try { const checkpoint = await markWatchlistReviewed(selectedId); setActionMessage(`Review saved at ${formatDate(checkpoint.reviewedAt)}.`); await loadResult(selectedId) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to mark watchlist as reviewed.') } }
  const stocks = result?.stocks || []
  const attention = stocks.filter((stock) => stock.classification === 'CRITICAL' || stock.classification === 'MEANINGFUL')
  const watching = stocks.filter((stock) => stock.classification === 'WORTH_WATCHING')
  const normal = stocks.filter((stock) => stock.classification === 'NORMAL')

  return <WorkspaceLayout><main className="mp-content">
    <div className="mp-dashboard-toolbar"><div><p className="mp-eyebrow">Since you last checked</p><h1>Overview</h1></div><label className="mp-watchlist-select">Watchlist<select value={selectedId || ''} onChange={(event) => setSelectedId(Number(event.target.value))}>{watchlists.map((watchlist) => <option key={watchlist.id} value={watchlist.id}>{watchlist.name}</option>)}</select></label></div>
    {error && <div className="mp-alert" role="alert">{error}<button type="button" onClick={refresh}>Retry</button></div>}
    {loading && <div className="mp-panel" aria-live="polite">Loading intelligence...</div>}
    {!loading && result && <>
      <section className="mp-dashboard-hero"><div><p className="mp-eyebrow">{result.watchlistName}</p><h2>{result.status === 'NO_CHECKPOINT' ? 'Establish your first review point.' : 'Here is what changed since your last review.'}</h2><p className="mp-muted">Last reviewed: {formatDate(result.reviewedAt)} · Latest market data: {formatDate(result.latestObservationTimestamp)}</p><p className="mp-muted">Market status: <strong>{result.marketStatus.replaceAll('_', ' ')}</strong></p></div><div className="mp-dashboard-actions"><button type="button" onClick={refresh}>Refresh</button><button type="button" onClick={review}>Mark as Reviewed</button></div></section>
      {actionMessage && <p className="mp-positive" role="status">{actionMessage}</p>}
      {result.status === 'NO_CHECKPOINT' ? <section className="mp-panel mp-first-review"><h2>No review checkpoint yet</h2><p>Mark this watchlist as reviewed to establish your first comparison point.</p><button type="button" onClick={review}>Mark as Reviewed</button></section> : result.marketStatus === 'NO_MARKET_DATA' ? <section className="mp-panel"><h2>No market data available</h2><p>No historical observations are available for this watchlist.</p></section> : <>
        <section className="mp-summary"><div className="CRITICAL"><span>Critical</span><strong>{result.summary.criticalCount}</strong></div><div className="MEANINGFUL"><span>Meaningful</span><strong>{result.summary.meaningfulCount}</strong></div><div className="WORTH_WATCHING"><span>Worth watching</span><strong>{result.summary.worthWatchingCount}</strong></div><div className="NORMAL"><span>Normal</span><strong>{result.summary.normalCount}</strong></div></section>
        {result.marketStatus === 'STALE_DATA' && <p className="mp-warning">STALE DATA · Latest historical observations may not represent current market conditions.</p>}
        <section className="mp-dashboard-section"><div className="mp-section-heading"><p className="mp-eyebrow">Priority queue</p><h2>Needs attention</h2></div>{attention.length ? attention.map((stock) => <StockCard key={stock.ticker} stock={stock} watchlistId={result.watchlistId} />) : <p className="mp-muted">No critical or meaningful changes since the last review.</p>}</section>
        <section className="mp-dashboard-section"><div className="mp-section-heading"><p className="mp-eyebrow">Monitor</p><h2>Worth watching</h2></div>{watching.length ? watching.map((stock) => <StockCard key={stock.ticker} stock={stock} watchlistId={result.watchlistId} compact />) : <p className="mp-muted">No stocks are currently worth watching.</p>}</section>
        <section className="mp-dashboard-section"><div className="mp-section-heading"><p className="mp-eyebrow">Quiet signals</p><h2>Normal</h2></div>{normal.length ? normal.map((stock) => <StockCard key={stock.ticker} stock={stock} watchlistId={result.watchlistId} compact />) : <p className="mp-muted">No normal observations in this review.</p>}</section>
      </>}
    </>}
  </main></WorkspaceLayout>
}
