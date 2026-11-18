import { useAuth } from 'react-oidc-context'
import { displayName } from './displayName.ts'

export default function UserInfo() {
  const auth = useAuth()

  const signOut = () => {
    // The library removes the user before it redirects, so even if the
    // redirect fails the user is signed out here and lands on the login screen.
    void auth.signoutRedirect()
  }

  return (
    <div className="user-info">
      <span>Signed in as {displayName(auth.user)}</span>
      <button type="button" onClick={signOut}>
        Sign out
      </button>
    </div>
  )
}
