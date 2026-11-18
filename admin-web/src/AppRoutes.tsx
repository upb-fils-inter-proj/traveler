import { Navigate, Route, Routes } from 'react-router'
import CallbackPage from './auth/CallbackPage.tsx'
import LoginPage from './auth/LoginPage.tsx'
import RequireAuth from './auth/RequireAuth.tsx'
import HomePage from './HomePage.tsx'
import { CALLBACK_PATH, LOGIN_PATH } from './paths.ts'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path={LOGIN_PATH} element={<LoginPage />} />
      <Route path={CALLBACK_PATH} element={<CallbackPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<HomePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
