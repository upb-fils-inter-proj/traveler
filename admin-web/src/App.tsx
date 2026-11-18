import { useMemo } from 'react'
import { AuthProvider } from 'react-oidc-context'
import { BrowserRouter } from 'react-router'
import AppRoutes from './AppRoutes.tsx'
import ConfigErrorPage from './auth/ConfigErrorPage.tsx'
import { loadAuthConfig, toUserManagerSettings } from './config.ts'

/** Removes the code and state from the address bar after sign-in. */
function onSigninCallback() {
  window.history.replaceState({}, document.title, window.location.pathname)
}

interface AppProps {
  /** Where the configuration comes from. Tests pass their own. */
  env?: Readonly<Record<string, unknown>>
}

export default function App({ env = import.meta.env }: AppProps) {
  const setup = useMemo(() => {
    const result = loadAuthConfig(env)
    return result.ok
      ? { ok: true as const, settings: toUserManagerSettings(result.config) }
      : result
  }, [env])

  if (!setup.ok) {
    return <ConfigErrorPage problems={setup.problems} />
  }

  return (
    <AuthProvider {...setup.settings} onSigninCallback={onSigninCallback}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
