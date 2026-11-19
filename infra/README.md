# infra

Infrastructure for the test environment at Alacrity Education NGO: Docker
Compose files and deployment configuration for both backends, the two
PostgreSQL + PostGIS servers, Redis, MinIO and authentik.

## Test environment

[`test/`](test) holds the Compose file of the test environment. How it is
deployed and rolled back is in [`test/DEPLOY.md`](test/DEPLOY.md). The host pulls
`main` itself with a timer, so no CI runner is needed.

```bash
cd infra/test
cp .env.example .env      # fill in the passwords
docker compose up -d --build
docker compose ps
```

| Service | What it is |
| --- | --- |
| `tourist-db`, `admin-db` | Two separate PostgreSQL 16 + PostGIS servers, one per backend |
| `redis` | Cache of the tourist backend |
| `tourist-backend`, `admin-backend` | The two backends, built from this repository |
| `tourist-gateway`, `admin-gateway` | One public Nginx gateway per backend: HTTPS, rate limiting, load balancing. The admin gateway also serves the admin web app |
| `authentik-server`, `authentik-worker` | authentik 2026.8.5, the identity provider (OIDC) of both applications |
| `authentik-db` | authentik's own PostgreSQL 16 server |
| `object-storage` | RustFS 1.0.6 (S3 API) for attraction photos, reachable only from the admin backend |

The data networks and the network between the backends are internal: the
databases and Redis can only be reached from their own backend (authentik's
database only from authentik), and the backends can only reach each other over
the `backends` network.

Each backend runs its Flyway migrations when it starts.

### Gateways and certificates

Only the gateways publish a port: the tourist gateway on `TOURIST_HTTPS_PORT`
(443) and the admin gateway on `ADMIN_HTTPS_PORT` (8443), HTTPS only. Both block
`/internal/` and `/actuator/`, so the calls between the backends and the
health endpoints are never public. The one exception is authentik, which
publishes plain HTTP on `AUTHENTIK_HTTP_PORT` (9000) for a tunnel or an HTTPS
proxy in front of it, see [`test/DEPLOY.md`](test/DEPLOY.md).

Each gateway reads `fullchain.pem` and `privkey.pem` from its own folder,
`test/certs/tourist` and `test/certs/admin`. The certificates are never
committed. For a first run, create throwaway ones:

```bash
cd infra/test
for gateway in tourist admin; do
  mkdir -p certs/$gateway
  openssl req -x509 -newkey rsa:2048 -nodes -days 30 -subj "/CN=localhost" \
    -keyout certs/$gateway/privkey.pem -out certs/$gateway/fullchain.pem
done
```

To balance the load over more instances of a backend, add a `server` line to the
`upstream` block of its template in `test/nginx/templates`.
