import { vi } from 'vitest'
import type { Attraction } from '../api/attractions.ts'

type Reply = () => Response | Promise<Response>

export const reply = {
  json:
    (body: unknown, status = 200): Reply =>
    () =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
  problem:
    (status: number, detail?: string, errors?: { field: string; message: string }[]): Reply =>
    () =>
      new Response(JSON.stringify({ type: 'about:blank', title: 'Error', status, detail, errors }), {
        status,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
}

/**
 * Replaces fetch with a table of answers keyed by "METHOD url". A request
 * that is not in the table gets a 500 that names it, so the test fails.
 */
export function mockFetch(routes: Record<string, Reply>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const key = `${(init.method ?? 'GET').toUpperCase()} ${String(input)}`
    const answer = routes[key]
    return answer ? answer() : reply.problem(500, `Unexpected request ${key}`)()
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** The requests made with the given method and URL, with their options. */
export function requestsTo(
  fetchMock: ReturnType<typeof mockFetch>,
  method: string,
  url: string,
): RequestInit[] {
  return fetchMock.mock.calls
    .filter(([input, init]) => String(input) === url && (init?.method ?? 'GET') === method)
    .map(([, init]) => init ?? {})
}

export function makeAttraction(overrides: Partial<Attraction> = {}): Attraction {
  return {
    id: 'a1',
    status: 'open',
    location: { lat: 45.36009, lon: 25.54273 },
    pointValue: 10,
    visitMinutes: 15,
    dangerRadiusM: 100,
    dangerFlag: false,
    dangerMessage: null,
    qrVersion: 1,
    hasPhoto: false,
    version: 42,
    translations: {
      ro: { name: 'Castelul Peleș', description: 'Fostă reședință regală.', history: null, sources: null },
      en: { name: 'Peleș Castle', description: 'Former royal residence.', history: null, sources: null },
    },
    ...overrides,
  }
}

export const CITY_ADMIN = { email: 'ana@example.test', role: 'city_admin' }
