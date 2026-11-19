import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

/** Where the dev server sends /admin requests when nothing else is set. */
const DEFAULT_ADMIN_API_PROXY_TARGET = 'http://localhost:8080'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  // The app calls the admin API on its own origin (/admin/...). In
  // production the gateway routes those paths; locally Vite forwards them.
  const proxy = {
    '/admin/': {
      target: env.VITE_ADMIN_API_PROXY_TARGET || DEFAULT_ADMIN_API_PROXY_TARGET,
      changeOrigin: true,
    },
  }

  return {
    plugins: [react()],
    server: {
      // The redirect URI registered for the app contains the port, so never
      // fall back to another one silently.
      port: 5173,
      strictPort: true,
      proxy,
    },
    preview: {
      proxy,
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      unstubGlobals: true,
    },
  }
})
