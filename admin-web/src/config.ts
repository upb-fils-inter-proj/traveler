import { type UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts'
import { CALLBACK_PATH } from './paths.ts'

/** Names of the Vite environment variables that configure the sign-in. */
export const ENV = {
  authority: 'VITE_OIDC_AUTHORITY',
  clientId: 'VITE_OIDC_CLIENT_ID',
  redirectUri: 'VITE_OIDC_REDIRECT_URI',
  postLogoutRedirectUri: 'VITE_OIDC_POST_LOGOUT_REDIRECT_URI',
  scope: 'VITE_OIDC_SCOPE',
} as const

export interface AuthConfig {
  authority: string
  clientId: string
  redirectUri: string
  postLogoutRedirectUri: string
  scope: string
}

export interface ConfigProblem {
  /** Name of the environment variable. */
  variable: string
  reason: 'missing' | 'invalid'
}

export type ConfigResult =
  | { ok: true; config: AuthConfig }
  | { ok: false; problems: ConfigProblem[] }

function parseHttpUrl(value: string): URL | null {
  try {
    const url = new URL(value)
    return url.protocol === 'https:' || url.protocol === 'http:' ? url : null
  } catch {
    return null
  }
}

/**
 * Reads and validates the sign-in configuration. It never throws: problems
 * are returned so the app can show them instead of crashing.
 */
export function loadAuthConfig(env: Readonly<Record<string, unknown>>): ConfigResult {
  const problems: ConfigProblem[] = []

  const read = (variable: string): string | null => {
    const raw = env[variable]
    const value = typeof raw === 'string' ? raw.trim() : ''
    if (value === '') {
      problems.push({ variable, reason: 'missing' })
      return null
    }
    return value
  }

  const invalid = (variable: string) => {
    problems.push({ variable, reason: 'invalid' })
  }

  const authority = read(ENV.authority)
  const clientId = read(ENV.clientId)
  const redirectUri = read(ENV.redirectUri)
  const postLogoutRedirectUri = read(ENV.postLogoutRedirectUri)
  const scope = read(ENV.scope)

  if (authority !== null && parseHttpUrl(authority) === null) {
    invalid(ENV.authority)
  }
  if (redirectUri !== null) {
    // The router only handles the sign-in response on CALLBACK_PATH, so any
    // other path would leave the user stuck at the login screen.
    const url = parseHttpUrl(redirectUri)
    if (url === null || url.pathname !== CALLBACK_PATH) {
      invalid(ENV.redirectUri)
    }
  }
  if (postLogoutRedirectUri !== null && parseHttpUrl(postLogoutRedirectUri) === null) {
    invalid(ENV.postLogoutRedirectUri)
  }

  if (
    problems.length > 0 ||
    authority === null ||
    clientId === null ||
    redirectUri === null ||
    postLogoutRedirectUri === null ||
    scope === null
  ) {
    return { ok: false, problems }
  }

  return {
    ok: true,
    config: { authority, clientId, redirectUri, postLogoutRedirectUri, scope },
  }
}

/**
 * Settings for the authorization code flow with PKCE. oidc-client-ts sends a
 * S256 code challenge for the code flow unless `disablePKCE` is set, which we
 * never do.
 *
 * Everything the library persists (the signed-in user with its tokens, and the
 * in-flight request state with the PKCE code verifier) goes to sessionStorage.
 * The library would keep the request state in localStorage by default.
 */
export function toUserManagerSettings(config: AuthConfig): UserManagerSettings {
  return {
    authority: config.authority,
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    post_logout_redirect_uri: config.postLogoutRedirectUri,
    scope: config.scope,
    response_type: 'code',
    // Silent renewal needs extra setup (refresh tokens or an iframe) that
    // is out of scope for the skeleton: the user signs in again instead.
    automaticSilentRenew: false,
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
  }
}
