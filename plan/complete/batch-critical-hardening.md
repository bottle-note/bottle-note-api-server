# Plan: batch Critical hardening

Status: **COMPLETED**

## Overview

`bottlenote-batch` 모듈의 즉시 조치 대상 Critical 항목을 먼저 줄인다.

현재 배치 모듈은 실행 JAR 리소스에 루트 `git.environment-variables` 디렉터리 전체를 포함하고, main 설정에 JWT secret-key와 nonce salt 기본값을 직접 가지고 있다. 이 구조는 SQL 리소스 로딩 편의와 `mono` 호환성 때문에 생긴 것으로 보이지만, 배치 실행 산출물에 운영/배포/secret 성격의 파일이 함께 들어가거나 기본 secret이 그대로 사용될 수 있는 위험이 있다.

이번 define의 목표는 구현 범위를 `batch resource include 범위 축소`와 `batch JWT secret/salt 기본값 제거`로 고정하고, 다음 `/plan` 단계에서 작고 검증 가능한 태스크로 나눌 수 있게 성공 기준을 명확히 하는 것이다.

### Assumptions

- 대상 모듈은 `bottlenote-batch`다.
- 이번 작업은 기술 부채 감사 문서의 Phase 1 남은 batch Critical 항목 2개만 다룬다.
- `git.environment-variables` 자체 내용 수정, SOPS 재암호화, 운영 secret 변경, DB schema/migration은 이번 범위가 아니다.
- batch job이 로드하는 SQL 리소스는 계속 classpath에서 접근 가능해야 한다.
- main 런타임 설정에서는 JWT secret-key와 nonce salt의 하드코딩 기본값을 제거한다.
- test 리소스에서는 컨텍스트 구동에 필요한 테스트용 값은 유지할 수 있다.
- batch security 의존성 제거 여부는 구현 중 확인하되, 이번 define의 필수 목표는 기본 secret 제거다.
- batch component scan 축소는 관련 리스크지만 이번 계획에서는 제외한다.
- 기존 uncommitted 변경인 `git.environment-variables`, `plan/bottlenote-skill-refinements.md`, `.agents/.claude` reference 파일은 보호한다.

### Success Criteria

- `bottlenote-batch` main resources가 더 이상 루트 `git.environment-variables` 전체를 classpath에 포함하지 않는다.
- batch job에서 필요한 SQL 리소스 `storage/mysql/sql/popularity.sql`, `storage/mysql/sql/best-review-selected.sql`는 변경 후에도 classpath에서 로드 가능하다.
- batch 실행 JAR 또는 `processResources` 산출물에 deploy, kubeconfig, cosign, SOPS env, application.* 외부 환경 파일이 포함되지 않는 것을 검증한다.
- `bottlenote-batch/src/main/resources/application.yml`의 `security.jwt.secret-key`와 `security.nonce.salt`는 하드코딩 값이 아니라 환경 변수 참조 또는 명시적 필수 설정 방식으로 바뀐다.
- 테스트 전용 설정은 필요한 경우 `bottlenote-batch/src/test/resources/application.yml`에 테스트 값으로 남긴다.
- 변경 후 `./gradlew :bottlenote-batch:processResources` 또는 동등한 리소스 패키징 검증이 통과한다.
- 변경 후 batch 관련 컴파일 또는 테스트 검증이 통과한다. 배치 테스트가 없으면 그 사실을 명시하고 최소 컴파일/리소스 검증을 증거로 남긴다.

### Impact Scope

- `bottlenote-batch/build.gradle`
  - main/test resource source set 범위 조정
  - 필요한 SQL 리소스만 포함하는 방식 검토
- `bottlenote-batch/src/main/resources/application.yml`
  - JWT secret-key와 nonce salt 기본값 제거
- `bottlenote-batch/src/test/resources/application.yml`
  - 테스트 구동용 security 값 유지 또는 조정
- `bottlenote-batch/src/main/java/app/batch/bottlenote/job/ranking/*JobConfig.java`
  - SQL 리소스 classpath 경로 유지 여부 확인
