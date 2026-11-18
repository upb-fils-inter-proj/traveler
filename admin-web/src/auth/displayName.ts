import type { User } from 'oidc-client-ts'

/** The best available label for a signed-in user. */
export function displayName(user: User | null | undefined): string {
  const profile = user?.profile
  if (!profile) return ''
  for (const claim of [profile.name, profile.preferred_username, profile.email]) {
    if (typeof claim === 'string' && claim.trim() !== '') return claim
  }
  return profile.sub
}
