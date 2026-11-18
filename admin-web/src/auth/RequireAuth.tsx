import { useAuth } from 'react-oidc-context'
import { Navigate, Outlet, useLocation } from 'react-router'
import { LOGIN_PATH } from '../paths.ts'
import LoadingScreen from './LoadingScreen.tsx'

/**
 * Layout route that only renders its children for a signed-in user. Everyone
 * else is sent to the login screen, which remembers where they wanted to go.
 */
export default function RequireAuth() {
  const auth = useAuth()
  const location = useLocation()

  if (auth.isLoading) {
    return <LoadingScreen />
  }
  if (!auth.isAuthenticated) {
    return <Navigate to={LOGIN_PATH} replace state={{ from: location }} />
  }
  return <Outlet />
}
