import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useAuth } from './auth'
import {
  addTicker,
  createWatchlist,
  deleteWatchlist,
  getMarketData,
  getWatchlist,
  listWatchlists,
  markWatchlistReviewed,
  removeTicker,
  renameWatchlist,
  searchInstruments,
  type Instrument,
  type MarketData,
  type Watchlist,
} from './watchlistApi'

type ModalProps = { children: ReactNode; title: string; onClose: () => void }

function Modal({ children, title, onClose }: ModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#071a2b]/50 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-label={title}>
      <div className="w-full max-w-md rounded-xl border border-[#dbe4ec] bg-white p-6 shadow-[0_24px_80px_rgba(9,34,59,0.2)]">
        <div className="flex items-center justify-between"><h2 className="font-display text-lg font-bold text-[#102d4e]">{title}</h2><button type="button" onClick={onClose} aria-label="Close dialog" className="text-xl leading-none text-[#8291a4] hover:text-[#173554]">×</button></div>
        {children}
      </div>
    </div>
  )
}

function Sidebar({ user, selected, onSelect }: { user: { name: string; email: string }; selected: Watchlist | null; onSelect: (id: number) => void }) {
  const [watchlists, setWatchlists] = useState<Watchlist[]>([])
  const [loading, setLoading] = useState(true)
  useEffect(() => { listWatchlists().then(setWatchlists).finally(() => setLoading(false)) }, [selected])
  return (
    <aside className="hidden w-64 shrink-0 flex-col bg-[#071a2b] px-4 py-5 text-white lg:flex">
      <div className="px-3 pb-8"><p className="font-display text-xl font-bold tracking-[-0.04em]">Market<span className="text-[#69cfda]">Pulse</span></p><p className="mt-1 text-[0.62rem] font-semibold uppercase tracking-[0.18em] text-[#7895aa]">Intelligence workspace</p></div>
      <nav className="space-y-1 text-sm"><p className="px-3 pb-2 text-[0.63rem] font-bold uppercase tracking-[0.15em] text-[#66869e]">Workspace</p><button type="button" className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-[#96acbc] hover:bg-[#112e47]">Overview</button><button type="button" onClick={() => selected && onSelect(selected.id)} className="flex w-full items-center gap-3 rounded-lg bg-[#123653] px-3 py-2.5 text-left font-semibold text-white"><span className="h-1.5 w-1.5 rounded-full bg-[#70d5dd]" />Watchlists</button><button type="button" className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-[#96acbc] hover:bg-[#112e47]">Preferences</button></nav>
      <div className="mt-8"><p className="px-3 pb-2 text-[0.63rem] font-bold uppercase tracking-[0.15em] text-[#66869e]">Your lists</p>{loading ? <div className="space-y-2 px-3"><div className="h-3 animate-pulse rounded bg-[#153652]" /><div className="h-3 w-3/4 animate-pulse rounded bg-[#153652]" /></div> : watchlists.map((watchlist) => <button key={watchlist.id} type="button" onClick={() => onSelect(watchlist.id)} className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm ${selected?.id === watchlist.id ? 'text-[#73d4dd]' : 'text-[#96acbc] hover:bg-[#112e47]'}`}><span className="truncate">{watchlist.name}</span><span className="text-xs text-[#66869e]">{watchlist.items.length}</span></button>)}</div>
      <div className="mt-auto border-t border-[#1a3851] px-3 pt-4"><p className="truncate text-sm font-semibold">{user.name}</p><p className="mt-1 truncate text-xs text-[#7895aa]">{user.email}</p></div>
    </aside>
  )
}

function WatchlistModal({ onClose, onCreated }: { onClose: () => void; onCreated: (watchlist: Watchlist) => void }) {
  const [name, setName] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const submit = async (event: FormEvent) => { event.preventDefault(); if (!name.trim()) return; setPending(true); setError(''); try { onCreated(await createWatchlist(name.trim())) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to create watchlist.') } finally { setPending(false) } }
  return <Modal title="New watchlist" onClose={onClose}><form className="mt-5 space-y-4" onSubmit={submit}><label className="block text-sm font-semibold text-[#29415f]">Watchlist name<input autoFocus required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Long-term technology" className="mt-2 h-11 w-full rounded-lg border border-[#d5e0e9] px-3 text-sm text-[#173554] outline-none focus:border-[#159bb7] focus:ring-4 focus:ring-[#e2f6f8]" /></label>{error && <p className="text-sm text-[#bd4b53]" role="alert">{error}</p>}<button disabled={pending || !name.trim()} className="h-11 w-full rounded-lg bg-[#153456] text-sm font-semibold text-white disabled:opacity-50">{pending ? 'Creating...' : 'Create watchlist'}</button></form></Modal>
}

function AddStockModal({ watchlist, onClose, onAdded }: { watchlist: Watchlist; onClose: () => void; onAdded: (watchlist: Watchlist) => void }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Instrument[]>([])
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { if (query.trim().length < 2) { setResults([]); return } const timer = window.setTimeout(() => searchInstruments(query).then(setResults).catch(() => setError('Unable to search instruments.')), 220); return () => window.clearTimeout(timer) }, [query])
  const add = async (ticker: string) => { setPending(true); setError(''); try { onAdded(await addTicker(watchlist.id, ticker)) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to add stock.') } finally { setPending(false) } }
  return <Modal title="Add stock to watchlist" onClose={onClose}><div className="mt-5"><label className="text-xs font-bold uppercase tracking-wider text-[#718198]" htmlFor="instrument-search">Search ticker or company</label><input autoFocus id="instrument-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search your market catalog" className="mt-2 h-11 w-full rounded-lg border border-[#d5e0e9] px-3 text-sm text-[#173554] outline-none focus:border-[#159bb7] focus:ring-4 focus:ring-[#e2f6f8]" />{error && <p className="mt-3 text-sm text-[#bd4b53]" role="alert">{error}</p>}<div className="mt-4 divide-y divide-[#edf1f5]">{results.map((instrument) => <div key={instrument.ticker} className="flex items-center justify-between py-3"><div><p className="text-sm font-semibold text-[#173554]">{instrument.companyName}</p><p className="mt-1 text-xs text-[#8291a4]">{instrument.ticker} · {instrument.exchange}</p></div><button type="button" disabled={pending || watchlist.items.some((item) => item.ticker === instrument.ticker)} onClick={() => add(instrument.ticker)} className="rounded-md bg-[#e8f7f8] px-3 py-1.5 text-xs font-bold text-[#148eaa] disabled:opacity-40">{watchlist.items.some((item) => item.ticker === instrument.ticker) ? 'Added' : 'Add'}</button></div>)}</div>{query.length >= 2 && !results.length && !error && <p className="py-5 text-center text-sm text-[#8291a4]">No matching instruments.</p>}</div></Modal>
}

function MarketRow({ row, onRemove }: { row: MarketData; onRemove: () => void }) {
  const statusStyle = row.marketStatus === 'MEANINGFUL' ? 'bg-[#fff0f0] text-[#bd4b53]' : row.marketStatus === 'WATCHING' ? 'bg-[#fff8e5] text-[#9b741e]' : 'bg-[#edf8f2] text-[#397457]'
  return <tr className="border-b border-[#edf1f5] last:border-0"><td className="px-4 py-4"><p className="font-display text-sm font-bold text-[#173554]">{row.ticker}</p><p className="mt-1 text-xs text-[#8291a4]">{row.exchange || '—'}</p></td><td className="px-4 py-4 text-sm text-[#4e6379]">{row.companyName || 'Company metadata unavailable'}</td><td className="px-4 py-4 text-sm font-semibold text-[#173554]">{row.price == null ? '—' : `$${row.price.toFixed(2)}`}</td><td className={`px-4 py-4 text-sm font-semibold ${row.changePercent == null ? 'text-[#8291a4]' : row.changePercent >= 0 ? 'text-[#397457]' : 'text-[#bd4b53]'}`}>{row.changePercent == null ? '—' : `${row.changePercent >= 0 ? '+' : ''}${row.changePercent.toFixed(2)}%`}</td><td className="px-4 py-4"><span className={`rounded-full px-2 py-1 text-[0.62rem] font-bold tracking-wide ${statusStyle}`}>{row.marketStatus}</span></td><td className="px-4 py-4 text-sm font-bold text-[#173554]">{row.materiality || '—'}</td><td className="max-w-[220px] px-4 py-4 text-xs leading-5 text-[#718198]">{row.narrative}</td><td className="px-4 py-4 text-right"><button type="button" onClick={onRemove} aria-label={`Remove ${row.ticker}`} className="text-lg text-[#8291a4] hover:text-[#bd4b53]">⋯</button></td></tr>
}

export default function Watchlists() {
  const { user } = useAuth()
  const [watchlists, setWatchlists] = useState<Watchlist[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [marketData, setMarketData] = useState<MarketData[]>([])
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [modal, setModal] = useState<'create' | 'add' | null>(null)
  const selected = watchlists.find((item) => item.id === selectedId) || null

  const load = async () => { setLoading(true); setError(''); try { const loaded = await listWatchlists(); setWatchlists(loaded); setSelectedId((current) => current && loaded.some((item) => item.id === current) ? current : loaded[0]?.id ?? null) } catch { setError('Unable to load your watchlists.') } finally { setLoading(false) } }
  useEffect(() => { load() }, [])
  useEffect(() => { if (!selectedId) { setMarketData([]); return } setDetailLoading(true); Promise.all([getWatchlist(selectedId), getMarketData(selectedId)]).then(([detail, data]) => { update(detail); setMarketData(data) }).catch(() => setError('Unable to load your watchlist details.')).finally(() => setDetailLoading(false)) }, [selectedId])

  const update = (watchlist: Watchlist) => setWatchlists((current) => current.map((item) => item.id === watchlist.id ? watchlist : item))
  const remove = async (ticker: string) => { if (!selected || !window.confirm(`Remove ${ticker} from ${selected.name}?`)) return; try { const updated = await removeTicker(selected.id, ticker); update(updated); setMarketData((current) => current.filter((row) => row.ticker !== ticker)) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to remove stock.') } }
  const rename = async () => { if (!selected) return; const name = window.prompt('Rename watchlist', selected.name)?.trim(); if (!name || name === selected.name) return; try { update(await renameWatchlist(selected.id, name)) } catch { setError('Unable to rename watchlist.') } }
  const removeList = async () => { if (!selected || !window.confirm(`Delete ${selected.name}?`)) return; try { await deleteWatchlist(selected.id); const next = watchlists.filter((item) => item.id !== selected.id); setWatchlists(next); setSelectedId(next[0]?.id ?? null) } catch { setError('Unable to delete watchlist.') } }
  const reviewed = async () => { if (!selected) return; try { update(await markWatchlistReviewed(selected.id)) } catch { setError('Unable to mark this watchlist as reviewed.') } }

  return <div className="workspace-paper min-h-[calc(100vh-76px)]"><div className="mx-auto flex min-h-[calc(100vh-76px)] max-w-[1500px]"><Sidebar user={user!} selected={selected} onSelect={setSelectedId} /><main className="min-w-0 flex-1 px-5 py-7 sm:px-8 lg:px-10"><div className="workspace-rule flex flex-col justify-between gap-4 border-b pb-7 sm:flex-row sm:items-end"><div><p className="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-[#a06d32]">The daily read</p><h1 className="mt-3 font-editorial text-5xl leading-none tracking-[-0.04em] text-[#1d2724]">Watchlists</h1><p className="mt-3 max-w-md text-sm leading-6 text-[#69736c]">Organize the market signals you want to follow.</p></div><button type="button" onClick={() => setModal('create')} className="inline-flex h-10 items-center justify-center rounded-md bg-[#1d2724] px-4 text-sm font-semibold text-[#f7f4eb] shadow-sm transition hover:bg-[#344139]">+ New Watchlist</button></div>{error && <div className="mt-5 flex items-center justify-between rounded-md border border-[#e5bfc0] bg-[#fff6f4] px-4 py-3 text-sm text-[#a3484c]" role="alert"><span>{error}</span><button type="button" onClick={load} className="font-semibold underline">Retry</button></div>}{loading ? <div className="mt-7 space-y-3">{[1, 2, 3].map((item) => <div key={item} className="h-28 animate-pulse rounded-md border border-[#dfded5] bg-[#fbfaf6]" />)}</div> : !watchlists.length ? <div className="mt-8 rounded-md border border-dashed border-[#c7c9bf] bg-[#fbfaf6] px-6 py-20 text-center"><p className="font-editorial text-2xl text-[#1d2724]">No watchlists yet</p><p className="mt-2 text-sm text-[#69736c]">Create your first watchlist to start tracking market changes.</p><button type="button" onClick={() => setModal('create')} className="mt-6 h-10 rounded-md bg-[#1d2724] px-4 text-sm font-semibold text-[#f7f4eb]">Create Watchlist</button></div> : <><div className="mt-7 grid gap-4 xl:grid-cols-2">{watchlists.map((watchlist) => <button key={watchlist.id} type="button" onClick={() => setSelectedId(watchlist.id)} className={`signal-card rounded-md border p-5 text-left transition hover:border-[#a6b3a2] ${selectedId === watchlist.id ? 'border-[#8da78e] ring-2 ring-[#e4eadf]' : ''}`}><div className="flex items-start justify-between"><div><h2 className="font-display text-base font-bold text-[#1d2724]">{watchlist.name}</h2><p className="mt-1 text-xs text-[#7a837b]">{watchlist.items.length} {watchlist.items.length === 1 ? 'stock' : 'stocks'}</p></div><span className="text-xs font-semibold text-[#5d785e]">Open →</span></div><div className="mt-5 flex flex-wrap gap-2">{watchlist.items.slice(0, 5).map((item) => <span key={item.ticker} className="rounded-sm bg-[#eff1e9] px-2 py-1 text-xs font-semibold text-[#596a5b]">{item.ticker}</span>)}{watchlist.items.length > 5 && <span className="px-1 py-1 text-xs text-[#7a837b]">+{watchlist.items.length - 5} more</span>}</div><p className="mt-5 text-xs text-[#7a837b]">Last updated: {new Date(watchlist.updatedAt).toLocaleString()}</p></button>)}</div>{selected && <section className="signal-card mt-8 rounded-md border"><div className="workspace-rule flex flex-col gap-4 border-b p-5 sm:flex-row sm:items-end sm:justify-between"><div><div className="flex items-center gap-3"><h2 className="font-editorial text-2xl text-[#1d2724]">{selected.name}</h2><button type="button" onClick={rename} className="text-xs font-semibold text-[#5d785e]">Rename</button><button type="button" onClick={removeList} className="text-xs font-semibold text-[#a3484c]">Delete</button></div><p className="mt-2 text-sm text-[#69736c]">{marketData.length} stocks · Last reviewed: {selected.reviewedAt ? new Date(selected.reviewedAt).toLocaleString() : 'Not yet reviewed'}</p></div><div className="flex gap-2"><button type="button" onClick={reviewed} className="h-9 rounded-md border border-[#c7c9bf] px-3 text-xs font-semibold text-[#596a5b] hover:border-[#8da78e]">Mark as Reviewed</button><button type="button" onClick={() => setModal('add')} className="h-9 rounded-md bg-[#1d2724] px-3 text-xs font-semibold text-[#f7f4eb]">+ Add Stock</button></div></div><div className="overflow-x-auto">{detailLoading ? <div className="space-y-2 p-5"><div className="h-12 animate-pulse rounded bg-[#eff1e9]" /><div className="h-12 animate-pulse rounded bg-[#eff1e9]" /></div> : !marketData.length ? <div className="p-12 text-center text-sm text-[#7a837b]">This watchlist has no market data yet.</div> : <table className="w-full min-w-[940px] text-left"><thead className="bg-[#f7f5ee] text-[0.65rem] font-bold uppercase tracking-[0.1em] text-[#7a837b]"><tr><th className="px-4 py-3">Ticker</th><th className="px-4 py-3">Company</th><th className="px-4 py-3">Current price</th><th className="px-4 py-3">Change</th><th className="px-4 py-3">Market status</th><th className="px-4 py-3">Materiality</th><th className="px-4 py-3">Narrative</th><th className="px-4 py-3 text-right">Actions</th></tr></thead><tbody>{marketData.map((row) => <MarketRow key={row.ticker} row={row} onRemove={() => remove(row.ticker)} />)}</tbody></table>}</div></section>}</>}</main></div>{modal === 'create' && <WatchlistModal onClose={() => setModal(null)} onCreated={(created) => { setWatchlists((current) => [created, ...current]); setSelectedId(created.id); setModal(null) }} />}{modal === 'add' && selected && <AddStockModal watchlist={selected} onClose={() => setModal(null)} onAdded={(updated) => { update(updated); setModal(null); getMarketData(updated.id).then(setMarketData) }} />}</div>
}
