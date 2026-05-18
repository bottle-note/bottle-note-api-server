# Plan: Curation V2 Test Hardening

## Overview

큐레이션 v2의 현재 코드베이스 로직을 테스트로 더 명확하게 고정한다.

이번 작업은 신규 기능 구현이 아니라 테스트 보강이다. 특히 사용자가 설계한 핵심 정책인 "큐레이션 아이템은 저장 시점의 메타 정보를 payload로 스냅샷 저장하고, 현재성이 필요한 통계만 GraphQL로 보강한다"는 동작을 테스트명과 assertion으로 직접 표현한다.

테스트 보강은 에이전트 역할을 분리해서 진행한다. 구현 에이전트가 테스트를 추가하고, 목적 검증 에이전트가 테스트가 실제 의도를 검증하는지 반례까지 확인하며, 컨벤션 검토 에이전트가 기존 테스트 방식과 프로젝트 규칙을 점검한다. 마지막으로 회귀 검증 에이전트가 focused Gradle suite를 실행해 실제 통과 여부를 확인한다.

### Assumptions

- 대상은 spec 기반 큐레이션 v2만이다. legacy `curation_keyword` 테스트는 이번 범위가 아니다.
- 현재 canonical endpoint는 Admin `/admin/api/v2/curation-specs`, `/admin/api/v2/curations`, Product `/api/v2/curations`다.
- `source: BOTTLE_NOTE(내부 알코올 참조)`는 `alcoholId`를 통해 현재 통계를 GraphQL로 보강할 수 있는 아이템이다.
- `source: MANUAL(직접 입력)`은 내부 알코올 조회 대상이 아니며 저장된 payload 그대로 응답하고 `stats`는 `null`이다.
- 이름, 이미지, 태그, 코멘트 같은 노출용 메타 정보는 저장 시점 payload의 스냅샷을 사용한다.
- 별점, 평가 수, 리뷰 수, 찜 수 같은 현재성 있는 통계만 Product 상세 조회 시 GraphQL hydration으로 보강한다.
- Unit test는 fake, in-memory repository, fake GraphQL executor를 우선 사용한다.
- E2E/Integration test는 실제 Spring context, JPA, TestContainers가 필요한 검증에만 사용한다.
- 기존 미커밋 변경인 Admin Asciidoc, display demo plan/runbook, `git.environment-variables` gitlink 변경은 보존한다.

### Success Criteria

- `source: BOTTLE_NOTE(내부 알코올 참조)` 항목은 저장된 메타 정보를 유지하고 현재 통계만 GraphQL로 보강한다는 테스트가 추가된다.
- `source: MANUAL(직접 입력)` 항목은 GraphQL 조회 대상에서 제외되고 `stats=null`로 응답한다는 테스트가 명확히 유지되거나 보강된다.
- 알코올 원본 정보가 변경된 뒤 Product v2 상세를 조회해도 큐레이션 payload의 저장 시점 메타 정보가 응답된다는 E2E/Integration 테스트가 추가된다.
- 3개 스펙 `RECOMMENDED_WHISKY`, `WHISKY_PAIRING`, `WHISKY_TASTING_EVENT`에 대해 유효 payload 저장 또는 materialized response 검증이 보강된다.
- Admin v2 무인증 접근 또는 존재하지 않는 spec/curation 같은 API 경계 오류가 필요한 범위에서 보강된다.
- 테스트 이름은 가능한 한 `~할 경우 ~한다` 형태를 사용하고, source 타입은 `source: BOTTLE_NOTE(내부 알코올 참조)`, `source: MANUAL(직접 입력)`처럼 괄호 설명을 포함한다.
- Parameterized test가 중복을 줄일 수 있는 스펙별 검증에는 `@ParameterizedTest`를 우선 고려한다.
- 추가 테스트는 기존 tag 규칙을 따른다: unit은 `@Tag("unit")`, product integration은 `@Tag("integration")`, admin integration은 `@Tag("admin_integration")`.
- focused verification이 성공한다:
  - `./gradlew :bottlenote-mono:test --tests 'app.bottlenote.curation.*' --tests 'app.bottlenote.graphql.GraphQLCurationSchemaTest'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'`
  - `./gradlew :bottlenote-product-api:integration_test --tests 'app.bottlenote.curation.integration.ProductSpecBasedCurationIntegrationTest'`
  - 필요 시 관련 RestDocs 테스트와 `git diff --check`

### Impact Scope

