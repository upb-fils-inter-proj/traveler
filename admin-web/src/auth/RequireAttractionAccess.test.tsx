import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setLanguage } from '../i18n/index.ts'
import { CITY_ADMIN, mockFetch, reply } from '../test/api.ts'
import { makeUser, mockAuth } from '../test/auth.ts'
import RequireAttractionAccess from './RequireAttractionAccess.tsx'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function renderGuard() {
  return render(
    <MemoryRouter initialEntries={['/city-administrator']}>
      <Routes>
        <Route path="/city-administrator" element={<RequireAttractionAccess />}>
          <Route index element={<p>attraction screens</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAttractionAccess', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isAuthenticated: true, user: makeUser() }))
  })

  it.each(['city_admin', 'traveler_admin'])('shows the screens to the role %s', async (role) => {
    mockFetch({ 'GET /admin/me': reply.json({ ...CITY_ADMIN, role }) })

    renderGuard()

    expect(await screen.findByText('attraction screens')).toBeInTheDocument()
  })

  it('shows a no access message when the backend answers 403', async () => {
    mockFetch({ 'GET /admin/me': reply.problem(403, 'Not a staff member') })

    renderGuard()

    expect(await screen.findByText('No access')).toBeInTheDocument()
    expect(screen.queryByText('attraction screens')).not.toBeInTheDocument()
  })

  it('shows the no access message in Romanian to a partner', async () => {
    await setLanguage('ro')
    mockFetch({ 'GET /admin/me': reply.json({ ...CITY_ADMIN, role: 'partner' }) })

    renderGuard()

    expect(await screen.findByText('Fără acces')).toBeInTheDocument()
    expect(screen.queryByText('attraction screens')).not.toBeInTheDocument()
  })

  it('offers to try again when the check fails for another reason', async () => {
    const fetchMock = mockFetch({ 'GET /admin/me': reply.problem(503, 'Database down') })

    renderGuard()

    expect(await screen.findByText('Server message: Database down')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
