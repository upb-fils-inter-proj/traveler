import { render, screen } from '@testing-library/react'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mockAuth, makeUser } from '../test/auth.ts'
import RequireAuth from './RequireAuth.tsx'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function LoginProbe() {
  const location = useLocation()
  return (
    <p>
      login screen, from {(location.state as { from: { pathname: string } }).from.pathname}
    </p>
  )
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<LoginProbe />} />
        <Route element={<RequireAuth />}>
          <Route path="/partner" element={<p>partner page</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAuth', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('sends an unauthenticated user to the login screen', () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isAuthenticated: false }))

    renderAt('/partner')

    expect(screen.getByText('login screen, from /partner')).toBeInTheDocument()
    expect(screen.queryByText('partner page')).not.toBeInTheDocument()
  })

  it('renders the page for a signed-in user', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ isAuthenticated: true, user: makeUser({ name: 'Ana Pop' }) }),
    )

    renderAt('/partner')

    expect(screen.getByText('partner page')).toBeInTheDocument()
  })

  it('waits while the sign-in state is loading', () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isLoading: true }))

    renderAt('/partner')

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.queryByText('partner page')).not.toBeInTheDocument()
    expect(screen.queryByText(/login screen/)).not.toBeInTheDocument()
  })
})
