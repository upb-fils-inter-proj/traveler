# Deploy and roll back

How the test environment at Alacrity Education NGO is deployed, checked and
rolled back. The files are in this folder. The host pulls `main` itself, so it
needs neither a CI runner nor any inbound access.

## One-time setup on the server

1. Install Docker Engine with the Compose plugin, `git` and `openssl`.
2. Create the folder that holds what must not be committed:

   ```bash
   sudo mkdir -p /opt/traveler/certs/tourist /opt/traveler/certs/admin
   sudo chown -R "$USER": /opt/traveler     # the account that deploys
   ```

3. Create `/opt/traveler/.env` from [`.env.example`](.env.example). Replace every
   `change-me` (the comments there say how to generate the authentik values),
   set the two server names, and set `CERTS_DIR=/opt/traveler/certs`.
4. Put `fullchain.pem` and `privkey.pem` for each gateway in
   `/opt/traveler/certs/tourist` and `/opt/traveler/certs/admin`.
5. Open only the two gateway ports to the outside (443 and 8443 by default).
   authentik publishes plain HTTP on `AUTHENTIK_HTTP_PORT` (9000): never open it,
   reach it through the tunnel or an HTTPS proxy. Nothing else is published.
   Behind a tunnel, publish all three on the loopback interface only, see
   "Behind a tunnel" below.
6. Clone the repository to `/opt/traveler/repo`, then install the automatic
   deployment, see below.

## Deploy

**Automatically.** A systemd timer on the host runs `pull-deploy.sh` every 5
minutes (see "Install the automatic deployment"). It fetches `origin/main`, and if
`main` moved and the change touches a backend, the API contract or this folder, it
fast-forwards the checkout and runs `deploy.sh`.

- A change that touches only something else (documents, workflows) does not
  restart anything.
- If the build fails, the running stack is not touched, and the same commit is not
  tried again until `main` has a newer one.
- While the file `/opt/traveler/paused` exists, nothing is deployed.

**By hand on the server.**

```bash
cd <checkout of the repository>/infra/test
./deploy.sh
```

`deploy.sh` tags the images of both backends with the commit hash, starts the
stack, waits up to 5 minutes for both backends to be healthy, and appends the
tag to `/opt/traveler/deployed-tags`. If the backends do not become healthy it
prints the state and the last log lines and fails.

## Install the automatic deployment

On the server, as the account that deploys:

```bash
cd /opt/traveler/repo/infra/test
install -m 755 pull-deploy.sh /opt/traveler/bin/pull-deploy.sh
sed "s/^User=.*/User=$USER/" systemd/traveler-pull-deploy.service \
  | sudo tee /etc/systemd/system/traveler-pull-deploy.service > /dev/null
sudo install -m 644 systemd/traveler-pull-deploy.timer /etc/systemd/system/
sudo systemctl daemon-reload
/opt/traveler/bin/pull-deploy.sh --dry-run      # says what it would do
sudo systemctl enable --now traveler-pull-deploy.timer
```

The script is run from a copy outside the checkout, so a change on `main` never
replaces the program that deploys it. Copy it again when `pull-deploy.sh` changes.

```bash
systemctl list-timers traveler-pull-deploy.timer   # when it runs next
journalctl -u traveler-pull-deploy -n 40           # what it did
sudo systemctl start traveler-pull-deploy.service  # check now, without waiting
sudo systemctl disable --now traveler-pull-deploy.timer   # switch it off
```

### Behind a tunnel

When a tunnel on the same host (for example `cloudflared`) is the only way in,
publish the gateways on the loopback interface. Put this in
`/opt/traveler/repo/infra/test/compose.override.yaml`, which Compose reads
automatically, and keep it out of git (`.git/info/exclude`):

```yaml
services:
  tourist-gateway:
    ports: !override
      - "127.0.0.1:${TOURIST_HTTPS_PORT:-443}:443"
  admin-gateway:
    ports: !override
      - "127.0.0.1:${ADMIN_HTTPS_PORT:-8443}:443"
  authentik-server:
    ports: !override
      - "127.0.0.1:${AUTHENTIK_HTTP_PORT:-9000}:9000"
```

Point the tunnel at `https://localhost:443` and `https://localhost:8443`, with
certificate verification off for a self-signed certificate. Route the authentik
host name to `http://localhost:9000`, for example
`https://traveller-auth.lucasain.dev` → `http://localhost:9000`. The tunnel must
pass on the public host name and `X-Forwarded-Proto: https`: authentik builds
its issuer URLs from them.

### An account that is not in the docker group

Put a small `docker` helper first in the `PATH` of the unit (the unit already
looks in `/opt/traveler/bin` first). It runs docker through `sudo` and passes on
the variable that Compose reads:

```sh
#!/bin/sh
exec sudo -n env "IMAGE_TAG=${IMAGE_TAG:-latest}" /usr/bin/docker "$@"
```

Save it as `/opt/traveler/bin/docker` with mode 755. The account then needs
passwordless `sudo`, so use a dedicated account where that is possible.

## authentik

authentik (`ghcr.io/goauthentik/server:2026.8.5`) runs in the same stack as
`authentik-db`, `authentik-server` and `authentik-worker`. The example host
below is `https://traveller-auth.lucasain.dev`.

**On a server that already runs the stack**, add `PG_PASS`,
`AUTHENTIK_SECRET_KEY`, `AUTHENTIK_BOOTSTRAP_PASSWORD` and
`AUTHENTIK_BOOTSTRAP_EMAIL` to `/opt/traveler/.env`, and the `authentik-server`
lines to `compose.override.yaml`, before this reaches `main`. Otherwise the
deployment stops with `Set ... in .env`.

