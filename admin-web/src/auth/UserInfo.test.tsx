import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mockAuth, makeUser } from '../test/auth.ts'
import UserInfo from './UserInfo.tsx'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

describe('UserInfo', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('shows the name of the signed-in user', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({
        isAuthenticated: true,
        user: makeUser({ name: 'Ana Pop', preferred_username: 'ana' }),
      }),
    )

    render(<UserInfo />)

    expect(screen.getByText('Signed in as Ana Pop')).toBeInTheDocument()
  })

  it('falls back to the username, then the email, then the subject', () => {
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ user: makeUser({ preferred_username: 'ana' }) }),
    )
    const { unmount } = render(<UserInfo />)
    expect(screen.getByText('Signed in as ana')).toBeInTheDocument()
    unmount()

    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ user: makeUser({ email: 'ana@example.test' }) }),
    )
    const second = render(<UserInfo />)
    expect(screen.getByText('Signed in as ana@example.test')).toBeInTheDocument()
    second.unmount()

    vi.mocked(useAuth).mockReturnValue(mockAuth({ user: makeUser() }))
    render(<UserInfo />)
    expect(screen.getByText('Signed in as user-1')).toBeInTheDocument()
  })

  it('signs out through the identity provider', async () => {
    const auth = mockAuth({ user: makeUser({ name: 'Ana Pop' }) })
    vi.mocked(useAuth).mockReturnValue(auth)

    render(<UserInfo />)
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(auth.signoutRedirect).toHaveBeenCalledTimes(1)
  })
})
