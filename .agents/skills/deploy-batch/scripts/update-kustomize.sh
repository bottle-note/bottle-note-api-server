#!/usr/bin/env bash
set -euo pipefail

# update-kustomize.sh
# kustomization.yaml 업데이트 및 서브모듈 푸시

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 파라미터 파싱
VERSION=""
ENV="both"
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        production|development|both)
            ENV="$1"
            shift
            ;;
        -*)
            echo "Usage: $0 [version] [production|development|both] [--dry-run]" >&2
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

# 환경 파라미터 검증
if [[ "$ENV" != "production" ]] && [[ "$ENV" != "development" ]] && [[ "$ENV" != "both" ]]; then
    echo -e "${RED}[ERROR]${NC} 잘못된 환경: $ENV (production|development|both)" >&2
    exit 1
fi

REGISTRY="docker-registry.bottle-note.com"
IMAGE_NAME="bottlenote-batch"
FULL_TAG="batch_${VERSION}"
SUBMODULE_DIR="$PROJECT_ROOT/git.environment-variables"

echo "=== Kustomize 업데이트 ==="
echo "버전: $VERSION"
echo "태그: $FULL_TAG"
echo "환경: $ENV"
echo ""

# 서브모듈 동기화 (최우선)
echo -e "${GREEN}[SYNC]${NC} 서브모듈 원격 동기화..."
if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} cd $SUBMODULE_DIR && git fetch origin && git reset --hard origin/main"
else
    cd "$SUBMODULE_DIR"

    # 현재 브랜치 확인
    CURRENT_BRANCH=$(git branch --show-current)
    if [[ "$CURRENT_BRANCH" != "main" ]]; then
        echo -e "${YELLOW}[WARN]${NC} 현재 브랜치: $CURRENT_BRANCH (main이 아님)"
        git checkout main
    fi

    # 원격에서 최신 상태 가져오기
    git fetch origin

    # 로컬 변경사항 확인 및 처리
    if ! git diff --quiet || ! git diff --cached --quiet; then
        echo -e "${YELLOW}[WARN]${NC} 로컬 변경사항 있음 - 리셋합니다"
        git reset --hard origin/main
    else
        # 변경사항 없으면 fast-forward
        git reset --hard origin/main
    fi

    echo -e "${GREEN}[OK]${NC} 서브모듈 동기화 완료 (origin/main)"
fi
echo ""

update_kustomize() {
    local env=$1
    local overlay_dir="$SUBMODULE_DIR/deploy/overlays/$env"

    if [[ ! -d "$overlay_dir" ]]; then
        echo -e "${RED}[ERROR]${NC} 디렉토리 없음: $overlay_dir" >&2
        return 1
    fi

    echo -e "${GREEN}[$env]${NC} kustomize 업데이트..."

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${YELLOW}[DRY-RUN]${NC} cd $overlay_dir && kustomize edit set image bottlenote-batch=${REGISTRY}/${IMAGE_NAME}:${FULL_TAG}"
    else
        cd "$overlay_dir"
        kustomize edit set image "bottlenote-batch=${REGISTRY}/${IMAGE_NAME}:${FULL_TAG}"
        echo -e "${GREEN}[OK]${NC} $env 업데이트 완료"
    fi
}

# 환경별 업데이트
if [[ "$ENV" == "both" ]] || [[ "$ENV" == "production" ]]; then
    update_kustomize "production"
fi

if [[ "$ENV" == "both" ]] || [[ "$ENV" == "development" ]]; then
    update_kustomize "development"
fi

# 서브모듈 커밋 및 푸시
echo ""
echo -e "${GREEN}[COMMIT]${NC} 서브모듈 커밋 및 푸시..."

if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} cd $SUBMODULE_DIR"
    echo -e "${YELLOW}[DRY-RUN]${NC} git add -A"
    echo -e "${YELLOW}[DRY-RUN]${NC} git commit -m 'chore(batch): update image tag to ${FULL_TAG}'"
    echo -e "${YELLOW}[DRY-RUN]${NC} git push origin main"
else
    cd "$SUBMODULE_DIR"

    # 변경사항 확인
    if git diff --quiet && git diff --cached --quiet; then
        echo -e "${YELLOW}[SKIP]${NC} 변경사항 없음"
    else
        git add -A
        git commit -m "chore(batch): update image tag to ${FULL_TAG}"
        git push origin main
        echo -e "${GREEN}[OK]${NC} 서브모듈 푸시 완료"
    fi
fi

echo ""
echo "---"
echo "VERSION=$VERSION"
echo "TAG=$FULL_TAG"
echo "ENVIRONMENT=$ENV"
