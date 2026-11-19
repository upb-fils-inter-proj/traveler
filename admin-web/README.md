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
expires, the user signs in again.

## Admin API

The app calls the admin backend on its own origin, with relative URLs under
`/admin/...` and the user's access token in an `Authorization: Bearer` header
(`src/api/`). Errors come back as Spring ProblemDetail: the app shows a
translated summary, the server's `detail`, and each field error from `errors`
next to the matching form field.

In production the web server or gateway that serves the app has to route
`/admin/` to the admin backend. During development `npm run dev` (and
`npm run preview`) forward `/admin/` to `VITE_ADMIN_API_PROXY_TARGET`, which
defaults to `http://localhost:8080`. Set it in `.env` when the backend runs
elsewhere and restart the dev server.

The backend decides the role: the City Administrator section asks `GET
/admin/me` first and shows a "no access" message instead of the attraction
screens when the answer is 403 or a role other than `city_admin` or
`traveler_admin`.

## Sections

After signing in, a header shows the signed-in user with a sign-out button, and
a side menu links to three sections. City Administrator holds the attraction
screens; the other two are empty for now:

| Section                 | Route                      |
| ----------------------- | -------------------------- |
| City Administrator      | `/city-administrator`      |
| Partner                 | `/partner`                 |
| Traveler Administration | `/traveler-administration` |

The sections are listed in `src/sections.ts`, which drives both the menu and the
routes.

## Attractions

In the City Administrator section:

- The list shows the Romanian and English name, status, points and version of
  each attraction, sorted by the backend. A checkbox also shows removed ones.
- The form for a new or existing attraction has the name, description and the
  optional history and sources in Romanian and English side by side, the
  points, the visit duration and the location. The checks match the API
  contract, so most mistakes are shown before anything is sent.
- The location is set on an OpenStreetMap map (Leaflet with `react-leaflet`,
  with the OpenStreetMap attribution) by clicking or dragging the pin, or by
  typing the latitude and longitude. The map starts at Sinaia. Leaflet is only
  downloaded when a form opens.
- A saved attraction can be closed, reopened or removed. Removing asks first
  and is final.
- Photos: any image file is scaled down to at most 1600 pixels on the longer
  side and converted to WebP (quality 0.85) in the browser, then uploaded. The
  current photo is downloaded with the access token and shown from memory.
- "Print QR code" downloads the PDF with the QR code. "Generate new QR code"
  asks first, because the codes already printed stop working.

## Languages

The app is available in Romanian and English (`i18next` with `react-i18next`).
A language switcher is on every screen. The choice is saved in `localStorage`
under `traveler-admin.language`. Without a saved choice the app uses the browser
language when it is Romanian or English, and English otherwise.

The texts live in `src/i18n/locales/ro.json` and `src/i18n/locales/en.json`.
Every key has to exist in both files, a test checks this. Write Romanian with
comma-below diacritics (ș, ț), not cedillas.

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
