import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from '../AppRoutes.tsx'
import { CITY_ADMIN, makeAttraction, mockFetch, reply } from '../test/api.ts'
import { makeUser, mockAuth } from '../test/auth.ts'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

const pelesCastle = makeAttraction()
const bigWaterfall = makeAttraction({
  id: 'a2',
  status: 'closed',
  pointValue: 20,
  version: 50,
  translations: {
    ro: { name: 'Cascada Urlătoarea', description: 'Cascadă.', history: null, sources: null },
    en: { name: 'Urlătoarea Waterfall', description: 'Waterfall.', history: null, sources: null },
  },
})
const oldHut = makeAttraction({
  id: 'a3',
  status: 'removed',
  translations: {
    ro: { name: 'Cabana veche', description: 'Cabană.', history: null, sources: null },
    en: { name: 'Old hut', description: 'Hut.', history: null, sources: null },
  },
})

function renderList() {
  return render(
    <MemoryRouter initialEntries={['/city-administrator']}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('AttractionListPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isAuthenticated: true, user: makeUser() }))
  })

  it('lists the attractions from the API', async () => {
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'GET /admin/attractions?includeRemoved=false': reply.json({
        items: [pelesCastle, bigWaterfall],
      }),
    })

    renderList()

    const table = await screen.findByRole('table', { name: 'Attractions' })
    const rows = within(table)
      .getAllByRole('row')
      .map((row) => Array.from((row as HTMLTableRowElement).cells, (cell) => cell.textContent))
    expect(rows).toEqual([
      ['Romanian name', 'English name', 'Status', 'Points', 'Version'],
      ['Castelul Peleș', 'Peleș Castle', 'Open', '10', '42'],
      ['Cascada Urlătoarea', 'Urlătoarea Waterfall', 'Closed', '20', '50'],
    ])
    expect(screen.getByRole('link', { name: 'Castelul Peleș' })).toHaveAttribute(
      'href',
      '/city-administrator/attractions/a1',
    )
    expect(screen.getByRole('link', { name: 'Add attraction' })).toHaveAttribute(
      'href',
      '/city-administrator/attractions/new',
    )
    const [, init] = fetchMock.mock.calls[1] ?? []
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer test-access-token')
  })

  it('asks for the removed attractions when the filter is on', async () => {
    mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'GET /admin/attractions?includeRemoved=false': reply.json({ items: [pelesCastle] }),
      'GET /admin/attractions?includeRemoved=true': reply.json({ items: [oldHut, pelesCastle] }),
    })
    renderList()
    await screen.findByRole('table')

    await userEvent.click(screen.getByRole('checkbox', { name: 'Show removed attractions' }))

    expect(await screen.findByRole('link', { name: 'Cabana veche' })).toBeInTheDocument()
    expect(screen.getByText('Removed')).toBeInTheDocument()
  })

  it('says when there are no attractions', async () => {
    mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'GET /admin/attractions?includeRemoved=false': reply.json({ items: [] }),
    })

    renderList()

    expect(await screen.findByText('There are no attractions yet.')).toBeInTheDocument()
  })
})
