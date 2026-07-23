#!/usr/bin/env bash
set -euo pipefail

# 명시적 SemVer를 비파괴적으로 증가시킨다.

SEMVER_PATTERN='^(0|[1-9][0-9]*)\.((0|[1-9][0-9]*))\.((0|[1-9][0-9]*))$'
BASE_VERSION=""
BUMP_TYPE="patch"
VALUE_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --patch)
            BUMP_TYPE="patch"
            shift
            ;;
        --minor)
            BUMP_TYPE="minor"
            shift
            ;;
        --major)
            BUMP_TYPE="major"
            shift
            ;;
        --value-only)
            VALUE_ONLY=true
            shift
            ;;
        -*)
            echo "Usage: $0 <base-version> [--patch|--minor|--major] [--value-only]" >&2
            exit 1
            ;;
        *)
            [[ -z "$BASE_VERSION" ]] || {
                echo "Usage: $0 <base-version> [--patch|--minor|--major] [--value-only]" >&2
                exit 1
            }
            BASE_VERSION=$1
            shift
            ;;
    esac
done

[[ -n "$BASE_VERSION" ]] || {
    echo "[ERROR] 기준 버전이 필요합니다" >&2
    exit 1
}
[[ "$BASE_VERSION" =~ $SEMVER_PATTERN ]] || {
    echo "[ERROR] 기준 버전은 exact X.Y.Z 형식이어야 합니다: $BASE_VERSION" >&2
    exit 1
}

IFS=. read -r major minor patch <<< "$BASE_VERSION"
case $BUMP_TYPE in
    major)
        new_version="$((major + 1)).0.0"
        ;;
    minor)
        new_version="${major}.$((minor + 1)).0"
        ;;
    patch)
        new_version="${major}.${minor}.$((patch + 1))"
        ;;
esac

if [[ "$VALUE_ONLY" == "true" ]]; then
    echo "$new_version"
else
    echo "BASE_VERSION=$BASE_VERSION"
    echo "BUMP_TYPE=$BUMP_TYPE"
    echo "VERSION=$new_version"
fi
