#!/usr/bin/env bash
set -euo pipefail

# push-image.sh
# Docker 푸시 및 선택적 cosign 서명

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 파라미터 파싱
VERSION=""
DRY_RUN=false
SIGN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --sign)
            SIGN=true
            shift
            ;;
        -*)
            echo "Usage: $0 [version] [--dry-run] [--sign]" >&2
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

echo "=== Docker 푸시 ==="
echo "이미지: $FULL_TAG"
echo "서명: $SIGN"
echo ""

# 레지스트리 인증 정보 복호화
echo -e "${GREEN}[1/3]${NC} 레지스트리 인증..."
if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} decrypt-registry.sh --export"
    REGISTRY_ADDRESS="docker-registry.bottle-note.com"
    REGISTRY_USERNAME="test"
    REGISTRY_PASSWORD="test"
else
    DECRYPT_OUTPUT=$("$SCRIPT_DIR/decrypt-registry.sh" --export)
    eval "$DECRYPT_OUTPUT"

    # Docker 로그인
    echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY_ADDRESS" -u "$REGISTRY_USERNAME" --password-stdin
    echo -e "${GREEN}[OK]${NC} Docker 로그인 완료"
fi

# Docker 푸시
echo -e "${GREEN}[2/3]${NC} Docker 이미지 푸시..."
if [[ "$DRY_RUN" == "true" ]]; then
    echo -e "${YELLOW}[DRY-RUN]${NC} docker push $FULL_TAG"
else
    docker push "$FULL_TAG"
    echo -e "${GREEN}[OK]${NC} 푸시 완료: $FULL_TAG"
fi

# cosign 서명 (선택)
if [[ "$SIGN" == "true" ]]; then
    echo -e "${GREEN}[3/3]${NC} cosign 서명..."

    COSIGN_KEY="$PROJECT_ROOT/git.environment-variables/storage/docker-registry/cosign.key"

    if [[ ! -f "$COSIGN_KEY" ]]; then
        echo -e "${RED}[ERROR]${NC} cosign 키 없음: $COSIGN_KEY" >&2
        exit 1
    fi

    if ! command -v cosign &> /dev/null; then
        echo -e "${RED}[ERROR]${NC} cosign 미설치" >&2
        exit 1
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${YELLOW}[DRY-RUN]${NC} COSIGN_PASSWORD=*** cosign sign --key $COSIGN_KEY $FULL_TAG"
    else
        # COSIGN_PASSWORD는 decrypt-registry.sh에서 export됨
        if [[ -z "${COSIGN_PASSWORD:-}" ]]; then
            echo -e "${RED}[ERROR]${NC} COSIGN_PASSWORD 없음 (registry.sops.env에 추가 필요)" >&2
            exit 1
        fi
        export COSIGN_PASSWORD
        cosign sign --key "$COSIGN_KEY" "$FULL_TAG"
        echo -e "${GREEN}[OK]${NC} cosign 서명 완료"
    fi
else
    echo -e "${YELLOW}[SKIP]${NC} cosign 서명 건너뜀 (--sign 옵션 없음)"
fi

echo ""
echo "---"
echo "PUSHED=$FULL_TAG"
echo "SIGNED=$SIGN"
