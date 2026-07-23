#!/usr/bin/env bash
set -euo pipefail

# 최신 배포 태그를 기준으로 다음 버전을 추천하고 exact 후보를 검증한다.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
ENV_REPO_DIR="${ENV_REPO_DIR:-$PROJECT_ROOT/git.environment-variables}"
SEMVER_PATTERN='^(0|[1-9][0-9]*)\.((0|[1-9][0-9]*))\.((0|[1-9][0-9]*))$'
CANDIDATE_VERSION="${1:-}"

fail() {
    echo "[ERROR] $1" >&2
    exit 1
}

is_semver() {
    [[ "$1" =~ $SEMVER_PATTERN ]]
}

semver_gt() {
    local left=$1
    local right=$2
    local left_major left_minor left_patch
    local right_major right_minor right_patch

    IFS=. read -r left_major left_minor left_patch <<< "$left"
    IFS=. read -r right_major right_minor right_patch <<< "$right"

    ((left_major > right_major)) && return 0
    ((left_major < right_major)) && return 1
    ((left_minor > right_minor)) && return 0
    ((left_minor < right_minor)) && return 1
    ((left_patch > right_patch))
}

get_deployed_version() {
    local environment=$1
    local overlay="deploy/overlays/${environment}/kustomization.yaml"
    local tag

    tag=$(
        git -C "$ENV_REPO_DIR" show "origin/main:${overlay}" |
            awk '
                /^[[:space:]]*-[[:space:]]*name:[[:space:]]*bottlenote-batch[[:space:]]*$/ {
                    found = 1
                    next
                }
                found && /^[[:space:]]*newTag:[[:space:]]*/ {
                    sub(/^[[:space:]]*newTag:[[:space:]]*/, "")
                    print
                    exit
                }
                found && /^[[:space:]]*-[[:space:]]*name:/ {
                    exit
                }
            '
    ) || fail "${environment} overlay를 origin/main에서 읽지 못했습니다"

    [[ "$tag" == batch_* ]] ||
        fail "${environment} batch 태그가 없거나 batch_X.Y.Z 형식이 아닙니다"

    local version=${tag#batch_}
    is_semver "$version" ||
        fail "${environment} batch 태그가 exact SemVer가 아닙니다: $tag"

    printf '%s\n' "$version"
}

[[ -d "$ENV_REPO_DIR" ]] ||
    fail "environment repository가 없습니다: $ENV_REPO_DIR"
git -C "$ENV_REPO_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1 ||
    fail "environment repository가 초기화되지 않았습니다: $ENV_REPO_DIR"
git -C "$ENV_REPO_DIR" fetch --quiet origin main ||
    fail "environment repository origin/main fetch에 실패했습니다"

PRODUCTION_VERSION=$(get_deployed_version production)
DEVELOPMENT_VERSION=$(get_deployed_version development)

BASE_VERSION=$PRODUCTION_VERSION
if semver_gt "$DEVELOPMENT_VERSION" "$PRODUCTION_VERSION"; then
    BASE_VERSION=$DEVELOPMENT_VERSION
fi

RECOMMENDED_VERSION=$(
    "$SCRIPT_DIR/bump-version.sh" "$BASE_VERSION" --patch --value-only
)

if [[ -n "$CANDIDATE_VERSION" ]]; then
    is_semver "$CANDIDATE_VERSION" ||
        fail "입력 버전은 exact X.Y.Z 형식이어야 합니다: $CANDIDATE_VERSION"
    semver_gt "$CANDIDATE_VERSION" "$BASE_VERSION" ||
        fail "입력 버전은 현재 최댓값 $BASE_VERSION 보다 커야 합니다"
fi

echo "PRODUCTION_VERSION=$PRODUCTION_VERSION"
echo "DEVELOPMENT_VERSION=$DEVELOPMENT_VERSION"
echo "BASE_VERSION=$BASE_VERSION"
echo "RECOMMENDED_VERSION=$RECOMMENDED_VERSION"
if [[ -n "$CANDIDATE_VERSION" ]]; then
    echo "CANDIDATE_VERSION=$CANDIDATE_VERSION"
fi
echo "STATUS=OK"
