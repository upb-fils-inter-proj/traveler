# Deploy and roll back

How the test environment at Alacrity Education NGO is deployed, checked and
rolled back. The files are in this folder; the server needs the self-hosted
runner from [RUNNER.md](RUNNER.md).

## One-time setup on the server

1. Install Docker Engine with the Compose plugin, `git` and `openssl`.
2. Create the folder that holds what must not be committed:

   ```bash
   sudo mkdir -p /opt/traveler/certs/tourist /opt/traveler/certs/admin
   sudo chown -R github-runner: /opt/traveler
   ```

3. Create `/opt/traveler/.env` from [`.env.example`](.env.example). Replace every
   `change-me`, set the two server names, and set
   `CERTS_DIR=/opt/traveler/certs`.
4. Put `fullchain.pem` and `privkey.pem` for each gateway in
   `/opt/traveler/certs/tourist` and `/opt/traveler/certs/admin`.
5. Open only the two gateway ports to the outside (443 and 8443 by default).
   Nothing else is published.
6. Register the runner, see [RUNNER.md](RUNNER.md).

## Deploy

**Automatically.** Every push to `main` that changes a backend, the API contract
or this folder runs the workflow `deploy-test.yml` on the runner.

**By hand from GitHub.** Actions > Deploy test environment > Run workflow.

**By hand on the server.**

```bash
cd <checkout of the repository>/infra/test
./deploy.sh
```

`deploy.sh` tags the images of both backends with the commit hash, starts the
stack, waits up to 5 minutes for both backends to be healthy, and appends the
tag to `/opt/traveler/deployed-tags`. If the backends do not become healthy it
prints the state and the last log lines and fails.

## Check a deployment

```bash
cd infra/test
docker compose --env-file /opt/traveler/.env ps
```

Both backends must show `healthy`. Through the public gateways, the OpenAPI
description must answer (use `-k` only with a self-signed certificate):

```bash
curl -k https://<tourist-server-name>/v3/api-docs
curl -k https://<admin-server-name>:8443/v3/api-docs
```

`/actuator/` and `/internal/` answer 404 from outside, on purpose.

## Roll back

1. Find the tag to go back to. The last line of `/opt/traveler/deployed-tags` is
   the running version, the one before it is the previous version:

   ```bash
   tail -n 3 /opt/traveler/deployed-tags
   ```

2. Deploy that tag:

   ```bash
   ./deploy.sh <tag>
   ```

   The images of earlier deployments stay on the server, so this starts them
   without a build. If the images were removed, the
   script builds them again from the checked-out commit, so check out that commit
   first.

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

This deletes both databases.

## Everyday commands

```bash
docker compose --env-file /opt/traveler/.env logs -f tourist-backend
docker compose --env-file /opt/traveler/.env restart admin-backend
docker compose --env-file /opt/traveler/.env down      # stops, keeps the data
```
