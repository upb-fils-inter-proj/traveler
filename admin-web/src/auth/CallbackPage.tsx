import { useTranslation } from 'react-i18next'
import { useAuth } from 'react-oidc-context'
import { Link, Navigate } from 'react-router'
import { LOGIN_PATH } from '../paths.ts'
import LoadingScreen from './LoadingScreen.tsx'
import { returnToFromUserState } from './returnTo.ts'

/**
 * The identity provider sends the browser here. AuthProvider exchanges the
 * code for tokens by itself, this page only reacts to the outcome.
 */
export default function CallbackPage() {
  const { t } = useTranslation()
  const auth = useAuth()

  if (auth.isLoading) {
    return <LoadingScreen message={t('callback.completing')} />
  }
  if (auth.error) {
    return (
      <main className="screen">
        <h1>{t('app.name')}</h1>
        <p role="alert" className="error">
          {t('callback.error')}
        </p>
        <Link to={LOGIN_PATH}>{t('callback.backToLogin')}</Link>
      </main>
    )
  }
  if (auth.isAuthenticated) {
    return <Navigate to={returnToFromUserState(auth.user?.state)} replace />
  }
  return <Navigate to={LOGIN_PATH} replace />
}