- `git.environment-variables/storage/mysql/sql/*.sql`
  - 읽기 대상 리소스. 내용 수정은 범위 밖
- `bottlenote-mono`
  - security property를 소비하는 bean이 batch context에서 필요한지 확인 대상
- 테스트/검증
  - batch resource packaging 검증
  - batch compile 또는 context smoke 검증

### Non-Goals

- batch component scan 범위 축소
- observability 의존성 경계 정리
- admin ROOT_ADMIN 기본값 제거
- admin RBAC 정책 추가
- AWS SDK Java v2 전환
- DB 테이블 변경 또는 운영 데이터 변경
- `git.environment-variables` 파일 내용 수정

### Open Questions

- SQL 리소스는 `git.environment-variables/storage/mysql/sql`에서 계속 읽을지, batch 모듈 내부 리소스로 복사할지 `/plan` 단계에서 선택해야 한다.
- main 설정에서 secret 누락 시 Spring placeholder 단계에서 fail-fast하게 할지, 별도 `@ConfigurationProperties` 검증으로 fail-fast하게 할지 `/plan` 단계에서 선택해야 한다.

### Approval Gate

이 문서는 `/define` 단계 산출물이다. 위 가정, 성공 기준, 제외 범위가 맞으면 다음 단계에서 `/plan`으로 태스크를 작성한다.

## Dependency Analysis

구현 순서는 resource packaging foundation을 먼저 고정한 뒤, main runtime secret 기본값을 제거하고, 마지막에 산출물/컴파일 검증으로 묶는다.

- Foundation: `bottlenote-batch/build.gradle`에서 루트 `git.environment-variables` 전체 resource include를 제거하고, 두 SQL 파일만 `storage/mysql/sql/**` classpath 경로로 복사되도록 제한한다.
- Configuration: `bottlenote-batch/src/main/resources/application.yml`에서 JWT secret-key와 nonce salt 기본값을 제거한다. 테스트용 값은 `src/test/resources/application.yml`에 남긴다.
- Verification: `processResources` 산출물과 batch compile/test 명령으로 SQL 리소스 존재와 외부 환경 파일 미포함을 확인한다.

계획 단계의 선택:

- SQL 리소스는 원본을 `git.environment-variables/storage/mysql/sql`에 유지하고, Gradle `processResources`/`processTestResources`에서 필요한 파일만 `storage/mysql/sql` 경로로 복사한다.
- main secret 누락은 Spring placeholder 해석 단계에서 fail-fast하도록 기본값 없는 환경 변수 참조를 사용한다.

## Tasks

### Task 1: Limit batch packaged resources

- Acceptance: `bottlenote-batch` main/test resource packaging이 루트 `git.environment-variables` 전체를 `srcDir`로 포함하지 않는다.
- Acceptance: `storage/mysql/sql/popularity.sql`와 `storage/mysql/sql/best-review-selected.sql`는 `processResources` 산출물에서 기존 classpath 경로로 존재한다.
- Acceptance: `git.environment-variables`의 `application.*`, deploy, kubeconfig, cosign, SOPS env 성격 파일은 `bottlenote-batch/build/resources/main`에 포함되지 않는다.
- Verification: `./gradlew :bottlenote-batch:processResources :bottlenote-batch:processTestResources`
- Verification: `find bottlenote-batch/build/resources/main -type f | sort`
- Files: `bottlenote-batch/build.gradle`
- Size: S
- Status: [x] done

### Task 2: Remove main secret defaults

- Acceptance: `bottlenote-batch/src/main/resources/application.yml`의 `security.jwt.secret-key`는 하드코딩 문자열이 아니라 기본값 없는 환경 변수 참조다.
- Acceptance: `bottlenote-batch/src/main/resources/application.yml`의 `security.nonce.salt`는 하드코딩 문자열이 아니라 기본값 없는 환경 변수 참조다.
- Acceptance: `bottlenote-batch/src/test/resources/application.yml`에는 테스트 컨텍스트 구동을 위한 테스트 전용 값이 유지된다.
- Verification: `rg -n "secure-salt|c2VjdXJl|JWT_SECRET|NONCE" bottlenote-batch/src/main/resources/application.yml bottlenote-batch/src/test/resources/application.yml`
- Verification: `./gradlew :bottlenote-batch:compileJava`
- Files: `bottlenote-batch/src/main/resources/application.yml`, `bottlenote-batch/src/test/resources/application.yml`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-2

