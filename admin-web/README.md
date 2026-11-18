# admin-web

Traveler web app for administrators and staff.

Its backend is [`admin-backend/`](../admin-backend). Staff sign in with
authentik accounts through the Traveler admin OIDC application.

## Stack

Vite, React and TypeScript (strict). Linting with ESLint, tests with Vitest and
Testing Library.

Requires Node `^22.22.2`, `^24.15.0` or `>=26` (the intersection of what the
tools in `package.json` support). CI runs Node 26.

## Commands

Run these from `admin-web/`.

| Command             | What it does                                    |
| ------------------- | ----------------------------------------------- |
| `npm ci`            | Install the exact versions from the lock file   |
| `npm run dev`       | Start the dev server on http://localhost:5173   |
| `npm run lint`      | Lint with ESLint                                |
| `npm run typecheck` | Type-check with `tsc`                           |
| `npm test`          | Run the tests (`npm test -- --run` runs once)   |
| `npm run build`     | Type-check and build the app into `dist/`       |
| `npm run preview`   | Serve the built `dist/` locally                 |

## Continuous integration

`.github/workflows/admin-web.yml` runs `npm ci`, lint, typecheck, test and build
on every pull request and on pushes to `main` that touch `admin-web/`.
