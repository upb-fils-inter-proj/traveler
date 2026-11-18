import { OidcClient } from 'oidc-client-ts'
import { describe, expect, it } from 'vitest'
import { ENV, loadAuthConfig, toUserManagerSettings } from './config.ts'
import { validEnv } from './test/auth.ts'

describe('loadAuthConfig', () => {
  it('returns the configuration when every variable is set', () => {
    expect(loadAuthConfig(validEnv)).toEqual({
      ok: true,
      config: {
        authority: validEnv.VITE_OIDC_AUTHORITY,
        clientId: validEnv.VITE_OIDC_CLIENT_ID,
        redirectUri: validEnv.VITE_OIDC_REDIRECT_URI,
        postLogoutRedirectUri: validEnv.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
        scope: validEnv.VITE_OIDC_SCOPE,
      },
    })
  })

  it('reports every variable when nothing is set', () => {
    const result = loadAuthConfig({})
    expect(result).toEqual({
      ok: false,
      problems: Object.values(ENV).map((variable) => ({
        variable,
        reason: 'missing',
      })),
    })
  })

  it('treats blank and non-string values as missing', () => {
    const result = loadAuthConfig({
      ...validEnv,
      VITE_OIDC_CLIENT_ID: '   ',
      VITE_OIDC_SCOPE: undefined,
    })
    expect(result).toEqual({
      ok: false,
      problems: [
        { variable: ENV.clientId, reason: 'missing' },
        { variable: ENV.scope, reason: 'missing' },
      ],
    })
  })

  it('rejects URLs that are not http or https', () => {
    const result = loadAuthConfig({
      ...validEnv,
      VITE_OIDC_AUTHORITY: 'not a url',
      VITE_OIDC_POST_LOGOUT_REDIRECT_URI: 'javascript:alert(1)',
    })
    expect(result).toEqual({
      ok: false,
      problems: [
        { variable: ENV.authority, reason: 'invalid' },
        { variable: ENV.postLogoutRedirectUri, reason: 'invalid' },
      ],
    })
  })

  it('rejects a redirect URI that is not the callback route', () => {
    const result = loadAuthConfig({
      ...validEnv,
      VITE_OIDC_REDIRECT_URI: 'http://localhost:5173/somewhere-else',
    })
    expect(result).toEqual({
      ok: false,
      problems: [{ variable: ENV.redirectUri, reason: 'invalid' }],
    })
  })
})

describe('toUserManagerSettings', () => {
  const result = loadAuthConfig(validEnv)
  if (!result.ok) throw new Error('the test configuration must be valid')
  const settings = toUserManagerSettings(result.config)

  it('maps the configuration to the authorization code flow', () => {
    expect(settings).toMatchObject({
      authority: validEnv.VITE_OIDC_AUTHORITY,
      client_id: validEnv.VITE_OIDC_CLIENT_ID,
      redirect_uri: validEnv.VITE_OIDC_REDIRECT_URI,
      post_logout_redirect_uri: validEnv.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
      scope: validEnv.VITE_OIDC_SCOPE,
      response_type: 'code',
    })
  })

  it('builds an authorization request with a PKCE challenge', async () => {
    const client = new OidcClient({
      ...settings,
      // Static metadata so the test does not need a server.
      metadata: {
        issuer: validEnv.VITE_OIDC_AUTHORITY,
        authorization_endpoint: 'https://idp.example.test/authorize/',
      },
    })

    const request = await client.createSigninRequest({})
    const url = new URL(request.url)

    expect(url.searchParams.get('response_type')).toBe('code')
    expect(url.searchParams.get('client_id')).toBe(validEnv.VITE_OIDC_CLIENT_ID)
    expect(url.searchParams.get('code_challenge')).toBeTruthy()
    expect(url.searchParams.get('code_challenge_method')).toBe('S256')
    expect(url.searchParams.get('code_verifier')).toBeNull()
  })

  it('keeps the request state out of localStorage', async () => {
    window.localStorage.clear()
    window.sessionStorage.clear()
    const client = new OidcClient({
      ...settings,
      metadata: {
        issuer: validEnv.VITE_OIDC_AUTHORITY,
        authorization_endpoint: 'https://idp.example.test/authorize/',
      },
    })

    await client.createSigninRequest({})

    expect(window.localStorage.length).toBe(0)
    expect(window.sessionStorage.length).toBeGreaterThan(0)
  })
})
