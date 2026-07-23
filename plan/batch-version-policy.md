# Plan: batch 배포 버전 정책

## Overview

`bottlenote-batch` 배포 버전의 source of truth를 저장소의 `VERSION` 파일에서 배포 요청자가 입력하는 exact SemVer로 전환한다.

GitHub Actions 수동 배포는 필수 `X.Y.Z` 버전 입력을 받아 `batch_X.Y.Z` 이미지 태그를 만들고, 기존의 이미지 태그 중복 차단을 유지한다. 로컬 `deploy-batch` 스킬은 `environment-variables` 최신 `main`에 선언된 development/production batch 태그를 함께 조회하여 SemVer 최댓값의 patch 증가 버전을 기본값으로 추천한다. 사용자는 추천값 대신 더 큰 minor/major 계열의 exact `X.Y.Z` 버전을 직접 입력할 수 있다.

`.agents`와 `.claude`에 복제된 `deploy-batch` 스킬 및 스크립트는 같은 계약과 동작을 갖도록 함께 변경하고 동일성을 검증한다.

### Assumptions

- 이 문서의 요구사항은 사용자가 승인한 계약이며 추가 기능 확장 없이 그대로 구현한다.
- 버전 형식은 prerelease/build metadata가 없는 exact `X.Y.Z`만 허용한다.
- 버전 비교는 문자열 정렬이 아니라 major, minor, patch의 숫자값을 기준으로 한다.
- 기준 버전은 `environment-variables` 최신 `origin/main`의 development와 production overlay에 선언된 `bottlenote-batch` 태그 중 SemVer 최댓값이다.
- 기본 추천값은 기준 버전의 patch만 1 증가한 값이다.
- 사용자가 직접 입력하는 minor/major exact 버전도 기준 버전보다 커야 한다.
- GitHub Actions workflow는 입력한 exact 버전만 소비하며 자동으로 버전을 계산하거나 변경하지 않는다.
- 이미지 태그는 계속 `batch_X.Y.Z` 형식을 사용하고, registry에 이미 존재하는 태그는 prepare와 push 직전의 두 단계에서 차단한다.
- production 배포는 계속 `main` 브랜치에서만 허용한다.
- DB schema, batch 애플리케이션 코드, API 계약, 배포 대상 overlay 구조는 변경하지 않는다.

### Success Criteria

- `bottlenote-batch/VERSION` 파일이 삭제되고 workflow 및 `deploy-batch` 스킬/스크립트 어디에서도 이를 읽거나 갱신하지 않는다.
- `.github/workflows/deploy_batch.yml`의 `workflow_dispatch`에 필수 exact version 입력이 존재한다.
- workflow가 입력값을 `X.Y.Z` 형식으로 검증하고 정확히 `batch_${version}` 이미지 태그를 생성한다.
- version 입력이 비어 있거나 `v1.2.3`, `1.2`, `1.2.3-rc.1`, `1.2.3+build`처럼 exact `X.Y.Z`가 아니면 이미지 빌드 전에 실패한다.
- registry의 동일 이미지 태그 존재 여부를 prepare 단계와 push 직전 단계에서 각각 확인하고, 이미 존재하면 덮어쓰지 않고 실패한다.
- production의 main 브랜치 제한, production approval, 이미지 서명, GitOps 업데이트 흐름은 유지된다.
- GitOps 업데이트 실패 안내와 deployment summary가 `VERSION` 파일 증가가 아니라 새 exact version을 입력해 재실행하도록 안내한다.
- `deploy-batch` 스킬이 `environment-variables`의 최신 `origin/main`을 기준으로 development/production batch 태그를 모두 읽는다.
- 두 환경의 유효한 SemVer 중 최댓값을 숫자 비교로 계산하고 patch 증가값을 기본 추천한다.
- 2026-07-23 확인값인 development `1.0.6`, production `1.0.6`에 대해서는 기본 추천값 `1.0.7`을 산출한다.
- 태그 누락, 잘못된 태그 형식, fetch 실패 시 임의의 버전을 추천하지 않고 명시적으로 실패한다.
- 사용자가 추천 patch 버전을 선택하거나, 기준보다 큰 minor/major 계열 exact `X.Y.Z`를 직접 입력할 수 있다.
- build, push, kustomize update 스크립트는 VERSION 파일 fallback 없이 exact version 인자를 필수로 받는다.
- `.agents/skills/deploy-batch`와 `.claude/skills/deploy-batch`의 대응 파일 목록과 SHA-256이 모두 일치한다.
- 변경된 shell script의 syntax 검사와 대표 정상/오류 SemVer 시나리오 검증이 통과한다.
- workflow YAML parse 또는 동등한 정적 검증이 통과한다.
- `git diff --check`가 통과하고 요구 범위 밖 기존 변경이 보존된다.

