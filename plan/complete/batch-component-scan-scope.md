# Plan: batch component scan 축소

Status: **COMPLETED**

## Overview

`bottlenote-batch`의 `BatchApplication`은 현재 `@SpringBootApplication(scanBasePackages = "app")`, `@ComponentScan(basePackages = "app")`, `@EntityScan(basePackages = "app")`로 전체 `app` 패키지를 스캔한다. 이 범위는 batch 실행에 필요하지 않은 product/admin 성격의 bean, mono 내부 서비스, external 설정, observability component까지 batch application context에 로딩될 수 있게 만든다.

이 작업은 batch application bootstrap의 component scan 범위를 명시적으로 줄여, batch job 실행에 필요한 bean만 로딩하도록 만드는 것을 목표로 한다. 구현 단계에서는 batch job 3종이 계속 기동되고, context startup이 성공하며, 필요 없는 bean이 로딩되지 않는지 테스트로 확인한다.

### Assumptions

- 이 작업의 대상은 `bottlenote-batch`의 application bootstrap과 그에 필요한 최소 mono/external bean wiring이다.
- product-api, admin-api의 application scan 정책은 이번 작업 범위가 아니다.
- batch job 자체의 비즈니스 로직, cron, SQL, 리소스 패키징 정책은 변경하지 않는다.
- `git.environment-variables` 서브모듈 포인터 변경은 사용자/환경 변경으로 간주하고 건드리지 않는다.
- daily report job은 현재 `DailyDataReportService`, `WebhookConfig`, `DiscordWebhookProperties`, `AppInfoConfig`가 필요하므로 이 의존성을 유지해야 한다.
- ranking job 2종은 주로 `JdbcTemplate`, `JobRepository`, `PlatformTransactionManager`, SQL classpath resource에 의존하므로 mono 전체 서비스 스캔이 필요하지 않다고 본다.
- JPA entity/repository 스캔 범위 축소는 component scan 축소와 분리해서 판단한다. 단, batch context 안정성에 직접 필요하면 최소 범위 조정을 포함할 수 있다.
- batch context 검증 테스트는 `@Tag("batch")` 또는 프로젝트 관습에 맞는 batch 전용 테스트로 작성한다.

### Success Criteria

- `BatchApplication`에서 전체 `app` component scan을 제거하거나, batch에 필요한 패키지 중심의 명시적 스캔 목록으로 대체한다.
- `bottlenote-batch` application context가 test profile에서 정상 기동한다.
- batch job bean 3종이 계속 등록된다: `bestReviewSelectedJob`, `popularAlcoholJob`, `dailyDataReportJob`.
- Quartz job binding 3종이 계속 등록된다: `BestReviewQuartzJob`, `PopularAlcoholQuartzJob`, `DailyDataReportQuartzJob`.
- daily report에 필요한 `DailyDataReportService`, `webhookRestTemplate`, `DiscordWebhookProperties`, `AppInfoConfig` 의존성이 깨지지 않는다.
- product/admin 전용 bean 또는 batch에 불필요한 대표 bean이 batch context에 로딩되지 않음을 테스트로 확인한다.
- `./gradlew :bottlenote-batch:compileJava :bottlenote-batch:compileTestJava`가 통과한다.
- `./gradlew :bottlenote-batch:batch_test` 또는 batch 전용 context 검증 테스트 실행 명령이 통과한다.
- 필요 시 전체 회귀로 `./gradlew build -x test -x asciidoctor --build-cache --parallel`까지 통과한다.

### Impact Scope

- Primary module: `bottlenote-batch`
- Likely files:
  - `bottlenote-batch/src/main/java/app/batch/bottlenote/BatchApplication.java`
  - `bottlenote-batch/src/main/java/app/batch/bottlenote/config/*`
  - `bottlenote-batch/src/main/java/app/batch/bottlenote/job/**/*`
  - `bottlenote-batch/src/test/java/**` 신규 context 검증 테스트
  - `bottlenote-batch/src/test/resources/application.yml`
- Neighbor modules/packages:
  - `bottlenote-mono/src/main/java/app/bottlenote/support/report/service/DailyDataReportService.java`
  - `bottlenote-mono/src/main/java/app/external/webhook/config/*`
  - `bottlenote-mono/src/main/java/app/external/version/config/AppInfoConfig.java`
  - `bottlenote-mono/src/main/java/app/bottlenote/global/config/jpa/*`
- Persistence:
  - DB schema 변경 없음.
  - SQL resource 변경 없음.
  - JPA repository/entity scan은 구현 설계에서 최소 조정 여부만 검토한다.
- Async/events/cache:
  - 신규 event, async flow, cache invalidation 없음.
- External integration:
  - Discord webhook 설정과 `RestTemplate` bean wiring은 유지해야 한다.
- Tests:
  - batch application context startup test가 필요하다.
  - 대표적인 불필요 bean 부재 검증이 필요하다.
  - ranking/daily report job bean presence 검증이 필요하다.
- Docs/API contracts:
  - 외부 API 계약 변경 없음.
  - 필요하면 기술 부채 감사 문서에 완료 상태만 반영한다.

### Approach Options

