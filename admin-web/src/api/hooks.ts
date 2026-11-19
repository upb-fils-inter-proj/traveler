import { useCallback, useEffect, useEffectEvent, useMemo, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { type ApiClient, ApiError, createApiClient, toApiError } from './client.ts'

/** An API client that sends the signed-in user's access token. */
export function useApiClient(): ApiClient {
  const auth = useAuth()
  const token = auth.user?.access_token
  return useMemo(() => createApiClient(() => token), [token])
}

export type Resource<T> =
  | { status: 'loading' }
  | { status: 'ok'; data: T }
  | { status: 'error'; error: ApiError }

interface Loaded<T> {
  key: string
  attempt: number
  result: Resource<T>
}

const LOADING = { status: 'loading' } as const

/**
 * Loads data when `key` changes (or on reload) and cancels the request that
 * is no longer wanted. Returns the state, a setter for data the caller got
 * from a later request, and a reload function.
 */
export function useResource<T>(
  key: string,
  load: (signal: AbortSignal) => Promise<T>,
): [Resource<T>, (data: T) => void, () => void] {
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<Loaded<T> | null>(null)
  const runLoad = useEffectEvent(load)

  useEffect(() => {
    const controller = new AbortController()
    const finish = (result: Resource<T>) => {
      if (!controller.signal.aborted) setLoaded({ key, attempt, result })
    }
    runLoad(controller.signal).then(
      (data) => finish({ status: 'ok', data }),
      (error: unknown) => finish({ status: 'error', error: toApiError(error) }),
    )
    return () => controller.abort()
  }, [key, attempt])

  const current =
    loaded !== null && loaded.key === key && loaded.attempt === attempt ? loaded.result : LOADING

  const setData = useCallback(
    (data: T) => setLoaded({ key, attempt, result: { status: 'ok', data } }),
    [key, attempt],
  )
  const reload = useCallback(() => setAttempt((value) => value + 1), [])

  return [current, setData, reload]
}
