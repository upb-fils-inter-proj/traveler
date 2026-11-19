import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from '../AppRoutes.tsx'
import { CITY_ADMIN, makeAttraction, mockFetch, reply, requestsTo } from '../test/api.ts'
import { makeUser, mockAuth } from '../test/auth.ts'
import { convertToWebp, saveFile } from './files.ts'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))
vi.mock('./LocationMap.tsx', () => ({ default: () => <p>map</p> }))
// Canvas and WebP encoding need a real browser.
vi.mock('./files.ts', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./files.ts')>()),
  convertToWebp: vi.fn(),
  saveFile: vi.fn(),
}))

const user = userEvent.setup()
const ATTRACTION_URL = '/admin/attractions/a1'
const pelesCastle = makeAttraction()

function renderAttraction() {
  return render(
    <MemoryRouter initialEntries={['/city-administrator/attractions/a1']}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

async function opened() {
  await screen.findByRole('heading', { name: 'Attraction: Castelul Peleș' })
}

function bodyOf(init: RequestInit | undefined): unknown {
  return JSON.parse(String(init?.body))
}

describe('EditAttractionPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue(mockAuth({ isAuthenticated: true, user: makeUser() }))
  })

  it('saves the changed fields', async () => {
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`PUT ${ATTRACTION_URL}`]: reply.json({ ...pelesCastle, pointValue: 30, version: 43 }),
    })
    renderAttraction()
    await opened()

    await user.clear(screen.getByLabelText('Points'))
    await user.type(screen.getByLabelText('Points'), '30')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('The attraction was saved.')).toBeInTheDocument()
    expect(bodyOf(requestsTo(fetchMock, 'PUT', ATTRACTION_URL)[0])).toMatchObject({
      location: { lat: 45.36009, lon: 25.54273 },
      pointValue: 30,
      visitMinutes: 15,
    })
  })

  it('closes and reopens the attraction', async () => {
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`PUT ${ATTRACTION_URL}/status`]: reply.json({ ...pelesCastle, status: 'closed' }),
    })
    renderAttraction()
    await opened()

    await user.click(screen.getByRole('button', { name: 'Close attraction' }))

    expect(await screen.findByText('The status was changed to Closed.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reopen attraction' })).toBeInTheDocument()
    expect(bodyOf(requestsTo(fetchMock, 'PUT', `${ATTRACTION_URL}/status`)[0])).toEqual({
      status: 'closed',
    })
  })

  it('asks before removing and says that removal is final', async () => {
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`PUT ${ATTRACTION_URL}/status`]: reply.json({ ...pelesCastle, status: 'removed' }),
    })
    renderAttraction()
    await opened()

    await user.click(screen.getByRole('button', { name: 'Remove attraction' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Remove this attraction?' })
    expect(dialog).toHaveAccessibleDescription(/Removing is final/)
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(requestsTo(fetchMock, 'PUT', `${ATTRACTION_URL}/status`)).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: 'Remove attraction' }))
    await user.click(screen.getByRole('button', { name: 'Remove for good' }))

    expect(await screen.findByText(/This attraction was removed/)).toBeInTheDocument()
    expect(bodyOf(requestsTo(fetchMock, 'PUT', `${ATTRACTION_URL}/status`)[0])).toEqual({
      status: 'removed',
    })
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remove attraction' })).not.toBeInTheDocument()
  })

  it('warns that printed codes stop working before generating a new QR code', async () => {
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`POST ${ATTRACTION_URL}/qr-code/regenerate`]: reply.json({ ...pelesCastle, qrVersion: 2 }),
    })
    renderAttraction()
    await opened()
    expect(screen.getByText('Current QR code version: 1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Generate new QR code' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Generate a new QR code?' })
    expect(dialog).toHaveAccessibleDescription(/will stop working/)
    await user.click(within(dialog).getByRole('button', { name: 'Generate new code' }))

    expect(await screen.findByText('Current QR code version: 2')).toBeInTheDocument()
    expect(requestsTo(fetchMock, 'POST', `${ATTRACTION_URL}/qr-code/regenerate`)).toHaveLength(1)
  })

  it('downloads the QR code PDF', async () => {
    mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`GET ${ATTRACTION_URL}/qr-code.pdf`]: () =>
        new Response(new Blob(['%PDF-1.7'], { type: 'application/pdf' }), {
          headers: { 'Content-Disposition': 'attachment; filename="qr-a1-v1.pdf"' },
        }),
    })
    renderAttraction()
    await opened()

    await user.click(screen.getByRole('button', { name: 'Print QR code' }))

    await vi.waitFor(() => expect(saveFile).toHaveBeenCalledWith(expect.any(Blob), 'qr-a1-v1.pdf'))
  })

  it('uploads the converted photo and shows it', async () => {
    const webp = new Blob(['webp bytes'], { type: 'image/webp' })
    vi.mocked(convertToWebp).mockResolvedValue(webp)
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: () => 'blob:photo' })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
    const fetchMock = mockFetch({
      'GET /admin/me': reply.json(CITY_ADMIN),
      [`GET ${ATTRACTION_URL}`]: reply.json(pelesCastle),
      [`PUT ${ATTRACTION_URL}/photo`]: reply.json({ ...pelesCastle, hasPhoto: true, version: 43 }),
      [`GET ${ATTRACTION_URL}/photo`]: () => new Response(webp),
    })
    renderAttraction()
    await opened()
    expect(screen.getByText('There is no photo yet.')).toBeInTheDocument()

    const photo = new File(['jpeg bytes'], 'peles.jpg', { type: 'image/jpeg' })
    await user.upload(screen.getByLabelText('Upload a photo'), photo)

    expect(await screen.findByRole('img', { name: 'Photo of Peleș Castle' })).toHaveAttribute(
      'src',
      'blob:photo',
    )
    expect(convertToWebp).toHaveBeenCalledWith(photo)
    const [upload] = requestsTo(fetchMock, 'PUT', `${ATTRACTION_URL}/photo`)
    expect(new Headers(upload?.headers).get('Content-Type')).toBe('image/webp')
    expect(upload?.body).toBe(webp)
    const [download] = requestsTo(fetchMock, 'GET', `${ATTRACTION_URL}/photo`)
    expect(new Headers(download?.headers).get('Authorization')).toBe('Bearer test-access-token')
  })
})
