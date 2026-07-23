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

### 배포 버전 기준
!`.claude/skills/deploy-batch/scripts/check-version.sh 2>&1 || true`

## 배포 플로우

1. 사전 조건을 확인한다.
2. 최신 environment repository `main`의 development/production 태그를 조회한다.
3. 두 SemVer 중 최댓값의 patch 증가값을 기본 버전으로 추천한다.
4. 사용자에게 추천 버전 또는 더 큰 exact `X.Y.Z` 버전을 입력받는다.
5. 선택한 버전을 `check-version.sh <version>`으로 다시 검증한다.
6. 배포 환경과 cosign 서명 여부를 확인한다.
7. 같은 exact version으로 build, push, kustomize update를 순서대로 실행한다.

## 실행 가이드

### 1단계: 사전 조건 확인

```bash
.claude/skills/deploy-batch/scripts/check-prerequisites.sh
```

실패하면 안내된 조건을 충족한 뒤 처음부터 다시 확인한다.

### 2단계: 기준 버전과 추천값 확인

```bash
.claude/skills/deploy-batch/scripts/check-version.sh
```

출력 필드:

- `PRODUCTION_VERSION`: 최신 production batch 버전
- `DEVELOPMENT_VERSION`: 최신 development batch 버전
- `BASE_VERSION`: 두 환경의 SemVer 최댓값
- `RECOMMENDED_VERSION`: `BASE_VERSION`의 patch 증가값

태그 누락, invalid SemVer, fetch 실패 시 임의의 버전을 만들지 말고 중단한다.

### 3단계: 사용자 선택

AskUserQuestion으로 다음 항목을 확인한다.

- 버전: `RECOMMENDED_VERSION` 사용 또는 기준보다 큰 minor/major exact `X.Y.Z` 직접 입력
- 환경: Production만, Development만, 둘 다(권장)
- cosign: 서명 포함(권장), 서명 제외

선택한 exact version을 이후 단계의 `VERSION`으로 고정하고 검증한다.

```bash
.claude/skills/deploy-batch/scripts/check-version.sh "$VERSION"
```

### 4단계: 이미지 빌드

```bash
.claude/skills/deploy-batch/scripts/build-image.sh "$VERSION"
```

### 5단계: 이미지 푸시

```bash
.claude/skills/deploy-batch/scripts/push-image.sh "$VERSION" --sign
.claude/skills/deploy-batch/scripts/push-image.sh "$VERSION"
```

### 6단계: kustomize 업데이트

```bash
.claude/skills/deploy-batch/scripts/update-kustomize.sh "$VERSION" both
.claude/skills/deploy-batch/scripts/update-kustomize.sh "$VERSION" production
.claude/skills/deploy-batch/scripts/update-kustomize.sh "$VERSION" development
```

## 테스트

외부 변경 없이 명령 계약을 확인한다.

```bash
.claude/skills/deploy-batch/scripts/build-image.sh "$VERSION" --dry-run
.claude/skills/deploy-batch/scripts/push-image.sh "$VERSION" --dry-run --sign
.claude/skills/deploy-batch/scripts/update-kustomize.sh "$VERSION" both --dry-run
.claude/skills/deploy-batch/scripts/test-version-policy.sh
```

## 주의사항

- version은 prerelease/build metadata가 없는 exact `X.Y.Z`만 허용한다.
- 선택 버전은 development/production 배포 버전의 최댓값보다 커야 한다.
- 이미지 태그는 `batch_X.Y.Z`이며 기존 registry 태그를 덮어쓰지 않는다.
- kustomize update는 선택한 environment overlay만 변경한다.
- cosign 서명 시 cosign key와 password가 필요하다.
