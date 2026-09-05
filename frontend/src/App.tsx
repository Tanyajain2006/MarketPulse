import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import {
    addTicker,
    createWatchlist,
    deleteWatchlist,
    listWatchlists,
    removeTicker,
    renameWatchlist,
    type Watchlist,
} from './watchlists'

type HealthResponse = { status: string; service: string }
const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function Shell({ children }: { children: ReactNode }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-slate-950">
            <header className="border-b border-slate-800/80">
                <nav className="mx-auto flex h-[88px] max-w-6xl items-center justify-between px-6 lg:px-10">
                    <Link
                        className="text-lg font-semibold tracking-tight text-white"
                        to={user ? '/dashboard' : '/login'}
                    >
                        Market<span className="text-cyan-300">Pulse</span>
                    </Link>

                    {user && (
                        <div className="flex items-center gap-5">
                            <span className="text-sm text-slate-400">
                                {user.name}
                            </span>

                            <button
                                type="button"
                                className="text-sm font-medium text-slate-400 hover:text-white"
                                onClick={handleLogout}
                            >
                                Log out
                            </button>
                        </div>
                    )}
                </nav>
            </header>

            {children}
        </div>
    );
}

type AuthMode = 'login' | 'register';

function AuthCard({ mode }: { mode: AuthMode }) {
    const { user, login, register } = useAuth();
    const navigate = useNavigate();

    const isRegister = mode === 'register';

    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [pending, setPending] = useState(false);

    if (user) {
        return <Navigate to="/dashboard" replace />;
    }

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        setError('');
        setPending(true);

        try {
            if (isRegister) {
                await register({
                    name,
                    email,
                    password,
                });
            } else {
                await login({
                    email,
                    password,
                });
            }

            navigate('/dashboard');
        } catch (reason) {
            setError(
                reason instanceof Error
                    ? reason.message
                    : 'Unable to authenticate.'
            );
        } finally {
            setPending(false);
        }
    };

    return (
        <main className="mx-auto flex min-h-[calc(100vh-88px)] max-w-6xl items-center justify-center px-6 py-12">
            <section className="w-full max-w-md border border-slate-800 bg-slate-900/70 p-8">
                <p className="text-sm font-semibold uppercase tracking-[0.24em] text-cyan-300">
                    MarketPulse
                </p>

                <h1 className="mt-5 text-4xl font-semibold text-white">
                    {isRegister ? 'Create your account' : 'Welcome back'}
                </h1>

                <p className="mt-3 text-slate-400">
                    {isRegister
                        ? 'Start with a clearer view of what changed.'
                        : 'Return to your market intelligence workspace.'}
                </p>

                <form
                    className="mt-8 space-y-5"
                    onSubmit={handleSubmit}
                >
                    {isRegister && (
                        <label className="block text-sm text-slate-300">
                            Name

                            <input
                                required
                                minLength={2}
                                maxLength={100}
                                value={name}
                                onChange={(event) => setName(event.target.value)}
                                className="mt-2 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-white"
                            />
                        </label>
                    )}

                    <label className="block text-sm text-slate-300">
                        Email

                        <input
                            required
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            className="mt-2 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-white"
                        />
                    </label>

                    <label className="block text-sm text-slate-300">
                        Password

                        <input
                            required
                            minLength={8}
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            className="mt-2 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-white"
                        />
                    </label>

                    {error && (
                        <p
                            className="text-sm text-rose-300"
                            role="alert"
                        >
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={pending}
                        className="w-full bg-cyan-300 px-5 py-3 font-bold text-slate-950 disabled:opacity-60"
                    >
                        {pending
                            ? 'Please wait...'
                            : isRegister
                                ? 'Create account'
                                : 'Log in'}
                    </button>
                </form>

                <p className="mt-6 text-sm text-slate-400">
                    {isRegister
                        ? 'Already have an account?'
                        : 'New to MarketPulse?'}

                    {' '}

                    <Link
                        className="font-semibold text-cyan-300"
                        to={isRegister ? '/login' : '/signup'}
                    >
                        {isRegister ? 'Log in' : 'Sign up'}
                    </Link>
                </p>
            </section>
        </main>
    );
}

function Dashboard() {
    const { user } = useAuth();
    const [watchlists, setWatchlists] = useState<Watchlist[]>([]);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [newName, setNewName] = useState('');
    const [ticker, setTicker] = useState('');
    const [editing, setEditing] = useState(false);
    const [nameDraft, setNameDraft] = useState('');
    const [loading, setLoading] = useState(true);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');

    const selected = watchlists.find((watchlist) => watchlist.id === selectedId) || null;

    useEffect(() => {
        let active = true;
        listWatchlists()
            .then((loaded) => {
                if (!active) return;
                setWatchlists(loaded);
                setSelectedId(loaded[0]?.id ?? null);
            })
            .catch((reason) => active && setError(reason instanceof Error ? reason.message : 'Unable to load watchlists.'))
            .finally(() => active && setLoading(false));
        return () => { active = false; };
    }, []);

    const updateSelected = (updated: Watchlist) => {
        setWatchlists((current) => current.map((watchlist) => watchlist.id === updated.id ? updated : watchlist));
    };

    const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!newName.trim()) return;
        setPending(true); setError('');
        try {
            const created = await createWatchlist(newName.trim());
            setWatchlists((current) => [created, ...current]);
            setSelectedId(created.id); setNewName('');
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to create watchlist.');
        } finally { setPending(false); }
    };

    const handleRename = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!selected || !nameDraft.trim()) return;
        setPending(true); setError('');
        try {
            updateSelected(await renameWatchlist(selected.id, nameDraft.trim()));
            setEditing(false);
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to rename watchlist.');
        } finally { setPending(false); }
    };

    const handleDelete = async () => {
        if (!selected || !window.confirm(`Delete ${selected.name}?`)) return;
        setPending(true); setError('');
        try {
            await deleteWatchlist(selected.id);
            const remaining = watchlists.filter((watchlist) => watchlist.id !== selected.id);
            setWatchlists(remaining); setSelectedId(remaining[0]?.id ?? null);
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to delete watchlist.');
        } finally { setPending(false); }
    };

    const handleAddTicker = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!selected || !/^[a-zA-Z0-9.-]{1,15}$/.test(ticker.trim())) {
            setError('Enter a valid ticker using up to 15 letters, numbers, dots, or dashes.');
            return;
        }
        setPending(true); setError('');
        try {
            updateSelected(await addTicker(selected.id, ticker.trim()));
            setTicker('');
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to add ticker.');
        } finally { setPending(false); }
    };

    const handleRemoveTicker = async (symbol: string) => {
        if (!selected) return;
        setPending(true); setError('');
        try { updateSelected(await removeTicker(selected.id, symbol)); }
        catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to remove ticker.'); }
        finally { setPending(false); }
    };

    if (loading) {
        return <main className="mx-auto max-w-6xl px-6 py-16 lg:px-10"><p className="text-slate-400">Loading your watchlists...</p></main>;
    }

    return (
        <main className="mx-auto max-w-6xl px-6 py-12 lg:px-10">
            <div className="flex flex-col gap-3 border-b border-slate-800 pb-10 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <p className="text-sm font-semibold uppercase tracking-[0.28em] text-cyan-300">Workspace</p>
                    <h1 className="mt-4 text-4xl font-semibold text-white">Good to see you, {user?.name}.</h1>
                    <p className="mt-3 text-slate-400">Keep the symbols that matter close at hand.</p>
                </div>
                <span className="text-sm text-slate-500">{watchlists.length} {watchlists.length === 1 ? 'watchlist' : 'watchlists'}</span>
            </div>

            {error && <p className="mt-6 border border-rose-400/30 bg-rose-400/10 px-4 py-3 text-sm text-rose-200" role="alert">{error}</p>}

            <div className="mt-8 grid gap-8 lg:grid-cols-[260px_1fr]">
                <aside className="border border-slate-800 bg-slate-900/50 p-5">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold text-white">Watchlists</h2>
                        <span className="text-xs text-slate-500">{watchlists.length}</span>
                    </div>
                    <div className="mt-5 space-y-2">
                        {watchlists.map((watchlist) => (
                            <button key={watchlist.id} type="button" onClick={() => setSelectedId(watchlist.id)} className={`flex w-full items-center justify-between px-3 py-3 text-left text-sm transition ${selectedId === watchlist.id ? 'bg-cyan-300 font-semibold text-slate-950' : 'text-slate-300 hover:bg-slate-800'}`}>
                                <span className="truncate">{watchlist.name}</span><span className={selectedId === watchlist.id ? 'text-slate-700' : 'text-slate-500'}>{watchlist.items.length}</span>
                            </button>
                        ))}
                        {!watchlists.length && <p className="text-sm leading-6 text-slate-500">Create your first list to start tracking symbols.</p>}
                    </div>
                    <form className="mt-6 border-t border-slate-800 pt-5" onSubmit={handleCreate}>
                        <label className="text-xs font-semibold uppercase tracking-wider text-slate-500" htmlFor="new-watchlist">New watchlist</label>
                        <div className="mt-2 flex gap-2">
                            <input id="new-watchlist" value={newName} onChange={(event) => setNewName(event.target.value)} maxLength={100} placeholder="e.g. Long term" className="min-w-0 flex-1 border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-cyan-300" />
                            <button disabled={pending || !newName.trim()} className="bg-cyan-300 px-3 py-2 text-sm font-bold text-slate-950 disabled:opacity-40" title="Create watchlist" type="submit">+</button>
                        </div>
                    </form>
                </aside>

                <section className="min-w-0">
                    {!selected ? (
                        <div className="border border-dashed border-slate-700 px-6 py-16 text-center"><p className="text-lg font-semibold text-white">No watchlist selected</p><p className="mt-2 text-sm text-slate-500">Create a watchlist from the panel to begin.</p></div>
                    ) : (
                        <>
                            <div className="flex flex-col gap-4 border-b border-slate-800 pb-6 sm:flex-row sm:items-center sm:justify-between">
                                {editing ? <form className="flex gap-2" onSubmit={handleRename}><input autoFocus value={nameDraft} onChange={(event) => setNameDraft(event.target.value)} maxLength={100} className="border border-slate-700 bg-slate-950 px-3 py-2 text-white outline-none focus:border-cyan-300" /><button disabled={pending || !nameDraft.trim()} className="bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950 disabled:opacity-40">Save</button></form> : <h2 className="text-2xl font-semibold text-white">{selected.name}</h2>}
                                <div className="flex gap-4 text-sm"><button type="button" onClick={() => { setNameDraft(selected.name); setEditing(true); }} className="text-slate-400 hover:text-white">Rename</button><button type="button" onClick={handleDelete} disabled={pending} className="text-rose-300 hover:text-rose-200 disabled:opacity-40">Delete</button></div>
                            </div>
                            <form className="mt-6 flex max-w-lg gap-3" onSubmit={handleAddTicker}>
                                <input value={ticker} onChange={(event) => setTicker(event.target.value)} maxLength={15} placeholder="Add ticker, e.g. AAPL" aria-label="Ticker symbol" className="min-w-0 flex-1 border border-slate-700 bg-slate-900 px-4 py-3 text-white uppercase outline-none placeholder:normal-case focus:border-cyan-300" />
                                <button disabled={pending || !ticker.trim()} type="submit" className="bg-white px-5 py-3 text-sm font-bold text-slate-950 disabled:opacity-40">Add ticker</button>
                            </form>
                            {!selected.items.length ? <div className="mt-10 border border-dashed border-slate-700 px-6 py-16 text-center"><p className="text-lg font-semibold text-white">This watchlist is empty</p><p className="mt-2 text-sm text-slate-500">Add a ticker above to begin building your market view.</p></div> : <div className="mt-8 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{selected.items.map((item) => <div key={item.ticker} className="flex items-center justify-between border border-slate-800 bg-slate-900/60 px-4 py-5"><span className="font-semibold tracking-wide text-white">{item.ticker}</span><button type="button" onClick={() => handleRemoveTicker(item.ticker)} disabled={pending} className="text-xs text-slate-500 hover:text-rose-300 disabled:opacity-40">Remove</button></div>)}</div>}
                        </>
                    )}
                </section>
            </div>
        </main>
    );
}

