import { render, screen } from '@testing-library/react'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mockAuth, makeUser } from '../test/auth.ts'
import CallbackPage from './CallbackPage.tsx'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function renderCallback() {
  return render(
    <MemoryRouter initialEntries={['/auth/callback']}>
      <Routes>
        <Route path="/auth/callback" element={<CallbackPage />} />
        <Route path="/login" element={<p>login screen</p>} />
        <Route path="/partner" element={<p>partner page</p>} />
        <Route path="/" element={<p>home page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('CallbackPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('waits while the code is exchanged for tokens', () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isLoading: true }))

    renderCallback()

    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('returns the user to the page they asked for', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({
        isAuthenticated: true,
        user: makeUser({}, { returnTo: '/partner' }),
      }),
    )

    renderCallback()

    expect(screen.getByText('partner page')).toBeInTheDocument()
  })

  it('goes to the home page when no return path was stored', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ isAuthenticated: true, user: makeUser() }),
    )

    renderCallback()

    expect(screen.getByText('home page')).toBeInTheDocument()
  })

  it('shows an error with a way back when the sign-in failed', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({
        error: Object.assign(new Error('bad state'), {
          source: 'signinCallback' as const,
        }),
      }),
    )

    renderCallback()

    expect(screen.getByRole('alert')).toHaveTextContent('Sign-in failed')
    expect(
      screen.getByRole('link', { name: 'Back to the login screen' }),
    ).toHaveAttribute('href', '/login')
  })

  it('sends a visitor without a sign-in response to the login screen', () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth())

    renderCallback()

    expect(screen.getByText('login screen')).toBeInTheDocument()
  })
})
