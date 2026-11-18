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
   `change-me`, set the two server names, and set
   `CERTS_DIR=/opt/traveler/certs`.
4. Put `fullchain.pem` and `privkey.pem` for each gateway in
   `/opt/traveler/certs/tourist` and `/opt/traveler/certs/admin`.
5. Open only the two gateway ports to the outside (443 and 8443 by default).
   Nothing else is published. Behind a tunnel, publish them on the loopback
   interface only, see "Behind a tunnel" below.
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
```

Point the tunnel at `https://localhost:443` and `https://localhost:8443`, with
certificate verification off for a self-signed certificate.

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

This deletes both databases.

## Everyday commands

```bash
docker compose --env-file /opt/traveler/.env logs -f tourist-backend
docker compose --env-file /opt/traveler/.env restart admin-backend
docker compose --env-file /opt/traveler/.env down      # stops, keeps the data
```