- `bottlenote-mono`
  - `CurationResponseMaterializerTest`
  - `CurationPayloadValidatorTest`
  - `ProductSpecBasedCurationServiceTest`
  - `AdminSpecBasedCurationServiceTest`
  - `GraphQLCurationAlcoholResolverTest`
  - curation fixture/fake repository
- `bottlenote-product-api`
  - `ProductSpecBasedCurationIntegrationTest`
  - 필요 시 Product curation RestDocs test
- `bottlenote-admin-api`
  - `AdminSpecBasedCurationIntegrationTest`
  - 필요 시 Admin spec-based curation RestDocs test
- Persistence
  - schema 변경 없음.
  - 테스트 데이터만 추가한다.
- API Contract
  - endpoint 변경 없음.
  - 응답 형태 변경 없음.
- Docs
  - 테스트 보강 자체에는 API 문서 변경이 필수는 아니다.
  - 기존 문서 diff는 보존한다.

### Agent Roles

- Implementation Agent
  - 테스트 코드를 추가한다.
  - 기존 fixture, fake, TestFactory를 우선 재사용한다.
  - production code 변경은 테스트 불가능성이 확인된 경우에만 별도 보고한다.

- Test Intent Review Agent
  - 각 테스트가 이름 그대로의 목적을 검증하는지 확인한다.
  - `A일 때 X다`뿐 아니라 `B일 때 X가 아니다`에 해당하는 반례 assertion이 필요한지 점검한다.
  - 스냅샷 메타 정보와 GraphQL 통계 보강이 같은 assertion에 섞여 흐려지지 않았는지 확인한다.

- Test Convention Review Agent
  - 기존 테스트 패턴, tag, DisplayName, fake/in-memory 우선 원칙, TestContainers 사용 기준을 검토한다.
  - 불필요한 mock, 과도한 통합 테스트, 기존 fixture 중복 생성을 반려한다.

- Regression Verification Agent
  - focused Gradle suite를 실행한다.
  - 실패 시 compile/test/docs/integration/TestContainers 문제를 분리해 보고한다.
  - 성공 주장 전 실제 `BUILD SUCCESSFUL` 또는 실패 원인을 확인한다.

### Out of Scope

- 큐레이션 v2 production 로직 변경
- DB schema 변경
- legacy `curation_keyword` 동작 변경
- 신규 큐레이션 스펙 추가
- Admin/Product endpoint 변경
- display demo UI 구현 변경

## Runtime Boundary

이 문서는 `/define` 산출물이다. 다음 단계는 `/plan`에서 테스트 보강 task를 쪼개는 것이다.

## Dependency Analysis

1. Unit 테스트는 production context 없이 빠르게 정책을 고정한다. 특히 `CurationResponseMaterializer`와 `CurationPayloadValidator`가 Product 상세 응답의 payload 형태를 결정하므로 먼저 보강한다.
2. Product integration 테스트는 Admin 저장 경로, JPA persistence, Product 조회, 내부 GraphQL hydration까지 연결되는 핵심 E2E 경로다. 저장 시점 스냅샷 정책은 이 레이어에서 가장 명확하게 검증한다.
3. Admin integration 테스트는 v2 endpoint의 인증/오류 경계를 고정한다. Product integration과 독립적으로 구현 가능하다.
4. 리뷰 에이전트는 구현 후 병렬로 목적/컨벤션을 검토한다. 회귀 검증은 리뷰 반영 뒤 수행한다.

## Tasks

### Task 1: Unit materializer source policy hardening

- Acceptance: `source: BOTTLE_NOTE(내부 알코올 참조)`는 저장된 메타 정보를 유지하고 stats만 GraphQL 결과로 보강한다.
- Acceptance: `source: MANUAL(직접 입력)`은 GraphQL 변수에서 제외되고 stats를 null로 유지한다.
- Acceptance: 중복 `alcoholId`는 GraphQL 변수에서 중복 제거된다.
- Verification: `./gradlew :bottlenote-mono:test --tests 'app.bottlenote.curation.service.CurationResponseMaterializerTest'`
- Files: `bottlenote-mono/src/test/java/app/bottlenote/curation/service/CurationResponseMaterializerTest.java`
- Size: S
- Status: [x] done

### Task 2: Spec parameterized validation coverage

- Acceptance: `RECOMMENDED_WHISKY`, `WHISKY_PAIRING`, `WHISKY_TASTING_EVENT` 유효 request payload가 모두 requestSpec 검증을 통과한다.
- Acceptance: 세 스펙의 대표 materialized payload가 responseSpec 검증을 통과한다.
- Verification: `./gradlew :bottlenote-mono:test --tests 'app.bottlenote.curation.service.CurationPayloadValidatorTest'`
- Files: `bottlenote-mono/src/test/java/app/bottlenote/curation/service/CurationPayloadValidatorTest.java`
- Size: S
- Status: [x] done

