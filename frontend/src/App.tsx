import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'

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

    return (
        <main className="mx-auto max-w-6xl px-6 py-16 lg:px-10">
            <p className="text-sm font-semibold uppercase tracking-[0.28em] text-cyan-300">
                Dashboard
            </p>

            <h1 className="mt-5 text-5xl font-semibold text-white">
                Good to see you, {user?.name}.
            </h1>

            <p className="mt-5 max-w-xl text-lg leading-8 text-slate-400">
                Your market watch begins here. Watchlists and market intelligence
                arrive in the next product phase.
            </p>
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
```
