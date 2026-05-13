#!/usr/bin/env bash
set -euo pipefail

# check-version.sh
# 현재 버전과 배포된 태그 비교

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 환경 파라미터
ENV="${1:-both}"

if [[ "$ENV" != "production" ]] && [[ "$ENV" != "development" ]] && [[ "$ENV" != "both" ]]; then
    echo "Usage: $0 [production|development|both]" >&2
    exit 1
fi

# VERSION 파일 읽기
VERSION_FILE="$PROJECT_ROOT/bottlenote-batch/VERSION"
if [[ ! -f "$VERSION_FILE" ]]; then
    echo -e "${RED}[ERROR]${NC} VERSION 파일 없음: $VERSION_FILE" >&2
    exit 1
fi
CURRENT_VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')

# kustomization.yaml에서 batch 태그 추출
get_deployed_tag() {
    local env=$1
    local kustomize_file="$PROJECT_ROOT/git.environment-variables/deploy/overlays/$env/kustomization.yaml"

    if [[ ! -f "$kustomize_file" ]]; then
        echo "NOT_FOUND"
        return
    fi

    # newTag: batch_X.X.X 형식에서 버전 추출
    local tag=$(grep -A2 "name: bottlenote-batch" "$kustomize_file" | grep "newTag:" | sed 's/.*newTag: batch_//' | tr -d '[:space:]')

    if [[ -z "$tag" ]]; then
        echo "NOT_FOUND"
    else
        echo "$tag"
    fi
}

echo "=== 버전 확인 ==="
echo "VERSION 파일: $CURRENT_VERSION"
echo ""

CONFLICT=false

check_env() {
    local env=$1
    local deployed=$(get_deployed_tag "$env")

    if [[ "$deployed" == "NOT_FOUND" ]]; then
        echo -e "${YELLOW}[$env]${NC} 태그 없음"
    elif [[ "$deployed" == "$CURRENT_VERSION" ]]; then
        echo -e "${RED}[$env]${NC} 태그: batch_$deployed (버전 충돌!)"
        CONFLICT=true
    else
        echo -e "${GREEN}[$env]${NC} 태그: batch_$deployed"
    fi
}

if [[ "$ENV" == "both" ]] || [[ "$ENV" == "production" ]]; then
    check_env "production"
fi

if [[ "$ENV" == "both" ]] || [[ "$ENV" == "development" ]]; then
    check_env "development"
fi

echo ""

# 결과 출력 (스크립트에서 파싱 가능한 형식)
echo "---"
echo "VERSION=$CURRENT_VERSION"
PROD_TAG=$(get_deployed_tag "production")
DEV_TAG=$(get_deployed_tag "development")
echo "PRODUCTION_TAG=$PROD_TAG"
echo "DEVELOPMENT_TAG=$DEV_TAG"

if [[ "$CONFLICT" == "true" ]]; then
    echo "STATUS=CONFLICT"
    echo -e "${YELLOW}[권장]${NC} bump-version.sh 실행하여 버전 증가 필요"
    exit 1
else
    echo "STATUS=OK"
fi
