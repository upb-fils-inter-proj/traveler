# infra

Infrastructure for the test environment at Alacrity Education NGO: Docker
Compose files and deployment configuration for both backends, the two
PostgreSQL + PostGIS servers, Redis, MinIO and authentik.

## Test environment

[`test/`](test) holds the Compose file of the test environment.

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

The data networks and the network between the backends are internal: the
databases and Redis can only be reached from their own backend, and the
backends can only reach each other over the `backends` network.

Each backend runs its Flyway migrations when it starts.
