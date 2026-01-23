#!/usr/bin/env bash
set -euo pipefail

# check-prerequisites.sh
# 배포에 필요한 도구와 키 존재 확인
# 필수 조건 미충족 시 즉시 중단

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

panic() {
    echo -e "${RED}[FATAL]${NC} $1" >&2
    echo -e "${YELLOW}[해결방법]${NC} $2" >&2
    exit 1
}

ok() {
    echo -e "${GREEN}[OK]${NC} $1"
}

echo "=== 사전 조건 확인 ==="

# 1. age 키 확인
AGE_KEY_PATH="${HOME}/.config/sops/age/keys.txt"
if [[ -f "$AGE_KEY_PATH" ]]; then
    ok "age 키: $AGE_KEY_PATH"
else
    panic "age 키 없음: $AGE_KEY_PATH" \
          "age-keygen -o ~/.config/sops/age/keys.txt"
fi

# 2. sops 설치 확인
if command -v sops &> /dev/null; then
    SOPS_VERSION=$(sops --version 2>&1 | head -1)
    ok "sops: $SOPS_VERSION"
else
    panic "sops 미설치" \
          "brew install sops"
fi

# 3. docker 설치 확인
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    ok "docker: $DOCKER_VERSION"
else
    panic "docker 미설치" \
          "brew install --cask docker"
fi

# 4. kustomize 설치 확인
if command -v kustomize &> /dev/null; then
    KUSTOMIZE_VERSION=$(kustomize version 2>&1 | head -1)
    ok "kustomize: $KUSTOMIZE_VERSION"
else
    panic "kustomize 미설치" \
          "brew install kustomize"
fi

# 5. cosign 설치 확인 (선택사항 - 경고만)
if command -v cosign &> /dev/null; then
    COSIGN_VERSION=$(cosign version 2>&1 | grep -i version | head -1 || echo "installed")
    ok "cosign: $COSIGN_VERSION"
else
    echo -e "${YELLOW}[WARN]${NC} cosign 미설치 (서명 기능 제한) - brew install cosign"
fi

# 6. VERSION 파일 확인
VERSION_FILE="$PROJECT_ROOT/bottlenote-batch/VERSION"
if [[ -f "$VERSION_FILE" ]]; then
    VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')
    ok "VERSION 파일: $VERSION"
else
    panic "VERSION 파일 없음: $VERSION_FILE" \
          "echo '1.0.0' > $VERSION_FILE"
fi

# 7. registry.sops.env 파일 확인
SOPS_ENV_FILE="$PROJECT_ROOT/git.environment-variables/storage/docker-registry/registry.sops.env"
if [[ -f "$SOPS_ENV_FILE" ]]; then
    ok "registry.sops.env 존재"
else
    panic "registry.sops.env 없음" \
          "git submodule update --init --recursive"
fi

# 8. Dockerfile-batch 확인
DOCKERFILE="$PROJECT_ROOT/Dockerfile-batch"
if [[ -f "$DOCKERFILE" ]]; then
    ok "Dockerfile-batch 존재"
else
    panic "Dockerfile-batch 없음: $DOCKERFILE" \
          "프로젝트 루트에 Dockerfile-batch 파일 필요"
fi

echo ""
echo -e "${GREEN}=== 모든 사전 조건 충족 ===${NC}"
exit 0
