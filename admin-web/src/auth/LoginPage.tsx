import { useTranslation } from 'react-i18next'
import { useAuth } from 'react-oidc-context'
import { Navigate, useLocation } from 'react-router'
import LanguageSwitcher from '../i18n/LanguageSwitcher.tsx'
import LoadingScreen from './LoadingScreen.tsx'
import { returnToFromLocationState } from './returnTo.ts'

export default function LoginPage() {
  const { t } = useTranslation()
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
      <LanguageSwitcher />
      <h1>{t('app.name')}</h1>
      <p>{t('login.description')}</p>
      {auth.error?.source.startsWith('signin') && (
        <p role="alert" className="error">
          {t('login.error')}
        </p>
      )}
      <button type="button" onClick={signIn}>
        {t('login.button')}
      </button>
    </main>
  )
}
