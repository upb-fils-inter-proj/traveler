import { useTranslation } from 'react-i18next'
import { useAuth } from 'react-oidc-context'
import { displayName } from './displayName.ts'

export default function UserInfo() {
  const { t } = useTranslation()
  const auth = useAuth()

  const signOut = () => {
    // The library removes the user before it redirects, so even if the
    // redirect fails the user is signed out here and lands on the login screen.
    void auth.signoutRedirect()
  }

  return (
    <div className="user-info">
      <span>{t('user.signedInAs', { name: displayName(auth.user) })}</span>
      <button type="button" onClick={signOut}>
        {t('user.signOut')}
      </button>
    </div>
  )
}
