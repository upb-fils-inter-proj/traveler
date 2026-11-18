# Self-hosted runner

A GitHub Actions runner on the test server deploys `main` to the test
environment (workflow `deploy-test.yml`). The runner only makes outbound
connections to GitHub, so the NGO does not have to allow any inbound access.

## Requirements on the server

- Linux in the EU (confirm the location with the NGO).
- Docker Engine with the Compose plugin, `git` and `openssl`.
- A dedicated user, for example `github-runner`, that is a member of the
  `docker` group. Membership of that group is equivalent to root on the host,
  so this user must not be used for anything else.
- `/opt/traveler/` owned by that user, holding `.env` and `certs/` (see
  [DEPLOY.md](DEPLOY.md)).

## Install and register

1. In the repository, open Settings > Actions > Runners > New self-hosted runner,
   choose Linux and the server's architecture, and follow the download and
   `config.sh` commands it shows, as the runner user.
2. At the `config.sh` step, add the label the workflow expects:

   ```bash
   ./config.sh --url https://github.com/upb-fils-inter-proj/traveler \
     --token <token shown by GitHub> --labels traveler-test --unattended
   ```

3. Install it as a service, so it starts with the server:

   ```bash
   sudo ./svc.sh install github-runner
   sudo ./svc.sh start
   ```

4. In Settings > Actions > Runners the runner must show as Idle with the labels
   `self-hosted` and `traveler-test`.

## Keep it safe

This repository is public, so anyone can open a pull request. A self-hosted
runner must never run code from one.

- Only `deploy-test.yml` uses this runner, and it runs only for pushes to `main`
  and manual starts. Do not add a `pull_request` trigger to a workflow that uses
  `runs-on: [self-hosted, ...]`.
- In Settings > Actions > General, set "Fork pull request workflows" to require
  approval for all outside collaborators.
- Keep the runner registered to this repository only, not to the organization.