- Recommended: `BatchApplication`의 component scan을 `app.batch.bottlenote`와 batch가 실제 사용하는 mono/external package로 명시적으로 제한한다. 변경 범위가 작고 현재 구조를 유지하면서 불필요한 bean 로딩을 줄일 수 있다.
- Alternative: batch 전용 configuration class를 만들고 필요한 mono/external bean만 `@Import`한다. 가장 명확하지만, 현재 mono 설정이 넓게 묶여 있어 초기 작업량이 커질 수 있다.
- Deferred: mono의 report/webhook 기능을 batch 전용 adapter로 분리한다. 구조적으로 가장 깔끔하지만 별도 리팩토링 성격이 강하므로 이번 부채 처리 범위를 넘을 수 있다.

## Approval Gate

위 가정과 성공 기준 승인 후 `/plan` 단계에서 구현 태스크를 분해한다. 이 문서는 아직 구현 계획이 아니며, 현재 단계에서는 코드 변경을 수행하지 않는다.

## Tasks

### Task 1: batch scan dependency map 고정
- Acceptance: batch context에 반드시 필요한 package/bean 목록과 제외해야 할 대표 bean 목록을 테스트 기준으로 확정한다.
- Verification: `rg -n "scanBasePackages|@ComponentScan|@Configuration|@Component|@Service" bottlenote-batch bottlenote-mono/src/main/java/app/bottlenote/support/report bottlenote-mono/src/main/java/app/external`
- Files: `plan/batch-component-scan-scope.md`
- Size: S
- Status: [x] done

### Task 2: BatchApplication scan 범위 축소
- Acceptance: `BatchApplication`에서 broad `app` component scan이 제거되고 batch 실행에 필요한 package만 명시된다.
- Verification: `./gradlew :bottlenote-batch:compileJava`
- Files: `bottlenote-batch/src/main/java/app/batch/bottlenote/BatchApplication.java`
- Size: S
- Status: [x] done

### Task 3: 필요한 mono/external wiring 유지
- Acceptance: daily report job에 필요한 `DailyDataReportService`, `WebhookConfig`, `DiscordWebhookProperties`, `AppInfoConfig` wiring이 유지된다.
- Verification: `./gradlew :bottlenote-batch:compileJava :bottlenote-batch:compileTestJava`
- Files: `bottlenote-batch/src/main/java/app/batch/bottlenote/BatchApplication.java`, 필요 시 batch 전용 config 1개
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-3
- [x] `./gradlew :bottlenote-batch:compileJava :bottlenote-batch:compileTestJava` 통과
- [x] broad component scan 제거 여부 확인
- [x] product/admin application code 변경 없음 확인

### Task 4: batch context startup 테스트 추가
- Acceptance: test profile에서 batch application context가 기동되고 batch job bean 3종 및 Quartz binding 3종이 등록됨을 검증한다.
- Verification: `./gradlew :bottlenote-batch:batch_test`
- Files: `bottlenote-batch/src/test/java/**`, 필요 시 `bottlenote-batch/src/test/resources/application.yml`
- Size: M
- Status: [x] done

### Task 5: 불필요 bean 미로딩 테스트 추가
- Acceptance: product/admin 전용 또는 batch와 무관한 대표 bean이 batch context에 등록되지 않음을 검증한다.
- Verification: `./gradlew :bottlenote-batch:batch_test`
- Files: `bottlenote-batch/src/test/java/**`
- Size: S
- Status: [x] done

### Task 6: 회귀 검증 및 문서 상태 정리
- Acceptance: batch compile/test와 lightweight build 검증 결과를 Progress Log에 남기고, 필요 시 기술 부채 문서 완료 반영 범위를 정리한다.
- Verification: `./gradlew :bottlenote-batch:batch_test` 및 `./gradlew build -x test -x asciidoctor --build-cache --parallel`
- Files: `plan/batch-component-scan-scope.md`, 필요 시 기술 부채 감사 문서
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 4-6
- [x] `./gradlew :bottlenote-batch:batch_test` 통과
- [x] `./gradlew build -x test -x asciidoctor --build-cache --parallel` 통과
- [x] `git status --short`에서 AWS SDK v2 작업 및 `git.environment-variables` 변경을 침범하지 않았는지 확인

## Progress Log

- Task 1: `rg`로 batch scan dependency를 확인했다. 필요한 bean은 batch job/config, `DailyDataReportService`, `WebhookConfig`, `DiscordWebhookProperties`, `AppInfoConfig`로 고정했고, 제외 대표 bean은 `OauthService`, `AdminUserService`, `ReviewService`, `UserReportService`, `ReviewReportService`로 잡았다.
- Task 2: `BatchApplication`의 broad `app` scan과 중복 `@ComponentScan`을 제거하고 `app.batch.bottlenote` component scan으로 축소했다.
- Task 3: daily report wiring은 package scan 대신 `@Import({DailyDataReportService.class, WebhookConfig.class, AppInfoConfig.class})`로 명시했다.
- Task 4: `BatchApplicationContextTest`를 추가해 batch job 3종과 Quartz binding 3종 등록을 검증했다.
- Task 5: 동일 context 테스트에서 product/admin 성격의 mono bean 5종이 로드되지 않는지 검증했다.
- Task 6: `./gradlew :bottlenote-batch:compileJava` 통과, `./gradlew :bottlenote-batch:compileTestJava` 통과, `./gradlew :bottlenote-batch:batch_test` 통과, `./gradlew build -x test -x asciidoctor --build-cache --parallel` 통과. worktree의 `git.environment-variables` 서브모듈은 초기화하지 않았고, 테스트용 SQL 리소스만 `src/test/resources`에 추가했다.