### Impact Scope

- GitHub Actions:
  - `.github/workflows/deploy_batch.yml`
  - 필수 version 입력, exact SemVer 검증, 이미지 태그 생성, 실패/summary 문구 변경
- Batch module:
  - `bottlenote-batch/VERSION` 삭제
  - Java/Gradle/resources 변경 없음
- Codex deploy skill:
  - `.agents/skills/deploy-batch/SKILL.md`
  - `.agents/skills/deploy-batch/scripts/*.sh`
- Claude deploy skill:
  - `.claude/skills/deploy-batch/SKILL.md`
  - `.claude/skills/deploy-batch/scripts/*.sh`
- Environment repository:
  - `git.environment-variables`의 최신 `origin/main`과 두 overlay는 버전 계산의 읽기 대상
  - 버전 추천 단계에서는 파일, commit, submodule pointer를 변경하지 않음
  - 실제 배포의 선택된 overlay 갱신 동작은 기존 범위로 유지
- Persistence / async / cache:
  - DB schema와 데이터 변경 없음
  - 신규 event, async 처리, cache invalidation 없음
- Tests:
  - exact SemVer 입력 검증
  - development/production SemVer 최댓값 및 patch 추천 계산
  - minor/major exact 입력의 유효성 및 기준 버전 초과 검증
  - VERSION 파일 의존 제거
  - immutable 이미지 태그 차단 유지
  - `.agents`/`.claude` 복제본 동일성 검증

### Non-Goals

- `deploy_batch.yml` 외 다른 application 배포 workflow의 버전 정책 변경
- 이미지 태그 naming convention 변경
- registry 이미지 삭제 또는 기존 태그 재사용
- production approval 또는 cosign 서명 정책 변경
- environment overlay 구조 변경
- batch 애플리케이션 로직, Gradle 의존성, DB migration 변경

### Current Evidence

- 작업 브랜치: `Whale0928/batch-version-policy`
- 기준 commit: `origin/main` `c76c8667fd9bb7374684b4f7dbe8eef230529ff3`
- define 시점 environment-variables 최신 `origin/main`: `076fbfc176d8feb4ac8cd54fc673deea44d4ac51`
- development tag: `batch_1.0.6`
- production tag: `batch_1.0.6`
- 계산된 기본 patch 추천: `1.0.7`
- 기존 `.agents`와 `.claude` deploy-batch 대응 파일은 define 시점 SHA-256이 모두 일치한다.

## Approval Gate

아래 태스크 목록 승인 후 별도 사용자 메시지에서 `/implement`로 구현을 시작한다.

## Dependency Analysis

버전 정책은 배포 애플리케이션 내부의 batch job/step이 아니라 수동 배포 binding과 로컬 배포 CLI 성격의 스크립트에만 영향을 준다. 구현은 source of truth와 검증 규칙을 먼저 고정한 뒤 소비자를 전환하는 순서로 진행한다.

1. Foundation: development/production 태그 조회, 숫자 SemVer 비교, patch 추천, exact candidate 검증을 비파괴 shell helper로 고정한다.
2. Local consumers: build, push, kustomize update가 VERSION fallback 없이 exact version 인자를 소비하도록 전환한다.
3. Local binding: `deploy-batch` 스킬과 prerequisite가 새 helper 및 필수 인자 계약만 안내하도록 맞춘다.
4. GitHub binding: workflow dispatch가 exact version을 필수 입력으로 받아 동일한 태그 규칙을 적용한다. 이 작업은 로컬 소비자 전환과 독립적이지만 동일한 계약을 따른다.
5. Regression guard: 실제 임시 Git remote/overlay fixture를 이용해 추천·검증 동작을 확인하고, `.agents`/`.claude` parity와 workflow 정적 계약을 함께 검증한다.

의존 관계는 `Task 1 → Task 2 → Task 3`, `Task 1 → Task 4`, `Tasks 1-4 → Task 5`다. Task 4는 Task 2-3과 파일 충돌 없이 병렬 구현할 수 있지만, exact SemVer 규칙은 Task 1의 계약을 기준으로 맞춘다.

