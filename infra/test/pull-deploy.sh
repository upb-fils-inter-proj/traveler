#!/usr/bin/env bash
# Deploys the test environment when main has changed. A systemd timer on the test
# host runs it every few minutes (see systemd/ and DEPLOY.md).
#
# It fetches origin/main. If main moved and the change touches something that is
# deployed (a backend, the API contract or infra/test), it fast-forwards the local
# checkout and runs deploy.sh. Nothing on GitHub can start it, so the host needs no
# inbound access and no self-hosted runner.
#
#   pull-deploy.sh            check and deploy if needed
#   pull-deploy.sh --dry-run  only say what would happen
#
# Settings come from environment variables:
#   REPO_DIR   checkout of the repository          (default /opt/traveler/repo)
#   STATE_DIR  state files and lock                (default /opt/traveler)
#   BRANCH     branch to deploy                    (default main)
#
# While the file $STATE_DIR/paused exists, nothing is deployed. Create it before a
# manual rollback, so that the timer does not undo it, and remove it afterwards.
#
# A commit whose deployment failed is not tried again until main has a newer commit,
# so a broken build does not repeat every few minutes. The running stack is not
# touched when a build fails.
set -euo pipefail

# Everything is inside a function, called on the last line followed by exit, so that
# the file can be replaced by the git merge below while it is running.
main() {
  local repo="${REPO_DIR:-/opt/traveler/repo}"
  local state="${STATE_DIR:-/opt/traveler}"
  local branch="${BRANCH:-main}"
  local dry=0
  [ "${1:-}" = "--dry-run" ] && dry=1

  mkdir -p "$state"
  exec 9>"$state/pull-deploy.lock"
  if ! flock -n 9; then
    echo "Another deployment is running."
    return 0
  fi

  if [ -e "$state/paused" ]; then
    echo "Paused: remove $state/paused to resume."
    return 0
  fi

  cd "$repo"
  git fetch -q origin "$branch"
  local target
  target="$(git rev-parse --short=12 "origin/$branch")"

  if [ "$(cat "$state/last-checked" 2>/dev/null || true)" = "$target" ]; then
    echo "$branch is at $target: nothing new."
    return 0
  fi
  if [ "$(cat "$state/failed-tag" 2>/dev/null || true)" = "$target" ]; then
    echo "$target failed to deploy before. Waiting for a newer commit."
    return 0
  fi

  local deployed
  deployed="$(tail -n 1 "$state/deployed-tags" 2>/dev/null | awk '{print $2}' || true)"

  # Deploy unless it is known that the change touches nothing that is deployed.
  if [ -n "$deployed" ] && git cat-file -e "${deployed}^{commit}" 2>/dev/null \
    && git diff --quiet "$deployed" "origin/$branch" -- tourist-backend admin-backend api infra/test; then
    echo "$target changes nothing that is deployed (running: $deployed)."
    [ "$dry" = 1 ] || echo "$target" > "$state/last-checked"
    return 0
  fi

  if [ "$dry" = 1 ]; then
    echo "Would deploy $target (running: ${deployed:-nothing})."
    return 0
  fi
  echo "Deploying $target (running: ${deployed:-nothing})."

  git checkout -q "$branch"
  git merge -q --ff-only "origin/$branch"
  if STATE_FILE="$state/deployed-tags" infra/test/deploy.sh; then
    echo "$target" > "$state/last-checked"
    rm -f "$state/failed-tag"
  else
    echo "$target" > "$state/failed-tag"
    echo "The deployment of $target failed." >&2
    return 1
  fi
}

main "$@"
exit
