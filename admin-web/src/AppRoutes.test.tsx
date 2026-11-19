import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from './AppRoutes.tsx'
import { CITY_ADMIN, mockFetch, reply } from './test/api.ts'
import { makeUser, mockAuth } from './test/auth.ts'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('AppRoutes', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ isAuthenticated: true, user: makeUser({ name: 'Ana Pop' }) }),
    )
    mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'GET /admin/attractions?includeRemoved=false': reply.json({ items: [] }),
    })
  })

  it('opens the first section from the root path', () => {
    renderAt('/')

    expect(
      screen.getByRole('heading', { level: 1, name: 'City Administrator' }),
    ).toBeInTheDocument()
  })

  it('shows the attractions in the City Administrator section', async () => {
    renderAt('/city-administrator')

    expect(
      screen.getByRole('heading', { level: 1, name: 'City Administrator' }),
    ).toBeInTheDocument()
    expect(await screen.findByRole('heading', { level: 2, name: 'Attractions' })).toBeInTheDocument()
    expect(document.title).toBe('City Administrator - Traveler Admin')
  })

  it.each([
    ['/partner', 'Partner'],
    ['/traveler-administration', 'Traveler Administration'],
  ])('shows the empty page of %s', (path, title) => {
    renderAt(path)

    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument()
    expect(screen.getByText('This section is empty for now.')).toBeInTheDocument()
    expect(document.title).toBe(`${title} - Traveler Admin`)
  })

  it('moves between sections with the side menu', async () => {
    renderAt('/city-administrator')

    await userEvent.click(screen.getByRole('link', { name: 'Traveler Administration' }))

    expect(
      screen.getByRole('heading', { level: 1, name: 'Traveler Administration' }),
    ).toBeInTheDocument()
  })

  it('shows the signed-in user in the header', () => {
    renderAt('/partner')

    expect(screen.getByText('Signed in as Ana Pop')).toBeInTheDocument()
  })

  it('sends an unknown path to the first section', () => {
    renderAt('/no-such-page')

    expect(
      screen.getByRole('heading', { level: 1, name: 'City Administrator' }),
    ).toBeInTheDocument()
  })

  it('keeps a signed-out visitor out of the sections', () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth())

    renderAt('/partner')

    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })
})
