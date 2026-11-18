import { Navigate, Route, Routes } from 'react-router'
import CallbackPage from './auth/CallbackPage.tsx'
import LoginPage from './auth/LoginPage.tsx'
import RequireAuth from './auth/RequireAuth.tsx'
import AppLayout from './layout/AppLayout.tsx'
import { CALLBACK_PATH, LOGIN_PATH } from './paths.ts'
import SectionPage from './sections/SectionPage.tsx'
import { DEFAULT_SECTION_PATH, SECTIONS } from './sections.ts'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path={LOGIN_PATH} element={<LoginPage />} />
      <Route path={CALLBACK_PATH} element={<CallbackPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to={DEFAULT_SECTION_PATH} replace />} />
          {SECTIONS.map((section) => (
            <Route
              key={section.path}
              path={section.path}
              element={<SectionPage titleKey={section.titleKey} />}
            />
          ))}
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
