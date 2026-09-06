import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './auth'
import { addTicker, createWatchlist, deleteWatchlist, getMarketData, getWatchlistCheckpoint, listWatchlists, markWatchlistReviewed, removeTicker, renameWatchlist, type Checkpoint, type MarketData, type Watchlist } from './watchlistApi'

function Icon({ name }: { name: 'bell' | 'chart' | 'close' | 'edit' | 'eye' | 'key' | 'plus' | 'trash' }) {
  const paths = {
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    chart: <><path d="M5 19V9M12 19V5M19 19v-7" /><path d="M3 19h18" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
    edit: <><path d="m4 20 4.2-1 9.9-9.9a2.1 2.1 0 0 0-3-3L5.2 16Z" /><path d="m13.8 7.2 3 3" /></>,
    eye: <><path d="M2.5 12s3.4-6 9.5-6 9.5 6 9.5 6-3.4 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.4" /></>,
    key: <><circle cx="8" cy="15" r="3" /><path d="m10.2 12.8 7-7M15 8l2 2M17 6l1 1" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    trash: <><path d="M4 7h16M10 11v5M14 11v5M6 7l1 13h10l1-13M9 7V4h6v3" /></>,
  }
  return <svg aria-hidden="true" className="mp-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function Logo() { return <div className="mp-logo"><span className="mp-logo-mark">M<span /></span><span>Market<strong>Pulse</strong></span></div> }

function Sidebar({ user }: { user: { name: string; email: string } }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const active = (path: string) => location.pathname === path ? 'mp-nav-item is-active' : 'mp-nav-item'
  const signOut = () => { logout(); navigate('/login') }
  return <aside className="mp-sidebar"><Logo /><nav className="mp-nav" aria-label="Workspace navigation"><p className="mp-nav-label">Workspace</p><Link to="/dashboard" className={active('/dashboard')}><span>Overview</span>{location.pathname === '/dashboard' && <i />}</Link><Link to="/watchlists" className={active('/watchlists')}><span><Icon name="eye" />Watchlists</span>{location.pathname === '/watchlists' && <i />}</Link><Link to="/preferences" className={active('/preferences')}><span>Preferences</span>{location.pathname === '/preferences' && <i />}</Link><div className="mp-nav-divider" /><p className="mp-nav-label">Tools</p><Link to="/research" className={active('/research')}><span>Research desk</span>{location.pathname === '/research' && <i />}</Link></nav><div className="mp-profile"><div className="mp-avatar">{user.name.trim().charAt(0).toUpperCase()}</div><div className="mp-profile-copy"><strong>{user.name}</strong><span>{user.email}</span></div><button type="button" className="mp-signout" title="Sign out" onClick={signOut}>↗</button></div></aside>
}

function Header() { return <header className="mp-header"><span>Market intelligence workspace</span><div className="mp-header-actions"><button type="button" className="mp-quick-find"><Icon name="key" />Quick find <kbd>K</kbd></button><button type="button" className="mp-header-icon" aria-label="Notifications"><Icon name="bell" /></button></div></header> }
function InlineMessage({ children }: { children: ReactNode }) { return <div className="mp-alert" role="alert">{children}</div> }

function WatchlistCard({ watchlist, onUpdate, onDelete, onError }: { watchlist: Watchlist; onUpdate: (watchlist: Watchlist) => void; onDelete: (id: number) => void; onError: (message: string) => void }) {
  const [ticker, setTicker] = useState('')
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(watchlist.name)
  const [busy, setBusy] = useState<'rename' | 'delete' | 'ticker' | 'remove' | 'review' | null>(null)
  const [showTicker, setShowTicker] = useState(false)
  const [marketData, setMarketData] = useState<MarketData[]>([])
  const [marketLoading, setMarketLoading] = useState(false)
  const [marketError, setMarketError] = useState('')
  const [checkpoint, setCheckpoint] = useState<Checkpoint | null>(null)
  const [checkpointLoading, setCheckpointLoading] = useState(true)
  const [checkpointMessage, setCheckpointMessage] = useState('')
  useEffect(() => { setMarketLoading(true); getMarketData(watchlist.id).then(setMarketData).catch((reason) => setMarketError(reason instanceof Error ? reason.message : 'Unable to load market data.')).finally(() => setMarketLoading(false)) }, [watchlist.id, watchlist.items])
  useEffect(() => { setCheckpointLoading(true); getWatchlistCheckpoint(watchlist.id).then(setCheckpoint).catch((reason) => onError(reason instanceof Error ? reason.message : 'Unable to load review status.')).finally(() => setCheckpointLoading(false)) }, [watchlist.id])
  const run = async (action: () => Promise<Watchlist>, kind: NonNullable<typeof busy>) => { setBusy(kind); try { onUpdate(await action()) } catch (reason) { onError(reason instanceof Error ? reason.message : 'The request could not be completed.') } finally { setBusy(null) } }
  const submitRename = async (event: FormEvent) => { event.preventDefault(); const clean = name.trim(); if (!clean || clean === watchlist.name) { setEditing(false); return } await run(() => renameWatchlist(watchlist.id, clean), 'rename'); setEditing(false) }
  const submitTicker = async (event: FormEvent) => { event.preventDefault(); const clean = ticker.trim().toUpperCase(); if (!clean || watchlist.items.some((item) => item.ticker === clean)) { onError('Enter a new ticker symbol.'); return } await run(() => addTicker(watchlist.id, clean), 'ticker'); setTicker(''); setShowTicker(false) }
  const remove = (symbol: string) => run(() => removeTicker(watchlist.id, symbol), 'remove')
  const review = async () => { setBusy('review'); setCheckpointMessage(''); try { setCheckpoint(await markWatchlistReviewed(watchlist.id)); setCheckpointMessage('Review saved.') } catch (reason) { onError(reason instanceof Error ? reason.message : 'Unable to mark this watchlist as reviewed.') } finally { setBusy(null) } }
  const removeList = async () => { if (!window.confirm(`Delete ${watchlist.name}?`)) return; setBusy('delete'); try { await deleteWatchlist(watchlist.id); onDelete(watchlist.id) } catch (reason) { onError(reason instanceof Error ? reason.message : 'Unable to delete this watchlist.') } finally { setBusy(null) } }
  return <article className="mp-card"><div className="mp-card-head"><span className="mp-card-icon"><Icon name="chart" /></span><div className="mp-card-title">{editing ? <form onSubmit={submitRename} className="mp-inline-form"><input value={name} maxLength={100} onChange={(event) => setName(event.target.value)} autoFocus /><button type="submit" disabled={busy === 'rename'}>Save</button></form> : <><h2>{watchlist.name}</h2><p>{watchlist.items.length} {watchlist.items.length === 1 ? 'symbol' : 'symbols'} · checked {new Date(watchlist.updatedAt).toLocaleDateString()}</p></>}</div><div className="mp-card-actions"><button type="button" onClick={() => setEditing(true)} aria-label={`Rename ${watchlist.name}`} disabled={Boolean(busy)}><Icon name="edit" /></button><button type="button" onClick={removeList} aria-label={`Delete ${watchlist.name}`} disabled={Boolean(busy)}><Icon name="trash" /></button></div></div><div className="mp-tickers">{watchlist.items.length ? watchlist.items.map((item) => <span className="mp-ticker" key={item.ticker}>{item.ticker}<button type="button" onClick={() => remove(item.ticker)} disabled={Boolean(busy)} aria-label={`Remove ${item.ticker}`}><Icon name="close" /></button></span>) : <span className="mp-card-empty">No symbols yet</span>}</div><section className="mp-checkpoint"><div><p className="mp-eyebrow">Last reviewed</p><strong>{checkpointLoading ? 'Loading...' : checkpoint ? new Date(checkpoint.reviewedAt).toLocaleString() : 'Not reviewed yet'}</strong></div><button type="button" className="mp-review-button" onClick={review} disabled={Boolean(busy) || checkpointLoading}>{busy === 'review' ? 'Saving...' : 'Mark as Reviewed'}</button>{checkpointMessage && <span className="mp-positive">{checkpointMessage}</span>}</section><section className="mp-market-list"><p className="mp-eyebrow">Historical market data</p>{marketLoading ? <p className="mp-muted">Loading observations...</p> : marketError ? <p className="mp-negative">{marketError}</p> : marketData.length ? marketData.map((data) => <div className="mp-market-row" key={data.ticker}><strong>{data.ticker}</strong>{data.price === null ? <span className="mp-muted">No historical observation</span> : <span>Price {data.price.toFixed(2)} · Volume {data.volume?.toLocaleString()} · Volatility {data.volatility?.toFixed(3)} · Sector {data.sectorChange?.toFixed(3)}<small>Last observation: {data.observationTimestamp ? new Date(data.observationTimestamp).toLocaleString() : 'Unavailable'}</small></span>}</div>) : <p className="mp-muted">No historical observations for these tickers.</p>}</section><div className="mp-card-footer">{showTicker ? <form className="mp-add-form" onSubmit={submitTicker}><input value={ticker} maxLength={15} onChange={(event) => setTicker(event.target.value)} placeholder="Ticker symbol" autoFocus /><button type="submit" disabled={busy === 'ticker'}>Add</button><button type="button" onClick={() => setShowTicker(false)} aria-label="Cancel"><Icon name="close" /></button></form> : <button type="button" className="mp-add-trigger" onClick={() => setShowTicker(true)} disabled={Boolean(busy)}><Icon name="plus" />Add ticker</button>}</div></article>
}

export function WorkspaceLayout({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return <div className="mp-app"><Sidebar user={user!} /><div className="mp-main"><Header />{children}</div></div>
}

export default function Watchlists() {
  const { user } = useAuth()
  const [watchlists, setWatchlists] = useState<Watchlist[]>([])
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const load = async () => { setLoading(true); setError(''); try { setWatchlists(await listWatchlists()) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load your watchlists.') } finally { setLoading(false) } }
  useEffect(() => { void load() }, [])
  const create = async (event: FormEvent) => { event.preventDefault(); const clean = name.trim(); if (!clean) { setError('Give your watchlist a name.'); return } setCreating(true); setError(''); try { const created = await createWatchlist(clean); setWatchlists((current) => [created, ...current]); setName('') } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to create your watchlist.') } finally { setCreating(false) } }
  const update = (next: Watchlist) => setWatchlists((current) => current.map((item) => item.id === next.id ? next : item))
  return <WorkspaceLayout><main className="mp-content"><div className="mp-intro"><p className="mp-eyebrow">Your universe</p><h1>Watchlists</h1><p>Keep each investment thesis close. MarketPulse will measure what changed since your last review.</p></div><form className="mp-create" onSubmit={create}><input value={name} maxLength={100} onChange={(event) => setName(event.target.value)} placeholder="Name a new watchlist" aria-label="Name a new watchlist" /><button type="submit" disabled={creating || !name.trim()}><Icon name="plus" />{creating ? 'Creating...' : 'Create'}</button></form>{error && <InlineMessage>{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error"><Icon name="close" /></button></InlineMessage>}{loading ? <div className="mp-grid" aria-label="Loading watchlists">{[1, 2].map((item) => <div className="mp-skeleton" key={item} />)}</div> : watchlists.length ? <div className="mp-grid">{watchlists.map((watchlist) => <WatchlistCard key={watchlist.id} watchlist={watchlist} onUpdate={update} onDelete={(id) => setWatchlists((current) => current.filter((item) => item.id !== id))} onError={setError} />)}</div> : <section className="mp-empty"><span className="mp-card-icon"><Icon name="chart" /></span><h2>Your universe is empty</h2><p>Create a watchlist above to keep an investment thesis close.</p></section>}</main></WorkspaceLayout>
}