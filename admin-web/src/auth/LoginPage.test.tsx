import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mockAuth, makeUser } from '../test/auth.ts'
import LoginPage from './LoginPage.tsx'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function renderLogin(state?: unknown) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state }]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/partner" element={<p>partner page</p>} />
        <Route path="/" element={<p>home page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('starts the sign-in redirect and remembers where the user was going', async () => {
    const auth = mockAuth()
    vi.mocked(useAuth).mockReturnValue(auth)

    renderLogin({ from: { pathname: '/partner', search: '?tab=1', hash: '' } })
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(auth.signinRedirect).toHaveBeenCalledWith({
      state: { returnTo: '/partner?tab=1' },
    })
  })

  it('ignores a return location that points outside the app', async () => {
    const auth = mockAuth()
    vi.mocked(useAuth).mockReturnValue(auth)

    renderLogin({ from: { pathname: '//evil.example', search: '', hash: '' } })
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(auth.signinRedirect).toHaveBeenCalledWith({ state: { returnTo: '/' } })
  })

  it('shows an error when the sign-in could not start', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({
        error: Object.assign(new Error('metadata unreachable'), {
          source: 'signinRedirect' as const,
          args: undefined,
        }),
      }),
    )

    renderLogin()

    expect(screen.getByRole('alert')).toHaveTextContent('Sign-in failed')
  })

  it('does not report a failed sign-out as a failed sign-in', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({
        error: Object.assign(new Error('no end session endpoint'), {
          source: 'signoutRedirect' as const,
          args: undefined,
        }),
      }),
    )

    renderLogin()

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('moves a signed-in user on to the app', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ isAuthenticated: true, user: makeUser() }),
    )

    renderLogin()

    expect(screen.getByText('home page')).toBeInTheDocument()
  })
})