### Task 3: Product integration snapshot semantics

- Acceptance: Product v2에서 알코올 원본 정보가 변경된 뒤 조회할 경우 큐레이션 payload의 저장 시점 메타 정보로 응답한다.
- Acceptance: 같은 응답에서 현재 통계는 GraphQL hydration 결과로 보강된다.
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests 'app.bottlenote.curation.integration.ProductSpecBasedCurationIntegrationTest'`
- Files: `bottlenote-product-api/src/test/java/app/bottlenote/curation/integration/ProductSpecBasedCurationIntegrationTest.java`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-3

- [x] Mono curation unit tests pass
- [x] Product curation integration tests pass
- [x] Snapshot semantics are asserted by both unit or integration coverage

### Task 4: Admin integration boundary coverage

- Acceptance: Admin v2에서 존재하지 않는 specId로 생성할 경우 404를 반환한다.
- Acceptance: Admin v2에서 존재하지 않는 curationId로 수정할 경우 404를 반환한다.
- Acceptance: Admin v2에서 인증 없이 `/v2/curation-specs` 또는 `/v2/curations`를 요청할 경우 현재 보안 설정에 맞는 4xx를 반환한다.
- Verification: `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'`
- Files: `bottlenote-admin-api/src/test/kotlin/app/integration/curation/AdminSpecBasedCurationIntegrationTest.kt`
- Size: S
- Status: [x] done

### Task 5: Agent review and focused verification

- Acceptance: Test Intent Review Agent가 테스트 목적과 반례 assertion을 검토한다.
- Acceptance: Test Convention Review Agent가 tag, fixture, fake/TestContainers 사용 기준을 검토한다.
- Acceptance: Focused Gradle suite와 `git diff --check`가 성공한다.
- Verification:
  - `./gradlew :bottlenote-mono:test --tests 'app.bottlenote.curation.*' --tests 'app.bottlenote.graphql.GraphQLCurationSchemaTest'`
  - `./gradlew :bottlenote-admin-api:test --tests 'app.docs.curation.AdminSpecBasedCurationControllerDocsTest'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'`
  - `./gradlew :bottlenote-product-api:test --tests 'app.docs.curation.RestProductSpecBasedCurationControllerTest'`
  - `./gradlew :bottlenote-product-api:integration_test --tests 'app.bottlenote.curation.integration.ProductSpecBasedCurationIntegrationTest'`
  - `git diff --check`
- Files: no expected source files beyond test updates and plan progress log
- Size: S
- Status: [x] done

### Task 6: Full verification

- Acceptance: `/verify full` 상당 범위의 compile, rule, unit, build, integration, admin integration 검증이 성공한다.
- Acceptance: 실패 시 실패 레이어와 원인을 Progress Log에 기록한다.
- Verification:
  - `./gradlew compileJava compileKotlin compileTestJava compileTestKotlin`
  - `./gradlew check_rule_test`
  - `./gradlew unit_test`
  - `./gradlew build`
  - `./gradlew integration_test`
  - `./gradlew admin_integration_test`
- Files: `plan/curation-v2-test-hardening.md`
- Size: S
- Status: [x] done

## Progress Log

- 2026-05-18: `/plan` 완료. 테스트 보강을 unit materializer, spec parameterized validation, Product integration snapshot semantics, Admin integration boundary, agent review/focused verification, full verification 6개 task로 분리했다.
- 2026-05-18: Task 1-4 구현 및 리뷰 피드백 반영. `source: MANUAL(직접 입력)` 단독 payload에서 GraphQL 실행을 생략하고 `stats=null`을 유지하도록 materializer를 보강했다. Admin 무인증 경계는 현재 보안 설정의 실제 응답인 403을 포함하는 4xx 기준으로 문서화했다.
- 2026-05-18: Test Intent Review Agent와 Test Convention Review Agent 검토 완료. 반영 사항: 원본 알코올 변경 전제 assertion 추가, 스펙별 required 누락 parameterized 반례 추가, Admin 무인증 테스트 복구, helper mutation 제거, DisplayName 문장형 보정.
- 2026-05-18: Task 5 focused suite와 `git diff --check` 성공. `/verify full` 수행 결과 `compileJava compileTestJava`, `:bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`, `check_rule_test`, `unit_test`, `build -x test -x asciidoctor --build-cache --parallel`, `integration_test`, `admin_integration_test`, optional `asciidoctor` 모두 `BUILD SUCCESSFUL`.
