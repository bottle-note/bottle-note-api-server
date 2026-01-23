#!/usr/bin/env bash
set -euo pipefail

# build-image.sh
# Gradle 빌드 + Docker 이미지 빌드

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 파라미터 파싱
VERSION=""
DRY_RUN=false
SKIP_GRADLE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --skip-gradle)
            SKIP_GRADLE=true
            shift
            ;;
        -*)
            echo "Usage: $0 [version] [--dry-run] [--skip-gradle]" >&2
            exit 1
            ;;
        *)
            VERSION="$1"
            shift
            ;;
    esac
done

# 버전 파라미터 검증
if [[ -z "$VERSION" ]]; then
    VERSION_FILE="$PROJECT_ROOT/bottlenote-batch/VERSION"
    if [[ -f "$VERSION_FILE" ]]; then
        VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')
        echo -e "${YELLOW}[INFO]${NC} VERSION 파일에서 읽음: $VERSION"
    else
        echo -e "${RED}[ERROR]${NC} 버전을 지정하거나 VERSION 파일이 필요합니다" >&2
        exit 1
    fi
fi

REGISTRY="docker-registry.bottle-note.com"
IMAGE_NAME="bottlenote-batch"
FULL_TAG="${REGISTRY}/${IMAGE_NAME}:batch_${VERSION}"

echo "=== 빌드 시작 ==="
echo "버전: $VERSION"
echo "이미지: $FULL_TAG"
echo ""

# 1. Gradle 빌드
if [[ "$SKIP_GRADLE" == "false" ]]; then
    echo -e "${GREEN}[1/2]${NC} Gradle 빌드..."
    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${YELLOW}[DRY-RUN]${NC} ./gradlew :bottlenote-batch:build -x test --build-cache --parallel"
    else
        cd "$PROJECT_ROOT"
        ./gradlew :bottlenote-batch:build -x test --build-cache --parallel
        echo -e "${GREEN}[OK]${NC} Gradle 빌드 완료"
    fi
else
    echo -e "${YELLOW}[SKIP]${NC} Gradle 빌드 건너뜀"
fi

# 2. Docker 빌드
echo -e "${GREEN}[2/2]${NC} Docker 이미지 빌드..."
if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} docker build --platform linux/arm64 -f Dockerfile-batch -t $FULL_TAG ."
else
    cd "$PROJECT_ROOT"
    docker build --platform linux/arm64 -f Dockerfile-batch -t "$FULL_TAG" .
    echo -e "${GREEN}[OK]${NC} Docker 빌드 완료: $FULL_TAG"
fi

echo ""
echo "---"
echo "IMAGE=$FULL_TAG"
