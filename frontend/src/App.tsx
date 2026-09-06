import { useEffect, useState, type ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AuthProvider, AuthScreen, useAuth } from './auth'
import Watchlists from './Watchlists'

type HealthResponse = { status: string; service: string }
const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function Shell({ children }: { children: ReactNode }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const isAuthRoute = location.pathname === '/login' || location.pathname === '/signup';
    const isWorkspaceRoute = location.pathname === '/watchlists' || location.pathname === '/dashboard';

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div className={isAuthRoute ? 'min-h-screen bg-[#fbfcfd]' : 'min-h-screen bg-[#f5f2ea]'}>
            <header className={isAuthRoute ? 'border-b border-[#e5ebf1] bg-white/90' : 'border-b border-[#d9d8cf] bg-[#f5f2ea]'}>
                <nav className="mx-auto flex h-[76px] max-w-[1440px] items-center justify-between px-6 sm:px-10 lg:px-14">
                    {isWorkspaceRoute ? <p className="text-[0.68rem] font-bold uppercase tracking-[0.2em] text-[#6d756d]">Market intelligence workspace</p> : <Link className={isAuthRoute ? 'text-lg font-semibold tracking-tight text-[#102d4e]' : 'text-lg font-semibold tracking-tight text-white'} to={user ? '/dashboard' : '/login'}>{isAuthRoute ? <span className="font-display">Market<span className="text-[#18a8c7]">Pulse</span></span> : <>Market<span className="text-cyan-300">Pulse</span></>}</Link>}

                    {isAuthRoute ? (
                        <div className="flex items-center gap-3 text-sm text-[#718198] sm:gap-4">
                            <span className="hidden sm:inline">{location.pathname === '/signup' ? 'Already have an account?' : "Don't have an account?"}</span>
                            <Link className="rounded-lg border border-[#d6e2eb] px-3.5 py-2 font-bold text-[#173554] transition hover:border-[#18a0bd] hover:text-[#148eaa]" to={location.pathname === '/signup' ? '/login' : '/signup'}>{location.pathname === '/signup' ? 'Log in' : 'Sign up'}</Link>
                        </div>
                    ) : user && (
                        <div className="flex items-center gap-5">
                            <span className="text-sm text-[#6d756d]">
                                {user.name}
                            </span>

                            <button
                                type="button"
                                className="text-sm font-medium text-[#6d756d] hover:text-[#1d2724]"
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
                            <AuthScreen mode="login" />
                        </PublicRoute>
                    }
                />

                <Route
                    path="/signup"
                    element={
                        <PublicRoute>
                            <AuthScreen mode="register" />
                        </PublicRoute>
                    }
                />

                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <Watchlists />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/watchlists"
                    element={
                        <ProtectedRoute>
                            <Watchlists />
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
