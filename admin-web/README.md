# admin-web

Traveler web app for administrators and staff.

Its backend is [`admin-backend/`](../admin-backend). Staff sign in with
authentik accounts through the Traveler admin OIDC application.

## Stack

Vite, React and TypeScript (strict). Linting with ESLint, tests with Vitest and
Testing Library.

Requires Node `^22.22.2`, `^24.15.0` or `>=26` (the intersection of what the
tools in `package.json` support). CI runs Node 26.

## Sign-in

Staff sign in with their authentik account through the "Traveler admin" OIDC
application, using the authorization code flow with PKCE
([`oidc-client-ts`](https://github.com/authts/oidc-client-ts) with
`react-oidc-context`). There is no client secret.

authentik is not deployed yet, so no value is hard-coded. The app is configured
only through Vite environment variables:

| Variable                             | Meaning                                              |
| ------------------------------------ | ---------------------------------------------------- |
| `VITE_OIDC_AUTHORITY`                | Issuer URL of the authentik application              |
| `VITE_OIDC_CLIENT_ID`                | Client ID of the application                         |
| `VITE_OIDC_REDIRECT_URI`             | Where authentik returns after sign-in, path `/auth/callback` |
| `VITE_OIDC_POST_LOGOUT_REDIRECT_URI` | Where authentik returns after sign-out               |
| `VITE_OIDC_SCOPE`                    | Space-separated scopes, for example `openid profile email` |

Copy [`.env.example`](.env.example) to `.env` and replace the placeholders. Vite
reads the variables at start-up and at build time, so restart the dev server or
rebuild after changing them. The redirect URIs must also be registered in the
authentik application. If a variable is missing or invalid, the app shows a
configuration error that names it instead of failing.

The app has to be served from `localhost` or over HTTPS, because PKCE needs the
browser's Web Crypto API.

Tokens stay in `sessionStorage`, nothing auth-related is written to
`localStorage`. Silent token renewal is not set up yet: when the session
expires, the user signs in again. Role checks are not part of this skeleton.

## Sections

After signing in, a header shows the signed-in user with a sign-out button, and
a side menu links to three sections that are empty for now:

| Section                 | Route                      |
| ----------------------- | -------------------------- |
| City Administrator      | `/city-administrator`      |
| Partner                 | `/partner`                 |
| Traveler Administration | `/traveler-administration` |

The sections are listed in `src/sections.ts`, which drives both the menu and the
routes.

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
