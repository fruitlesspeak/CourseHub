import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore, type AuthSession } from '@/stores/authStore'

function buildSession(overrides: Partial<AuthSession> = {}): AuthSession {
  return {
    userId: 12,
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    role: 'STUDENT',
    dashboardPath: '/student/dashboard/12',
    ...overrides,
  }
}

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('returns /login as the default dashboard path when no session exists', () => {
    const store = useAuthStore()

    expect(store.defaultDashboardPath).toBe('/login')
  })

  it('derives a role-specific dashboard path when the session path is missing', () => {
    const store = useAuthStore()
    store.session = buildSession({ role: 'PROFESSOR', userId: 7, dashboardPath: '' })

    expect(store.defaultDashboardPath).toBe('/professor/dashboard/7')
  })

  it('fetchSession stores the returned session when the request succeeds', async () => {
    const store = useAuthStore()
    const session = buildSession()
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => session,
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await store.fetchSession(true)

    expect(result).toEqual(session)
    expect(store.session).toEqual(session)
    expect(store.isAuthenticated).toBe(true)
    expect(store.initialized).toBe(true)
    expect(store.isLoading).toBe(false)
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/session', {
      method: 'GET',
      credentials: 'include',
    })
  })

  it('fetchSession clears the session when the request is not ok', async () => {
    const store = useAuthStore()
    store.session = buildSession()
    const fetchMock = vi.fn().mockResolvedValue({ ok: false })
    vi.stubGlobal('fetch', fetchMock)

    const result = await store.fetchSession(true)

    expect(result).toBeNull()
    expect(store.session).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.initialized).toBe(true)
  })

  it('logout clears the session even if the network request fails', async () => {
    const store = useAuthStore()
    store.session = buildSession({ role: 'PROFESSOR', dashboardPath: '/professor/dashboard/12' })
    const fetchMock = vi.fn().mockRejectedValue(new Error('network down'))
    vi.stubGlobal('fetch', fetchMock)

    await expect(store.logout()).rejects.toThrow('network down')

    expect(store.session).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.initialized).toBe(true)
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
  })
})
