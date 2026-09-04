import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

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
