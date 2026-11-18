import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import App from './App.tsx'
import { ENV } from './config.ts'
import { validEnv } from './test/auth.ts'

describe('App configuration', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.history.pushState({}, '', '/')
  })

  afterEach(() => {
    window.sessionStorage.clear()
  })

  it('shows a configuration error instead of crashing when the settings are missing', () => {
    render(<App env={{}} />)

    expect(screen.getByRole('heading', { name: 'Configuration incomplete' })).toBeInTheDocument()
    for (const variable of Object.values(ENV)) {
      expect(screen.getByText(variable)).toBeInTheDocument()
    }
    expect(screen.queryByRole('button', { name: 'Sign in' })).not.toBeInTheDocument()
  })

  it('names only the variables that are wrong', () => {
    render(<App env={{ ...validEnv, VITE_OIDC_CLIENT_ID: '' }} />)

    expect(screen.getByText(ENV.clientId)).toBeInTheDocument()
    expect(screen.queryByText(ENV.authority)).not.toBeInTheDocument()
  })

  it('sends a visitor without a session to the login screen when configured', async () => {
    render(<App env={validEnv} />)

    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/login')
  })
})
