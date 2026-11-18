import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from 'react-oidc-context'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from '../AppRoutes.tsx'
import ConfigErrorPage from '../auth/ConfigErrorPage.tsx'
import { ENV } from '../config.ts'
import { makeUser, mockAuth } from '../test/auth.ts'
import { LANGUAGE_STORAGE_KEY, setLanguage } from './index.ts'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

function renderApp(path = '/partner') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('language switch', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
    vi.mocked(useAuth).mockReturnValue(
      mockAuth({ isAuthenticated: true, user: makeUser({ name: 'Ana Pop' }) }),
    )
  })

  it('offers Romanian and English', () => {
    renderApp()

    const options = screen
      .getAllByRole('option')
      .map((option) => option.textContent)
    expect(options).toEqual(['Română', 'English'])
  })

  it('translates the whole screen to Romanian and back', async () => {
    renderApp()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Partner')

    await userEvent.selectOptions(screen.getByRole('combobox'), 'ro')

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Partener')
    expect(screen.getByText('Această secțiune este goală deocamdată.')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Navigare principală' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Administrator de oraș' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Administrare turiști' })).toBeInTheDocument()
    expect(screen.getByText('Autentificat ca Ana Pop')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Deconectare' })).toBeInTheDocument()
    expect(screen.getByLabelText('Limbă')).toHaveValue('ro')
    expect(document.title).toBe('Partener - Traveler Admin')
    expect(document.documentElement.lang).toBe('ro')

    await userEvent.selectOptions(screen.getByRole('combobox'), 'en')

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Partner')
    expect(screen.getByText('This section is empty for now.')).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('en')
  })

  it('remembers the chosen language', async () => {
    renderApp()

    await userEvent.selectOptions(screen.getByRole('combobox'), 'ro')

    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('ro')
  })

  it('translates the login screen', async () => {
    vi.mocked(useAuth).mockReturnValue(mockAuth())
    renderApp('/login')
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByRole('combobox'), 'ro')

    expect(screen.getByRole('button', { name: 'Autentificare' })).toBeInTheDocument()
    expect(
      screen.getByText('Autentifică-te cu contul tău authentik pentru a continua.'),
    ).toBeInTheDocument()
  })

  it('translates the configuration error', async () => {
    await setLanguage('ro')

    render(
      <ConfigErrorPage
        problems={[
          { variable: ENV.clientId, reason: 'missing' },
          { variable: ENV.authority, reason: 'invalid' },
        ]}
      />,
    )

    expect(screen.getByRole('heading', { name: 'Configurare incompletă' })).toBeInTheDocument()
    expect(screen.getByText(ENV.clientId).parentElement).toHaveTextContent('(lipsește)')
    expect(screen.getByText(ENV.authority).parentElement).toHaveTextContent('(invalidă)')
  })
})
