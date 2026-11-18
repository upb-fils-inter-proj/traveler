import { useAuth } from 'react-oidc-context'
import { Navigate, useLocation } from 'react-router'
import LoadingScreen from './LoadingScreen.tsx'
import { returnToFromLocationState } from './returnTo.ts'

export default function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const returnTo = returnToFromLocationState(location.state)

  if (auth.isLoading) {
    return <LoadingScreen />
  }
  if (auth.isAuthenticated) {
    return <Navigate to={returnTo} replace />
  }

  const signIn = () => {
    // The library reports failures through `auth.error`, it does not throw.
    void auth.signinRedirect({ state: { returnTo } })
  }

  return (
    <main className="screen">
      <h1>Traveler Admin</h1>
      <p>Sign in with your authentik account to continue.</p>
      {auth.error?.source.startsWith('signin') && (
        <p role="alert" className="error">
          Sign-in failed. Try again.
        </p>
      )}
      <button type="button" onClick={signIn}>
        Sign in
      </button>
    </main>
  )
}
