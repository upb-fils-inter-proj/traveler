import { User } from 'oidc-client-ts'
import type { AuthContextProps } from 'react-oidc-context'
import { vi } from 'vitest'

/** Values for the settings that the tests of the config module share. */
export const validEnv = {
  VITE_OIDC_AUTHORITY: 'https://idp.example.test/application/o/admin/',
  VITE_OIDC_CLIENT_ID: 'test-client-id',
  VITE_OIDC_REDIRECT_URI: 'http://localhost:5173/auth/callback',
  VITE_OIDC_POST_LOGOUT_REDIRECT_URI: 'http://localhost:5173/login',
  VITE_OIDC_SCOPE: 'openid profile email',
}

export function makeUser(
  profile: Record<string, unknown> = {},
  userState?: unknown,
): User {
  return new User({
    access_token: 'test-access-token',
    token_type: 'Bearer',
    profile: {
      sub: 'user-1',
      iss: 'https://idp.example.test/',
      aud: 'test-client-id',
      exp: 4_000_000_000,
      iat: 1_700_000_000,
      ...profile,
    },
    expires_at: 4_000_000_000,
    userState,
  })
}

/** A stand-in for what `useAuth()` returns, signed out unless told otherwise. */
export function mockAuth(overrides: Partial<AuthContextProps> = {}): AuthContextProps {
  return {
    isLoading: false,
    isAuthenticated: false,
    signinRedirect: vi.fn().mockResolvedValue(undefined),
    signoutRedirect: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  } as unknown as AuthContextProps
}
