#!/usr/bin/env bash
set -euo pipefail

# bump-version.sh
# 패치 버전 자동 증가 (semver)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 옵션 파싱
DRY_RUN=false
BUMP_TYPE="patch"

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
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
        --patch)
            BUMP_TYPE="patch"
            shift
            ;;
        *)
            echo "Usage: $0 [--dry-run] [--patch|--minor|--major]" >&2
            exit 1
            ;;
    esac
done

VERSION_FILE="$PROJECT_ROOT/bottlenote-batch/VERSION"

if [[ ! -f "$VERSION_FILE" ]]; then
    echo -e "${RED}[ERROR]${NC} VERSION 파일 없음: $VERSION_FILE" >&2
    exit 1
fi

CURRENT_VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')

# semver 파싱
if [[ ! "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo -e "${RED}[ERROR]${NC} 잘못된 버전 형식: $CURRENT_VERSION (expected: X.Y.Z)" >&2
    exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

# 버전 증가
case $BUMP_TYPE in
    major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_VERSION="${NEW_MAJOR}.0.0"
        ;;
    minor)
        NEW_MINOR=$((MINOR + 1))
        NEW_VERSION="${MAJOR}.${NEW_MINOR}.0"
        ;;
    patch)
        NEW_PATCH=$((PATCH + 1))
        NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"
        ;;
esac

echo "=== 버전 증가 ($BUMP_TYPE) ==="
echo "OLD=$CURRENT_VERSION"
echo "NEW=$NEW_VERSION"

if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} 실제 파일 수정 없음"
else
    echo "$NEW_VERSION" > "$VERSION_FILE"
    echo -e "${GREEN}[OK]${NC} VERSION 파일 업데이트 완료"
fi
