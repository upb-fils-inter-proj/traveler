#!/usr/bin/env bash
# Deploys one commit of this repository to the test environment on this host.
#
#   ./deploy.sh            deploy the checked-out commit
#   ./deploy.sh <tag>      deploy (or roll back to) an image tag, usually a commit hash
#
# The images are tagged with the commit hash. A tag whose images are already on
# this host is started without a build, which is how a roll back works.
#
# Settings come from environment variables:
#   ENV_FILE    the .env file with the secrets   (default /opt/traveler/.env)
#   STATE_FILE  one line per successful deploy   (default /opt/traveler/deployed-tags)
set -euo pipefail

cd "$(dirname "$0")"

ENV_FILE="${ENV_FILE:-/opt/traveler/.env}"
STATE_FILE="${STATE_FILE:-/opt/traveler/deployed-tags}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE. Copy .env.example there and fill it in." >&2
  exit 1
fi

tag="${1:-$(git rev-parse --short=12 HEAD)}"
export IMAGE_TAG="$tag"

compose() { docker compose --env-file "$ENV_FILE" "$@"; }

if docker image inspect "traveler/tourist-backend:$tag" >/dev/null 2>&1 \
  && docker image inspect "traveler/admin-backend:$tag" >/dev/null 2>&1; then
  echo "Images for $tag are already on this host: starting them without a build."
  build_flag="--no-build"
else
  echo "Building the images for $tag."
  build_flag="--build"
fi

if ! compose up -d "$build_flag" --wait --wait-timeout 300 --remove-orphans; then
  echo "The deployment of $tag did not become healthy." >&2
  compose ps >&2 || true
  compose logs --tail 50 tourist-backend admin-backend >&2 || true
  exit 1
fi

mkdir -p "$(dirname "$STATE_FILE")"
echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) $tag" >> "$STATE_FILE"
echo "Deployed $tag."
