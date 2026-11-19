import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { LatLon } from '../api/attractions.ts'
import AppRoutes from '../AppRoutes.tsx'
import { CITY_ADMIN, makeAttraction, mockFetch, reply, requestsTo } from '../test/api.ts'
import { makeUser, mockAuth } from '../test/auth.ts'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))
// Leaflet needs a real browser; the stand-in places the pin like a map click.
vi.mock('./LocationMap.tsx', () => ({
  default: ({ onChange }: { onChange: (position: LatLon) => void }) => (
    <button type="button" onClick={() => onChange({ lat: 45.3599871, lon: 25.5428 })}>
      Place pin on map
    </button>
  ),
}))

const user = userEvent.setup()

function renderNewAttraction() {
  return render(
    <MemoryRouter initialEntries={['/city-administrator/attractions/new']}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

function language(name: 'Romanian' | 'English') {
  return within(screen.getByRole('group', { name }))
}

async function fillValidForm() {
  await user.type(language('Romanian').getByLabelText('Name'), '  Cascada Urlătoarea ')
  await user.type(language('Romanian').getByLabelText('Description'), 'Cascadă de 15 m.')
  await user.type(language('Romanian').getByLabelText('History (optional)'), 'Cunoscută din 1900.')
  await user.type(language('English').getByLabelText('Name'), 'Urlătoarea Waterfall')
  await user.type(language('English').getByLabelText('Description'), 'A 15 m waterfall.')
  await user.type(screen.getByLabelText('Points'), '25')
  await user.click(await screen.findByRole('button', { name: 'Place pin on map' }))
}

describe('NewAttractionPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isAuthenticated: true, user: makeUser() }))
  })

  it('checks the fields before sending anything', async () => {
    const fetchMock = mockFetch({ 'GET /admin/me': reply.json(CITY_ADMIN) })
    renderNewAttraction()
    await screen.findByRole('heading', { name: 'New attraction' })

    await user.type(language('English').getByLabelText('Name'), 'x'.repeat(121))
    await user.type(screen.getByLabelText('Points'), '2.5')
    await user.type(screen.getByLabelText('Latitude'), '95')
    await user.click(screen.getByRole('button', { name: 'Create attraction' }))

    expect(screen.getByText('Some fields need attention.')).toBeInTheDocument()
    expect(language('Romanian').getByLabelText('Name')).toHaveAccessibleDescription(
      'Fill in this field.',
    )
    expect(language('English').getByLabelText('Name')).toHaveAccessibleDescription(
      'Use at most 120 characters.',
    )
    expect(screen.getByLabelText('Points')).toHaveAccessibleDescription(
      'Enter a whole number greater than 0.',
    )
    expect(screen.getByLabelText('Latitude')).toHaveAccessibleDescription(
      'Enter a latitude between -90 and 90.',
    )
    expect(screen.getByLabelText('Longitude')).toHaveAccessibleDescription('Fill in this field.')
    expect(requestsTo(fetchMock, 'POST', '/admin/attractions')).toHaveLength(0)
  })

  it('creates the attraction with the right body and opens it', async () => {
    const created = makeAttraction({ id: 'new-1' })
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'POST /admin/attractions': reply.json(created, 201),
      'GET /admin/attractions/new-1': reply.json(created),
    })
    renderNewAttraction()
    await screen.findByRole('heading', { name: 'New attraction' })

    await fillValidForm()
    await user.click(screen.getByRole('button', { name: 'Create attraction' }))

    expect(await screen.findByText('The attraction was saved.')).toBeInTheDocument()
    const [request] = requestsTo(fetchMock, 'POST', '/admin/attractions')
    expect(new Headers(request?.headers).get('Content-Type')).toBe('application/json')
    expect(JSON.parse(String(request?.body))).toEqual({
      location: { lat: 45.359987, lon: 25.5428 },
      pointValue: 25,
      visitMinutes: 15,
      translations: {
        ro: {
          name: 'Cascada Urlătoarea',
          description: 'Cascadă de 15 m.',
          history: 'Cunoscută din 1900.',
          sources: null,
        },
        en: {
          name: 'Urlătoarea Waterfall',
          description: 'A 15 m waterfall.',
          history: null,
          sources: null,
        },
      },
    })
    expect(screen.getByRole('heading', { name: 'Attraction: Castelul Peleș' })).toBeInTheDocument()
  })

  it('shows the field errors of the server next to the fields', async () => {
    mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      'POST /admin/attractions': reply.problem(400, 'Invalid request content.', [
        { field: 'translations.en.name', message: 'must be unique' },
        { field: 'translations[ro].description', message: 'size must be between 1 and 4000' },
        { field: 'dangerRadiusM', message: 'must not be null' },
      ]),
    })
    renderNewAttraction()
    await screen.findByRole('heading', { name: 'New attraction' })

    await fillValidForm()
    await user.click(screen.getByRole('button', { name: 'Create attraction' }))

    expect(await screen.findByText('Server message: Invalid request content.')).toBeInTheDocument()
    expect(
      screen.getByText('The server did not accept the data. Check the marked fields.'),
    ).toBeInTheDocument()
    expect(language('English').getByLabelText('Name')).toHaveAccessibleDescription(
      'must be unique',
    )
    expect(language('Romanian').getByLabelText('Description')).toHaveAccessibleDescription(
      'size must be between 1 and 4000',
    )
    // A field the form does not have is listed with the general error.
    expect(screen.getByText('dangerRadiusM')).toBeInTheDocument()

    await user.type(language('English').getByLabelText('Name'), ' 2')
    expect(language('English').getByLabelText('Name')).not.toHaveAccessibleDescription()
  })
})
