import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router'
import ApiErrorMessage from '../api/ApiErrorMessage.tsx'
import { canManageAttractions, fetchMe } from '../api/attractions.ts'
import { useApiClient, useResource } from '../api/hooks.ts'

/**
 * Layout route that asks the admin backend who the user is and only renders
 * the attraction screens for City Administrators and Traveler Administration.
 */
export default function RequireAttractionAccess() {
  const { t } = useTranslation()
  const api = useApiClient()
  const [me, , reload] = useResource('me', (signal) => fetchMe(api, signal))

  if (me.status === 'loading') {
    return <p role="status">{t('access.checking')}</p>
  }
  if (me.status === 'error' && me.error.status !== 403) {
    return <ApiErrorMessage error={me.error} onRetry={reload} />
  }
  if (me.status === 'error' || !canManageAttractions(me.data.role)) {
    return (
      <div className="notice" role="alert">
        <p className="notice-title">{t('access.deniedTitle')}</p>
        <p>{t('access.denied')}</p>
      </div>
    )
  }
  return <Outlet />
}