- [x] `./gradlew :bottlenote-batch:processResources :bottlenote-batch:processTestResources`
- [x] `./gradlew :bottlenote-batch:compileJava`
- [x] Resource output contains only expected SQL files from `git.environment-variables`
- [x] Main batch config has no hardcoded JWT secret-key or nonce salt

### Task 3: Add resource packaging regression check

- Acceptance: resource packaging 검증을 반복 가능한 형태로 남겨, batch JAR/resource output에 외부 환경 파일이 섞이는 회귀를 빠르게 확인할 수 있다.
- Acceptance: 검증은 운영 DB, secret 복호화, 배포 시스템에 접근하지 않는다.
- Acceptance: 검증 실패 시 누락된 SQL 또는 금지 파일 포함 여부가 출력으로 드러난다.
- Verification: `./gradlew :bottlenote-batch:processResources`
- Verification: `test -f bottlenote-batch/build/resources/main/storage/mysql/sql/popularity.sql`
- Verification: `test -f bottlenote-batch/build/resources/main/storage/mysql/sql/best-review-selected.sql`
- Files: `bottlenote-batch/build.gradle` 또는 `bottlenote-batch/src/test/java/**`
- Size: S
- Status: [x] done

### Task 4: Run final batch verification

- Acceptance: batch 리소스 패키징 검증이 통과한다.
- Acceptance: batch 컴파일 또는 batch 테스트 검증이 통과한다.
- Acceptance: 배치 테스트가 없거나 환경 제약으로 실행하지 못한 검증은 완료 보고에 명시한다.
- Verification: `./gradlew :bottlenote-batch:processResources :bottlenote-batch:compileJava`
- Verification: `./gradlew :bottlenote-batch:batch_test`
- Verification: `git diff -- bottlenote-batch/build.gradle bottlenote-batch/src/main/resources/application.yml bottlenote-batch/src/test/resources/application.yml`
- Files: no production file expected unless prior tasks expose a small correction
- Size: S
- Status: [x] done

## Progress Log

- 2026-05-14 `/implement`: isolated worktree created at `/Users/hgkim/.config/superpowers/worktrees/bottle-note-api-server/batch-critical-hardening` on branch `codex/batch-critical-hardening`.
- 2026-05-14 `/implement`: Task 1-3 code changes applied to `bottlenote-batch/build.gradle` and `bottlenote-batch/src/main/resources/application.yml`.
- 2026-05-14 `/implement`: Initial Gradle verification used default Corretto `25.0.1` and failed while configuring `:bottlenote-admin-api`. User pointed out SDKMAN JDK 21 at `/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn`; verification continued with explicit `JAVA_HOME`/`PATH`.
- 2026-05-14 `/implement`: `env JAVA_HOME=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn PATH=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn/bin:$PATH ./gradlew :bottlenote-batch:verifyBatchPackagedResources` passed.
- 2026-05-14 `/implement`: `env JAVA_HOME=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn PATH=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn/bin:$PATH ./gradlew :bottlenote-batch:compileJava` passed.
- 2026-05-14 `/implement`: `env JAVA_HOME=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn PATH=/Users/hgkim/.sdkman/candidates/java/21.0.8-amzn/bin:$PATH ./gradlew :bottlenote-batch:batch_test` passed with `NO-SOURCE`.
- 2026-05-14 `/implement`: `git diff --check` passed; `find bottlenote-batch/build/resources/main -maxdepth 4 -type f | sort` showed only batch module application resources plus the two expected SQL files.
- 2026-05-14 `/self-review`: no Critical or Important findings. Main/test resource outputs contain the two expected SQL files; forbidden `git.environment-variables` paths were not found in `bottlenote-batch/build/resources/main` or `bottlenote-batch/build/resources/test`.
