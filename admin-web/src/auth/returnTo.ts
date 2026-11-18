/** Where to send the user after signing in when nothing else is known. */
export const DEFAULT_RETURN_TO = '/'

/**
 * Accepts only paths inside this app. Anything else (another origin, a
 * protocol-relative URL, a non-string) falls back to the default.
 */
export function safeReturnTo(value: unknown): string {
  if (typeof value !== 'string') return DEFAULT_RETURN_TO
  if (!value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return DEFAULT_RETURN_TO
  }
  return value
}

/** Reads the `from` location that the guard puts in the router state. */
export function returnToFromLocationState(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('from' in state)) {
    return DEFAULT_RETURN_TO
  }
  const from = state.from
  if (typeof from !== 'object' || from === null) return DEFAULT_RETURN_TO
  const { pathname, search, hash } = from as {
    pathname?: unknown
    search?: unknown
    hash?: unknown
  }
  if (typeof pathname !== 'string') return DEFAULT_RETURN_TO
  const query = typeof search === 'string' ? search : ''
  const fragment = typeof hash === 'string' ? hash : ''
  return safeReturnTo(`${pathname}${query}${fragment}`)
}

/** Reads the `returnTo` path that the login screen stored in the request state. */
export function returnToFromUserState(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('returnTo' in state)) {
    return DEFAULT_RETURN_TO
  }
  return safeReturnTo(state.returnTo)
}
