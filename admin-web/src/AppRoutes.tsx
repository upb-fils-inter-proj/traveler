import { Navigate, Route, Routes } from 'react-router'
import AttractionListPage from './attractions/AttractionListPage.tsx'
import EditAttractionPage from './attractions/EditAttractionPage.tsx'
import NewAttractionPage from './attractions/NewAttractionPage.tsx'
import CallbackPage from './auth/CallbackPage.tsx'
import LoginPage from './auth/LoginPage.tsx'
import RequireAttractionAccess from './auth/RequireAttractionAccess.tsx'
import RequireAuth from './auth/RequireAuth.tsx'
import AppLayout from './layout/AppLayout.tsx'
import { CALLBACK_PATH, LOGIN_PATH } from './paths.ts'
import SectionPage from './sections/SectionPage.tsx'
import { CITY_ADMINISTRATOR_PATH, DEFAULT_SECTION_PATH, SECTIONS } from './sections.ts'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path={LOGIN_PATH} element={<LoginPage />} />
      <Route path={CALLBACK_PATH} element={<CallbackPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to={DEFAULT_SECTION_PATH} replace />} />
          {SECTIONS.map((section) =>
            section.path === CITY_ADMINISTRATOR_PATH ? (
              <Route
                key={section.path}
                path={section.path}
                element={
                  <SectionPage titleKey={section.titleKey}>
                    <RequireAttractionAccess />
                  </SectionPage>
                }
              >
                <Route index element={<AttractionListPage />} />
                <Route path="attractions/new" element={<NewAttractionPage />} />
                <Route path="attractions/:id" element={<EditAttractionPage />} />
              </Route>
            ) : (
              <Route
                key={section.path}
                path={section.path}
                element={<SectionPage titleKey={section.titleKey} />}
              />
            ),
          )}
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