## Tasks

### Task 1: SemVer 기준 계산기 도입

- Acceptance: `check-version.sh`가 최신 environment repository `origin/main`의 development/production batch 태그를 읽고 숫자 SemVer 최댓값과 patch 추천값을 출력한다.
- Acceptance: 태그 누락·형식 오류·fetch 실패는 명시적으로 실패하며, exact candidate가 `X.Y.Z`이고 기준값보다 큰지 검증할 수 있다.
- Acceptance: `bump-version.sh`는 VERSION 파일을 쓰지 않고 명시적 기준값에서 patch/minor/major 값을 계산하는 비파괴 helper로 전환된다.
- Verification: `bash -n .agents/skills/deploy-batch/scripts/{check-version,bump-version}.sh .claude/skills/deploy-batch/scripts/{check-version,bump-version}.sh`
- Verification: 임시 Git remote와 development/production overlay fixture에서 `1.0.6 / 1.0.6 → 1.0.7`, 서로 다른 major/minor/patch 최댓값, invalid/missing tag, 기준 이하 candidate 실패를 실행 확인
- Files: `.agents/skills/deploy-batch/scripts/check-version.sh`, `.agents/skills/deploy-batch/scripts/bump-version.sh`, `.claude/skills/deploy-batch/scripts/check-version.sh`, `.claude/skills/deploy-batch/scripts/bump-version.sh`
- Size: M
- Status: [x] done

### Task 2: 배포 실행 스크립트에 exact version 강제

- Acceptance: build, push, kustomize update 스크립트가 exact `X.Y.Z` 인자를 필수로 받고 VERSION 파일 fallback을 제거한다.
- Acceptance: 인자 누락 또는 잘못된 SemVer는 Gradle, Docker, registry, environment repository 변경 전에 실패한다.
- Acceptance: 유효 버전 dry-run은 기존 `batch_X.Y.Z` 태그, 선택 환경, signing 옵션을 보존한다.
- Verification: `bash -n .agents/skills/deploy-batch/scripts/{build-image,push-image,update-kustomize}.sh .claude/skills/deploy-batch/scripts/{build-image,push-image,update-kustomize}.sh`
- Verification: 세 스크립트의 인자 누락·invalid version 실패와 `1.0.7 --dry-run` 정상 출력을 실행 확인
- Files: `.agents/skills/deploy-batch/scripts/build-image.sh`, `.agents/skills/deploy-batch/scripts/push-image.sh`, `.agents/skills/deploy-batch/scripts/update-kustomize.sh`, `.claude/skills/deploy-batch/scripts/build-image.sh`, `.claude/skills/deploy-batch/scripts/push-image.sh`, `.claude/skills/deploy-batch/scripts/update-kustomize.sh`
- Size: M
- Status: [x] done

### Task 3: 로컬 deploy-batch 선택 흐름 전환

- Acceptance: prerequisite는 VERSION 파일 검사를 제거하고 최신 태그 조회에 필요한 initialized environment repository와 Git 조건을 검증한다.
- Acceptance: 스킬은 최신 두 환경의 최댓값과 patch 추천값을 먼저 제시한 뒤 추천값 선택 또는 기준보다 큰 minor/major exact version 입력을 받는다.
- Acceptance: 모든 실행 예시가 선택된 exact version을 build, push, kustomize update에 명시적으로 전달하고 기존 환경·서명 선택 흐름을 유지한다.
- Verification: `bash -n .agents/skills/deploy-batch/scripts/check-prerequisites.sh .claude/skills/deploy-batch/scripts/check-prerequisites.sh`
- Verification: `rg -n "VERSION|check-version|bump-version|build-image|push-image|update-kustomize" .agents/skills/deploy-batch .claude/skills/deploy-batch`
- Files: `.agents/skills/deploy-batch/SKILL.md`, `.agents/skills/deploy-batch/scripts/check-prerequisites.sh`, `.claude/skills/deploy-batch/SKILL.md`, `.claude/skills/deploy-batch/scripts/check-prerequisites.sh`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-3

- [x] 모든 변경 shell script의 `bash -n` 통과
- [x] 추천 patch와 exact minor/major 선택 시나리오 통과
- [x] VERSION 파일 read/write 참조가 로컬 deploy-batch 스킬과 스크립트에서 제거됨
- [x] dry-run에서 registry push 및 environment repository 변경이 발생하지 않음

