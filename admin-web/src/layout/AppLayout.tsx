import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router'
import UserInfo from '../auth/UserInfo.tsx'
import LanguageSwitcher from '../i18n/LanguageSwitcher.tsx'
import SideMenu from './SideMenu.tsx'

/** Header, side menu and the page of the current section. */
export default function AppLayout() {
  const { t } = useTranslation()

  return (
    <div className="app-layout">
      <header className="app-header">
        <span className="app-name">{t('app.name')}</span>
        <div className="app-header-actions">
          <LanguageSwitcher />
          <UserInfo />
        </div>
      </header>
      <SideMenu />
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  )
}
