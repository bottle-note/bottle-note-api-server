#!/usr/bin/env bash
set -euo pipefail

: "${REPOSITORY:?REPOSITORY is required}"
: "${RELEASE_BRANCH:?RELEASE_BRANCH is required}"
: "${RELEASE_TYPE:?RELEASE_TYPE is required}"

case "$RELEASE_TYPE" in
  standard) expected_prefix="releases/" ;;
  hotfix) expected_prefix="hotfixes/" ;;
  *)
    echo "::error::unsupported release type: ${RELEASE_TYPE}" >&2
    exit 1
    ;;
esac

[[ "$RELEASE_BRANCH" == "${expected_prefix}"* ]] || {
  echo "::error::refusing to delete a branch outside ${expected_prefix}: ${RELEASE_BRANCH}" >&2
  exit 1
}

endpoint="repos/${REPOSITORY}/git/ref/heads/${RELEASE_BRANCH}"
if response="$(gh api "$endpoint" 2>&1)"; then
  gh api -X DELETE "repos/${REPOSITORY}/git/refs/heads/${RELEASE_BRANCH}" >/dev/null
  echo "[release-pr] deleted release_branch=${RELEASE_BRANCH}"
  exit 0
fi

if [[ "$response" == *"HTTP 404"* || "$response" == *"Not Found"* ]]; then
  echo "[release-pr] release branch already absent: ${RELEASE_BRANCH}"
  exit 0
fi

echo "::error::unable to inspect release branch: ${RELEASE_BRANCH}" >&2
exit 1