function Status() {
    const [status, setStatus] = useState('Checking backend');

    useEffect(() => {
        const checkBackendHealth = async () => {
            try {
                const response = await fetch(`${backendUrl}/health`);

                if (!response.ok) {
                    throw new Error('Health check failed');
                }

                const health = (await response.json()) as HealthResponse;

                setStatus(
                    health.status === 'UP'
                        ? 'Backend Connected'
                        : 'Backend Unavailable'
                );
            } catch {
                setStatus('Backend Unavailable');
            }
        };

        checkBackendHealth();
    }, []);

    return (
        <main className="mx-auto max-w-6xl px-6 py-16">
            <span className="rounded-full border border-slate-700 px-4 py-3 text-sm font-semibold text-slate-300">
                {status}
            </span>
        </main>
    );
}

function ProtectedRoute({ children }: { children: ReactNode }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    return <>{children}</>;
}

function PublicRoute({ children }: { children: ReactNode }) {
    const { user } = useAuth();

    if (user) {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
}

function AppRoutes() {
    return (
        <Shell>
            <Routes>
                <Route
                    path="/"
                    element={<Navigate to="/dashboard" replace />}
                />

                <Route
                    path="/login"
                    element={
                        <PublicRoute>
                            <AuthCard mode="login" />
                        </PublicRoute>
                    }
                />

                <Route
                    path="/signup"
                    element={
                        <PublicRoute>
                            <AuthCard mode="register" />
                        </PublicRoute>
                    }
                />

                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <Dashboard />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/status"
                    element={<Status />}
                />

                <Route
                    path="*"
                    element={<Navigate to="/dashboard" replace />}
                />
            </Routes>
        </Shell>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    );
}
