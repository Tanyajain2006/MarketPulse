import { createContext, useContext, useId, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'

type User = { id: number; name: string; email: string }
type AuthResponse = { token: string; user: User }
type Credentials = { email: string; password: string }
type RegisterInput = Credentials & { name: string }

type AuthContextValue = {
  user: User | null
  token: string | null
  login: (input: Credentials) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'marketpulse_token'
const USER_KEY = 'marketpulse_user'

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

async function request(path: string, method: 'POST', body: object): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const payload = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(payload.error || 'The request could not be completed.')
  return payload as AuthResponse
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<User | null>(() => {
    const stored = sessionStorage.getItem(USER_KEY)
    return stored ? JSON.parse(stored) as User : null
  })

  const saveSession = (session: AuthResponse) => {
    sessionStorage.setItem(TOKEN_KEY, session.token)
    sessionStorage.setItem(USER_KEY, JSON.stringify(session.user))
    setToken(session.token)
    setUser(session.user)
  }

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    login: async (input) => saveSession(await request('/auth/login', 'POST', input)),
    register: async (input) => saveSession(await request('/auth/register', 'POST', input)),
    logout: () => {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
      setToken(null)
      setUser(null)
    },
  }), [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

export function getAuthToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

type AuthMode = 'login' | 'register'

type AuthScreenProps = {
  mode: AuthMode
}

type FieldProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  placeholder: string
  icon: ReactNode
  error?: string
  autoComplete: string
  minLength?: number
  maxLength?: number
}

function BrandMark() {
  return (
    <span className="auth-logo" aria-label="MarketPulse">
      <span className="auth-logo-mark" aria-hidden="true">
        <span className="auth-logo-letter">M</span>
        <span className="auth-logo-dot" />
      </span>
      <span className="auth-logo-wordmark">Market<span>Pulse</span></span>
    </span>
  )
}

function SparkIcon() {
  return <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><path strokeLinecap="round" strokeLinejoin="round" d="m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7L12 3Z" /><path strokeLinecap="round" d="m19 16 .6 1.9L21.5 18l-1.9.6L19 20.5l-.6-1.9-1.9-.6 1.9-.6L19 16Z" /></svg>
}

function EyeIcon({ visible }: { visible: boolean }) {
  return visible ? <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><path strokeLinecap="round" strokeLinejoin="round" d="M2.5 12s3.4-6 9.5-6 9.5 6 9.5 6-3.4 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.4" /></svg> : <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><path strokeLinecap="round" strokeLinejoin="round" d="m3 3 18 18M10.6 6.2A9.7 9.7 0 0 1 12 6c6.1 0 9.5 6 9.5 6a16.6 16.6 0 0 1-3.1 3.7M6.2 6.8C3.8 8.4 2.5 12 2.5 12s3.4 6 9.5 6c1.1 0 2.1-.2 3-.5" /></svg>
}

function UserIcon() {
  return <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><circle cx="12" cy="8" r="3.2" /><path strokeLinecap="round" d="M5.5 20c.8-3.4 3-5.1 6.5-5.1s5.7 1.7 6.5 5.1" /></svg>
}

function MailIcon() {
  return <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><rect width="18" height="14" x="3" y="5" rx="2" /><path strokeLinecap="round" d="m4 7 8 6 8-6" /></svg>
}

function LockIcon() {
  return <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8"><rect width="14" height="11" x="5" y="10" rx="2" /><path strokeLinecap="round" d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
}

function Field({ id, label, value, onChange, type = 'text', placeholder, icon, error, autoComplete, minLength, maxLength }: FieldProps) {
  const [visible, setVisible] = useState(false)
  const inputType = type === 'password' && visible ? 'text' : type
  const hasError = Boolean(error)

  return (
    <div className={`auth-field ${hasError ? 'auth-field-error' : ''}`}>
      <label className="auth-label" htmlFor={id}>
        <span>{label}</span>
        {hasError && <span className="auth-error-text">{error}</span>}
      </label>
      <div className="auth-input-wrap">
        <input id={id} name={id} required minLength={minLength} maxLength={maxLength} type={inputType} value={value} onChange={(event) => onChange(event.target.value)} autoComplete={autoComplete} placeholder={placeholder} aria-invalid={hasError} aria-describedby={hasError ? `${id}-error` : undefined} className="auth-input" />
        {type === 'password' && <button type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible((current) => !current)} className="auth-password-toggle"><EyeIcon visible={visible} /></button>}
      </div>
      {hasError && <p id={`${id}-error`} className="auth-error-message">{error}</p>}
    </div>
  )
}

function AuthCard({ mode }: AuthScreenProps) {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const isRegister = mode === 'register'
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const nameId = useId()
  const emailId = useId()
  const passwordId = useId()

  if (user) return <Navigate to="/dashboard" replace />

  const validate = () => {
    const next: Record<string, string> = {}
    if (isRegister && name.trim().length < 2) next[nameId] = 'Enter at least 2 characters.'
    if (!email.trim() || !/^\S+@\S+\.\S+$/.test(email)) next[emailId] = 'Enter a valid email.'
    if (password.length < 8) next[passwordId] = 'Use at least 8 characters.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    if (!validate()) return
    setPending(true)
    try {
      if (isRegister) await register({ name, email, password })
      else await login({ email, password })
      navigate('/dashboard')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to authenticate.')
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-content">
        <BrandMark />
        <div className="auth-intro">
          <p className="auth-eyebrow">{isRegister ? 'Start with a clean slate' : 'Welcome back'}</p>
          <h1>{isRegister ? 'Open your workspace.' : 'Pick up where you left off.'}</h1>
          <p className="auth-subtitle">{isRegister ? 'Build a calmer way to read the markets.' : 'Your last checkpoint is waiting.'}</p>
        </div>
        {error && <div className="auth-alert" role="alert"><span className="auth-alert-dot" />{error}</div>}
        <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {isRegister && <Field id={nameId} label="Name" value={name} onChange={setName} placeholder="Your full name" autoComplete="name" minLength={2} maxLength={100} icon={<UserIcon />} error={errors[nameId]} />}
          <Field id={emailId} label="Email" value={email} onChange={setEmail} type="email" placeholder="" autoComplete="email" icon={<MailIcon />} error={errors[emailId]} />
          <Field id={passwordId} label="Password" value={password} onChange={setPassword} type="password" placeholder="" autoComplete={isRegister ? 'new-password' : 'current-password'} minLength={8} icon={<LockIcon />} error={errors[passwordId]} />
          <button type="submit" disabled={pending} className="auth-submit" aria-label={pending ? (isRegister ? 'Creating account' : 'Signing in') : (isRegister ? 'Create account' : 'Log in')}>
            <span>{pending ? (isRegister ? 'Creating account...' : 'Signing in...') : (isRegister ? 'Create account' : 'Log in')}</span>
          </button>
          </form>
        <p className="auth-footer">{isRegister ? 'Already have an account?' : 'New to MarketPulse?'}{' '}<Link to={isRegister ? '/login' : '/signup'}>{isRegister ? 'Sign in' : 'Create an account'}</Link></p>
      </div>
    </section>
  )
}

export function AuthScreen({ mode }: AuthScreenProps) {
  return <main><AuthCard mode={mode} /></main>
}
