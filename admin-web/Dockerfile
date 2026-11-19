# The admin gateway image: Nginx serving the built admin web app, which also forwards
# the admin API to the admin backend (configuration from infra/test/nginx/templates).
# The build context is the repository root:
#   docker build -f admin-web/Dockerfile -t traveler/admin-gateway .
#
# Vite bakes the sign-in settings into the app at build time, so they are build arguments.

FROM node:26-alpine AS build
WORKDIR /workspace/admin-web
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin-web ./
ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID=traveler-admin
ARG VITE_OIDC_REDIRECT_URI
ARG VITE_OIDC_POST_LOGOUT_REDIRECT_URI
ARG VITE_OIDC_SCOPE="openid profile email"
RUN npm run build

FROM nginx:1.28-alpine
COPY --from=build /workspace/admin-web/dist /usr/share/nginx/html
