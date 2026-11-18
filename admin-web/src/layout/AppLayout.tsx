import { Outlet } from 'react-router'
import UserInfo from '../auth/UserInfo.tsx'
import SideMenu from './SideMenu.tsx'

/** Header, side menu and the page of the current section. */
export default function AppLayout() {
  return (
    <div className="app-layout">
      <header className="app-header">
        <span className="app-name">Traveler Admin</span>
        <UserInfo />
      </header>
      <SideMenu />
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  )
}
