#!/usr/bin/env bash
set -euo pipefail

# decrypt-registry.sh
# sops로 레지스트리 인증 정보 복호화

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

SOPS_ENV_FILE="$PROJECT_ROOT/git.environment-variables/storage/docker-registry/registry.sops.env"

# 옵션 파싱
EXPORT_MODE=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --export)
            EXPORT_MODE=true
            shift
            ;;
        *)
            echo "Usage: $0 [--export]"
            echo "  --export: export 문 형태로 출력 (source로 사용 가능)"
            exit 1
            ;;
    esac
done

# age 키 존재 확인
AGE_KEY_PATH="${HOME}/.config/sops/age/keys.txt"
if [[ ! -f "$AGE_KEY_PATH" ]]; then
    echo "ERROR: age 키 없음: $AGE_KEY_PATH" >&2
    exit 1
fi

# sops.env 파일 존재 확인
if [[ ! -f "$SOPS_ENV_FILE" ]]; then
    echo "ERROR: registry.sops.env 없음: $SOPS_ENV_FILE" >&2
    exit 1
fi

# sops로 복호화
DECRYPTED=$(sops -d "$SOPS_ENV_FILE" 2>/dev/null)

# 파싱
REGISTRY_ADDRESS=$(echo "$DECRYPTED" | grep "^REGISTRY_ADDRESS=" | cut -d'=' -f2-)
REGISTRY_USERNAME=$(echo "$DECRYPTED" | grep "^REGISTRY_USERNAME=" | cut -d'=' -f2-)
REGISTRY_PASSWORD=$(echo "$DECRYPTED" | grep "^REGISTRY_PASSWORD=" | cut -d'=' -f2-)
COSIGN_PASSWORD=$(echo "$DECRYPTED" | grep "^COSIGN_PASSWORD=" | cut -d'=' -f2-)

if [[ -z "$REGISTRY_ADDRESS" ]] || [[ -z "$REGISTRY_USERNAME" ]] || [[ -z "$REGISTRY_PASSWORD" ]]; then
    echo "ERROR: 복호화 실패 또는 필수 필드 누락" >&2
    exit 1
fi

if [[ "$EXPORT_MODE" == "true" ]]; then
    # source로 사용 가능한 형태로 출력
    echo "export REGISTRY_ADDRESS=\"$REGISTRY_ADDRESS\""
    echo "export REGISTRY_USERNAME=\"$REGISTRY_USERNAME\""
    echo "export REGISTRY_PASSWORD=\"$REGISTRY_PASSWORD\""
    [[ -n "$COSIGN_PASSWORD" ]] && echo "export COSIGN_PASSWORD=\"$COSIGN_PASSWORD\""
else
    # 일반 출력
    echo "REGISTRY_ADDRESS=$REGISTRY_ADDRESS"
    echo "REGISTRY_USERNAME=$REGISTRY_USERNAME"
    echo "REGISTRY_PASSWORD=***"
    echo "COSIGN_PASSWORD=${COSIGN_PASSWORD:+***}"
    echo ""
    echo "복호화 성공. 전체 값을 보려면 --export 옵션 사용"
fi