**First login.** Open `https://traveller-auth.lucasain.dev/` and sign in as
`akadmin` with `AUTHENTIK_BOOTSTRAP_PASSWORD`. The bootstrap values are read on
the very first start only, so change the password in authentik afterwards.

**Upgrades.** Change the tag of both authentik services together, after reading
the release notes. authentik does not support downgrading, so back up its
database first (see "Roll back").

### Sign-in applications

The blueprint [`authentik/blueprints/traveler.yaml`](authentik/blueprints/traveler.yaml)
creates the two applications. The worker applies it by itself when it starts
and whenever the file changes. Changes made to these two applications in the
authentik interface are overwritten the next time it is applied.

| Application | Slug = client ID | Redirect URIs |
| --- | --- | --- |
| Traveler admin (staff) | `traveler-admin` | `http://localhost:5173/auth/callback`, `<ADMIN_WEB_URL>/auth/callback` |
| Traveler app (tourists) | `traveler-app` | `TRAVELER_APP_REDIRECT_URI`, a placeholder until the mobile app work (EN-05) sets it |

- Issuer: `https://<authentik host>/application/o/<slug>/`, for example
  `https://traveller-auth.lucasain.dev/application/o/traveler-admin/`. Its
  `.well-known/openid-configuration` lists every endpoint.
- Public clients (no secret), authorization code flow with PKCE. Access tokens
  last 15 minutes, refresh tokens 30 days; authentik issues a refresh token
  only when the client asks for the `offline_access` scope.
- `aud` is the client ID, so each backend accepts only its own application's
  tokens.
- `sub` is the authentik user's UUID (`sub_mode: user_uuid`) instead of the
  default hashed ID, because the admin backend maps (`iss`, `sub`) to a row in
  `staff_users`, and those rows are seeded by hand on the test environment. The
  user's page in the admin interface does not show the UUID: while signed in
  as an administrator, open
  `https://traveller-auth.lucasain.dev/api/v3/core/users/?username=<username>`
  and read `uuid`.

The admin web app (`admin-web/.env`) uses the "Traveler admin" application:

| Variable | Value |
| --- | --- |
| `VITE_OIDC_AUTHORITY` | `https://traveller-auth.lucasain.dev/application/o/traveler-admin/` |
| `VITE_OIDC_CLIENT_ID` | `traveler-admin` |
| `VITE_OIDC_REDIRECT_URI` | `http://localhost:5173/auth/callback`, or `<ADMIN_WEB_URL>/auth/callback` |
| `VITE_OIDC_POST_LOGOUT_REDIRECT_URI` | `http://localhost:5173/login`, or `<ADMIN_WEB_URL>/login` |
| `VITE_OIDC_SCOPE` | `openid profile email` |

`ADMIN_WEB_URL` and `TRAVELER_APP_REDIRECT_URI` are read when the blueprint is
applied. After changing them in `/opt/traveler/.env` and deploying, the file
itself has not changed, so apply it by hand:

```bash
docker compose --env-file /opt/traveler/.env exec authentik-worker \
  ak apply_blueprint custom/traveler.yaml
```

## Check a deployment

```bash
cd infra/test
docker compose --env-file /opt/traveler/.env ps
```

Both backends, `authentik-server` and `authentik-worker` must show `healthy`.
Through the public gateways, the OpenAPI description must answer (use `-k` only
with a self-signed certificate), and authentik must answer its health check:

```bash
curl -k https://<tourist-server-name>/v3/api-docs
curl -k https://<admin-server-name>:8443/v3/api-docs
curl -i https://traveller-auth.lucasain.dev/-/health/ready/
```

`/actuator/` and `/internal/` answer 404 from outside, on purpose.

## Roll back

1. Pause the automatic deployment, otherwise the timer deploys `main` again
   within minutes and undoes the rollback:

   ```bash
   touch /opt/traveler/paused
   ```

2. Find the tag to go back to. The last line of `/opt/traveler/deployed-tags` is
   the running version, the one before it is the previous version:

   ```bash
   tail -n 3 /opt/traveler/deployed-tags
   ```

3. Deploy that tag:

   ```bash
   ./deploy.sh <tag>
   ```

   The images of earlier deployments stay on the server, so this starts them
   without a build. If the images were removed, the
   script builds them again from the checked-out commit, so check out that commit
   first.

4. When `main` has the fix, remove `/opt/traveler/paused`.

### The databases are not rolled back

Flyway migrations only go forward. A rollback changes the application, not the
database. It works when the new version added tables or columns that the old
version ignores. It can fail at start-up when the new version changed or removed
something the old version needs.

When a migration is the problem, fix forward: correct it in a new migration and
deploy that. Never edit a migration that has already run. The test data is not
precious, so as a last resort the volumes can be removed and recreated:

```bash
docker compose --env-file /opt/traveler/.env down --volumes
./deploy.sh
```

This deletes all three databases, including every authentik account.

authentik's own migrations only go forward too, and authentik does not support
downgrading. A rollback to a commit with an older authentik image needs its
database restored from a backup taken before the upgrade:

```bash
docker compose --env-file /opt/traveler/.env exec -T authentik-db \
  pg_dump -U authentik authentik > authentik-$(date +%F).sql
```

## Everyday commands

```bash
docker compose --env-file /opt/traveler/.env logs -f tourist-backend
docker compose --env-file /opt/traveler/.env restart admin-backend
docker compose --env-file /opt/traveler/.env down      # stops, keeps the data
```
