import { describe, expect, it, vi } from 'vitest'
import { mockFetch, reply } from '../test/api.ts'
import { fileNameFrom } from './attractions.ts'
import { ApiError, createApiClient, problemToError } from './client.ts'

describe('createApiClient', () => {
  it('calls the same origin with the access token', async () => {
    const fetchMock = mockFetch({ 'GET /admin/me': reply.json({ email: 'a@b.c', role: 'city_admin' }) })

    const me = await createApiClient(() => 'token-1').json('/admin/me')

    expect(me).toEqual({ email: 'a@b.c', role: 'city_admin' })
    const [url, init] = fetchMock.mock.calls[0] ?? []
    expect(url).toBe('/admin/me')
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer token-1')
  })

  it('turns a ProblemDetail answer into an ApiError with the detail and field errors', async () => {
    mockFetch({
      'POST /admin/attractions': reply.problem(400, 'Validation failed', [
        { field: 'translations.ro.name', message: 'must not be blank' },
      ]),
    })

    const error = await createApiClient(() => 'token')
      .send('/admin/attractions', { method: 'POST' })
      .catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      detail: 'Validation failed',
      fieldErrors: [{ field: 'translations.ro.name', message: 'must not be blank' }],
    })
  })

  it('reports a server that does not answer as status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(createApiClient(() => 'token').send('/admin/me')).rejects.toMatchObject({
      status: 0,
    })
  })
})

describe('problemToError', () => {
  it('ignores a body that is not a ProblemDetail', () => {
    expect(problemToError(502, 'Bad gateway')).toMatchObject({
      status: 502,
      detail: null,
      fieldErrors: [],
    })
  })
})

describe('fileNameFrom', () => {
  it('reads the file name of the QR code PDF', () => {
    expect(fileNameFrom('attachment; filename="qr-a1-v3.pdf"')).toBe('qr-a1-v3.pdf')
    expect(fileNameFrom(null)).toBeNull()
  })
})