### Task 4: workflow dispatch를 exact version source로 전환

- Acceptance: `deploy_batch.yml`은 required version input을 exact `X.Y.Z`로 검증하여 `batch_X.Y.Z`를 만들고 `bottlenote-batch/VERSION`을 읽지 않는다.
- Acceptance: prepare와 push 직전의 immutable tag 중복 차단, production main 제한·approval, signing, GitOps 흐름을 유지한다.
- Acceptance: GitOps 실패 및 summary 안내는 VERSION 파일 증가 대신 새 exact version 입력 후 재실행을 지시하며 `bottlenote-batch/VERSION` 파일은 삭제된다.
- Verification: `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy_batch.yml", aliases: true)'`
- Verification: `rg -n "inputs.version|INPUT_VERSION|batch_|image tag already exists|production batch deployment" .github/workflows/deploy_batch.yml && ! rg -n "bottlenote-batch/VERSION" .github/workflows/deploy_batch.yml`
- Files: `.github/workflows/deploy_batch.yml`, `bottlenote-batch/VERSION` 삭제
- Size: S
- Status: [ ] not done

### Task 5: 버전 정책 회귀 guard 추가

- Acceptance: 실제 임시 Git remote/overlay fixture를 사용하는 shell 검증이 동일 태그, 서로 다른 태그, invalid/missing tag, exact candidate 경계값을 재현한다.
- Acceptance: 검증은 VERSION 참조 부재, workflow 필수 input/immutable guard, 모든 shell syntax를 확인하고 외부 registry나 운영 environment repository를 변경하지 않는다.
- Acceptance: `.agents/skills/deploy-batch`와 `.claude/skills/deploy-batch`의 대응 파일 목록 및 SHA-256 불일치가 있으면 실패한다.
- Verification: `.agents/skills/deploy-batch/scripts/test-version-policy.sh`
- Verification: `.claude/skills/deploy-batch/scripts/test-version-policy.sh`
- Files: `.agents/skills/deploy-batch/scripts/test-version-policy.sh`, `.claude/skills/deploy-batch/scripts/test-version-policy.sh`
- Size: S
- Status: [ ] not done

### Checkpoint: after Tasks 4-5

- [ ] workflow YAML parse 및 exact version 정적 계약 검증 통과
- [ ] immutable tag 차단 로직이 prepare/push 두 위치에 유지됨
- [ ] VERSION 파일과 전체 참조가 제거됨
- [ ] `.agents`/`.claude` 대응 파일 목록 및 SHA-256 일치
- [ ] `git diff --check` 통과 및 계획 밖 변경 없음

## Progress Log

- Task 1: `check-version.sh`를 최신 environment repository `origin/main`의 development/production batch 태그를 숫자 비교하는 도구로 전환했다. `1.0.6 / 1.0.6`에서 `1.0.7` 추천, `1.1.0` exact 후보 통과, 기준값 이하와 prerelease 후보 실패를 확인했다. `bump-version.sh`는 명시적 기준값만 비파괴적으로 증가시키며 patch/minor/major 결과 `1.0.7`, `1.1.0`, `2.0.0`을 확인했다. 네 스크립트의 `bash -n`, ShellCheck, `.agents`/`.claude` mode 및 content 일치를 확인했다.
- Task 2: build, push, kustomize update의 VERSION fallback을 제거하고 exact version positional을 필수화했다. `.agents`와 `.claude`의 여섯 스크립트에서 누락·prerelease 입력이 외부 동작 전에 실패하고, `1.0.7 --dry-run`이 기존 `batch_1.0.7` 태그와 환경 출력을 유지함을 확인했다. `bash -n`, ShellCheck, mirror SHA-256 일치를 확인했다.
- Task 3: prerequisite에 Git 및 initialized environment repository 검사를 추가하고 VERSION 파일 검사를 제거했다. deploy-batch 스킬은 최신 두 환경 태그의 최댓값과 patch 추천값을 제시하고, 선택한 exact version을 재검증한 뒤 build/push/kustomize 모든 단계에 전달하도록 갱신했다. 현재 로컬 사전조건 9개 통과, obsolete VERSION 파일 참조 0건, 두 복제본 SHA-256 일치를 확인했다.
