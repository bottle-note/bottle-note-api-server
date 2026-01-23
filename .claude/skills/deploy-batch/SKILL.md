---
name: deploy-batch
description: |
  배치 모듈 배포. "배치 배포", "batch deploy", "배치 이미지 올려줘" 요청 시 사용.
  단계별 스크립트로 구성되어 개별 테스트 가능.
disable-model-invocation: true
allowed-tools: Bash, Read, Edit, Write, AskUserQuestion
---

# Batch 모듈 배포

## 현재 상태

### 사전 조건
!`.claude/skills/deploy-batch/scripts/check-prerequisites.sh 2>&1 || true`

### 버전 정보
!`.claude/skills/deploy-batch/scripts/check-version.sh both 2>&1 || true`

## 배포 플로우

```
1. 사전 조건 확인 (check-prerequisites.sh)
2. 레지스트리 인증 복호화 (decrypt-registry.sh)
3. 버전 확인 (check-version.sh)
   → 충돌 시 버전 증가 (bump-version.sh)
4. [사용자 질문] 배포 환경 선택
5. [사용자 질문] cosign 서명 여부
6. 이미지 빌드 (build-image.sh)
7. 이미지 푸시 (push-image.sh)
8. kustomize 업데이트 (update-kustomize.sh)
```

## 실행 가이드

### 1단계: 사전 조건 확인
```bash
.claude/skills/deploy-batch/scripts/check-prerequisites.sh
```
- 실패 시 안내된 설치 명령어 실행 후 재시도

### 2단계: 버전 확인 및 조정
```bash
.claude/skills/deploy-batch/scripts/check-version.sh both
```
- STATUS=CONFLICT 시 버전 증가 필요:
```bash
.claude/skills/deploy-batch/scripts/bump-version.sh --patch
```

### 3단계: 사용자에게 질문 (AskUserQuestion)

**배포 환경 선택:**
- Production만
- Development만
- 둘 다 (권장)

**cosign 서명 여부:**
- 서명 포함 (권장)
- 서명 제외

### 4단계: 이미지 빌드
```bash
# VERSION 생략 시 VERSION 파일에서 자동 읽음
.claude/skills/deploy-batch/scripts/build-image.sh
.claude/skills/deploy-batch/scripts/build-image.sh 1.0.0  # 명시적 버전
```

### 5단계: 이미지 푸시
```bash
# 서명 없이
.claude/skills/deploy-batch/scripts/push-image.sh

# 서명 포함
.claude/skills/deploy-batch/scripts/push-image.sh --sign
```

### 6단계: kustomize 업데이트
```bash
# 환경: production | development | both (기본값: both)
.claude/skills/deploy-batch/scripts/update-kustomize.sh
.claude/skills/deploy-batch/scripts/update-kustomize.sh production
.claude/skills/deploy-batch/scripts/update-kustomize.sh 1.0.0 both  # 명시적 버전
```

## 스크립트 목록

| 스크립트 | 용도 |
|---------|------|
| `check-prerequisites.sh` | 필수 도구/키 확인, 미충족 시 즉시 중단 |
| `decrypt-registry.sh` | sops로 레지스트리 인증 정보 복호화 |
| `check-version.sh` | VERSION 파일과 배포 태그 비교 |
| `bump-version.sh` | semver 패치/마이너/메이저 증가 |
| `build-image.sh` | Gradle + Docker 빌드 |
| `push-image.sh` | Docker 푸시 + cosign 서명 (선택) |
| `update-kustomize.sh` | kustomize 태그 업데이트 + 서브모듈 푸시 |

## 테스트 (dry-run)

모든 스크립트는 `--dry-run` 옵션 지원:
```bash
.claude/skills/deploy-batch/scripts/bump-version.sh --dry-run
.claude/skills/deploy-batch/scripts/build-image.sh --dry-run
.claude/skills/deploy-batch/scripts/push-image.sh --dry-run --sign
.claude/skills/deploy-batch/scripts/update-kustomize.sh both --dry-run
```

## 주의사항

- VERSION에 `+` 문자 사용 금지 (Docker 태그 제한)
- 서브모듈 푸시 전 자동으로 `git pull --rebase` 실행
- cosign 서명 시 cosign.key 파일 필요
