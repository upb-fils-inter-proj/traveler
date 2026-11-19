/** One problem with a field of the request, as the backend reports it. */
export interface FieldError {
  field: string
  message: string
}

interface Problem {
  title?: string | null
  detail?: string | null
  fieldErrors?: FieldError[]
}

/**
 * A request that failed: the HTTP status and what the ProblemDetail body
 * says. Status 0 means the server did not answer at all.
 */
export class ApiError extends Error {
  readonly status: number
  readonly title: string | null
  readonly detail: string | null
  readonly fieldErrors: readonly FieldError[]

  constructor(status: number, problem: Problem = {}) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.title = problem.title ?? null
    this.detail = problem.detail ?? null
    this.fieldErrors = problem.fieldErrors ?? []
  }
}

export function toApiError(error: unknown): ApiError {
  return error instanceof ApiError ? error : new ApiError(0)
}

function text(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null
}

/** Reads a Spring ProblemDetail body, ignoring whatever does not fit. */
export function problemToError(status: number, body: unknown): ApiError {
  if (typeof body !== 'object' || body === null) return new ApiError(status)
  const problem = body as Record<string, unknown>
  const errors = Array.isArray(problem.errors) ? problem.errors : []
  const fieldErrors = errors.flatMap((item: unknown): FieldError[] => {
    if (typeof item !== 'object' || item === null) return []
    const { field, message } = item as Record<string, unknown>
    return typeof field === 'string' && typeof message === 'string'
      ? [{ field, message }]
      : []
  })
  return new ApiError(status, {
    title: text(problem.title),
    detail: text(problem.detail),
    fieldErrors,
  })
}

async function readError(response: Response): Promise<ApiError> {
  let body: unknown = null
  if ((response.headers.get('Content-Type') ?? '').includes('json')) {
    try {
      body = await response.json()
    } catch {
      // Not JSON after all: the status alone has to do.
    }
  }
  return problemToError(response.status, body)
}

export interface ApiClient {
  /** Resolves with the response when it is successful, rejects with an ApiError otherwise. */
  send(path: string, init?: RequestInit): Promise<Response>
  /** Like send, and reads the JSON body. */
  json<T>(path: string, init?: RequestInit): Promise<T>
}

/**
 * Calls the admin API on the app's own origin with the user's access token.
 * The token is read on every request, so a renewed token is used at once.
 */
export function createApiClient(getAccessToken: () => string | undefined): ApiClient {
  async function send(path: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers)
    const token = getAccessToken()
    if (token !== undefined && token !== '') {
      headers.set('Authorization', `Bearer ${token}`)
    }

    let response: Response
    try {
      response = await fetch(path, { ...init, headers })
    } catch (error) {
      if (init.signal?.aborted) throw error
      throw new ApiError(0)
    }
    if (!response.ok) throw await readError(response)
    return response
  }

  return {
    send,
    async json<T>(path: string, init?: RequestInit): Promise<T> {
      const response = await send(path, init)
      return (await response.json()) as T
    },
  }
}

/** Request options for a JSON body. */
export function jsonBody(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }
}
