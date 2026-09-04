import { useEffect, useState } from 'react'
import { Link, Route, Routes } from 'react-router-dom'

type HealthResponse = {
  status: string
  service: string
}

const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function Overview() {
  const [backendStatus, setBackendStatus] = useState<'checking' | 'connected' | 'unavailable'>('checking')

  useEffect(() => {
    const controller = new AbortController()

    fetch(`${backendUrl}/health`, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error('Health request failed')
        return response.json() as Promise<HealthResponse>
      })
      .then((health) => setBackendStatus(health.status === 'UP' ? 'connected' : 'unavailable'))
      .catch(() => setBackendStatus('unavailable'))

    return () => controller.abort()
  }, [])

  const statusLabel = backendStatus === 'connected'
    ? 'Backend Connected'
    : backendStatus === 'unavailable'
      ? 'Backend Unavailable'
      : 'Checking backend'

  return (
    <main className="mx-auto flex min-h-[calc(100vh-88px)] max-w-6xl items-center px-6 py-16 lg:px-10">
      <section className="grid w-full gap-12 lg:grid-cols-[1.15fr_0.85fr] lg:items-end">
        <div>
          <p className="mb-6 text-sm font-semibold uppercase tracking-[0.28em] text-cyan-300">Market intelligence, clarified</p>
          <h1 className="max-w-3xl text-5xl font-semibold leading-[0.98] tracking-tight text-white sm:text-7xl">
            See what changed. <span className="text-cyan-300">Understand why.</span>
          </h1>
          <p className="mt-8 max-w-xl text-lg leading-8 text-slate-300">
            MarketPulse is the foundation for a calmer, clearer way to revisit the market and know what deserves your attention.
          </p>
          <div className="mt-10 flex flex-wrap items-center gap-4">
            <Link className="rounded-full bg-cyan-300 px-5 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-200" to="/status">
              View system status
            </Link>
            <span className={`rounded-full border px-4 py-3 text-sm font-semibold ${backendStatus === 'connected' ? 'border-emerald-400/40 text-emerald-300' : 'border-slate-600 text-slate-300'}`}>
              {statusLabel}
            </span>
          </div>
        </div>
        <div className="border-l border-slate-700 pl-8 lg:mb-2">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-slate-500">Foundation phase</p>
          <dl className="mt-8 space-y-6">
            <div><dt className="text-sm text-slate-500">Frontend</dt><dd className="mt-1 text-xl text-white">React + TypeScript</dd></div>
            <div><dt className="text-sm text-slate-500">API</dt><dd className="mt-1 text-xl text-white">Spring Boot</dd></div>
            <div><dt className="text-sm text-slate-500">Data layer</dt><dd className="mt-1 text-xl text-white">MySQL</dd></div>
          </dl>
        </div>
      </section>
    </main>
  )
}

function Status() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-16 lg:px-10">
      <p className="text-sm font-semibold uppercase tracking-[0.28em] text-cyan-300">System status</p>
      <h1 className="mt-5 text-4xl font-semibold text-white">Local development foundation</h1>
      <p className="mt-4 max-w-2xl text-slate-300">The product services are being connected one boundary at a time.</p>
      <div className="mt-12 grid gap-4 sm:grid-cols-3">
        {['Frontend shell', 'Spring Boot API', 'FastAPI service'].map((service) => (
          <div className="border border-slate-700 bg-slate-900/60 p-6" key={service}>
            <div className="mb-8 h-2 w-2 rounded-full bg-cyan-300" />
            <p className="font-medium text-white">{service}</p>
            <p className="mt-2 text-sm text-slate-400">Ready for the next implementation phase.</p>
          </div>
        ))}
      </div>
    </main>
  )
}

export default function App() {
  return (
    <div className="min-h-screen bg-slate-950">
      <header className="border-b border-slate-800/80">
        <nav className="mx-auto flex h-[88px] max-w-6xl items-center justify-between px-6 lg:px-10">
          <Link className="text-lg font-semibold tracking-tight text-white" to="/">Market<span className="text-cyan-300">Pulse</span></Link>
          <Link className="text-sm font-medium text-slate-400 transition hover:text-white" to="/status">Status</Link>
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<Overview />} />
        <Route path="/status" element={<Status />} />
      </Routes>
    </div>
  )
}
