# Plan: Admin API V2 Curation Endpoints

## Overview

Admin API의 versioning 경계를 정리한다. 현재 admin-api는 `server.servlet.context-path=/admin/api/v1`에 version이 박혀 있어서 신규 spec 기반 큐레이션 endpoint가 `/admin/api/v1/spec-based-curations` 형태로 노출된다. 앞으로는 context-path를 `/admin/api`로 낮추고, 기존 Admin API는 controller mapping에서 `/v1`을 명시하며, 신규 spec 기반 큐레이션 API는 `/v2` surface로 노출한다.

최종 목표는 기존 legacy Admin API를 `/admin/api/v1/**`로 유지하면서, 신규 spec 기반 큐레이션 관리는 `/admin/api/v2/curations`, `/admin/api/v2/curation-specs`로 고정하는 것이다.

### Assumptions

- admin-api 전체 context-path는 `/admin/api`로 변경한다.
- 기존 Admin v1 API는 깨지지 않도록 모든 기존 controller mapping에 `/v1` prefix를 명시한다.
- 기존 spec 기반 endpoint `/admin/api/v1/spec-based-curations`, `/admin/api/v1/curation-specs`는 호환 alias로 남기지 않는다.
- spec 기반 큐레이션 본문 관리는 `/admin/api/v2/curations`로 노출한다.
- 큐레이션 스펙 관리는 `/admin/api/v2/curation-specs`로 노출한다.
- Product API `/api/v2/curations`는 이번 변경 범위가 아니다.
- DB schema, GraphQL SDL, payload validation, Product materializer 로직은 이번 변경 범위가 아니다.

### Success Criteria

- `bottlenote-admin-api`의 `server.servlet.context-path`가 `/admin/api`로 변경된다.
- 기존 Admin v1 controller들은 최종 URL이 기존과 동일하게 `/admin/api/v1/**`로 유지된다.
- spec 기반 큐레이션 관리 API 최종 URL은 다음과 같다.
  - `GET /admin/api/v2/curation-specs`
  - `GET /admin/api/v2/curation-specs/{specId}`
  - `GET /admin/api/v2/curations`
  - `GET /admin/api/v2/curations/{curationId}`
  - `POST /admin/api/v2/curations`
  - `PUT /admin/api/v2/curations/{curationId}`
- `/admin/api/v1/spec-based-curations`와 `/admin/api/v1/curation-specs`는 더 이상 canonical endpoint가 아니다.
- Admin RestDocs snippets와 문서 경로/설명이 신규 `/v2` endpoint 기준으로 갱신된다.
- Admin integration test가 신규 `/v2` endpoint로 생성/조회/검증을 수행한다.
- `.example/display` 데모의 Admin API 호출 경로가 신규 `/admin/api/v2` endpoint 기준으로 갱신된다.
- `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`가 성공한다.
- `./gradlew :bottlenote-admin-api:test --tests 'app.docs.curation.AdminSpecBasedCurationControllerDocsTest'`가 성공한다.
- `./gradlew admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'`가 성공한다.
- `./gradlew check_rule_test`가 성공한다.

### Impact Scope

- `bottlenote-admin-api`
  - `src/main/resources/application.yml`: context-path 변경.
  - `src/main/kotlin/app/bottlenote/**/presentation/*Controller.kt`: 기존 v1 surface 보존을 위한 `/v1` prefix 반영.
  - `src/main/kotlin/app/bottlenote/curation/presentation/AdminCurationSpecController.kt`: `/v2/curation-specs`로 변경.
  - `src/main/kotlin/app/bottlenote/curation/presentation/AdminSpecBasedCurationController.kt`: `/v2/curations`로 변경.
  - `src/test/kotlin/app/docs/**`: RestDocs URI와 snippets 갱신 가능.
  - `src/test/kotlin/app/integration/**`: context-path 변화에 따른 URI 갱신 가능.
- `.example/display`
  - Admin API base path 또는 endpoint 조합이 `/admin/api/v2/curations`, `/admin/api/v2/curation-specs`를 사용하도록 갱신된다.
- `plan/spec-based-curation-v2-graphql-sdl.md`
  - 기존 `/admin/api/v1/spec-based-curations`, `/admin/api/v1/curation-specs` 결정 내용을 취소선 + 정정으로 갱신한다.
- `bottlenote-mono`
  - 서비스, 도메인, repository, GraphQL hydration 로직은 변경하지 않는다.
- Persistence
  - schema migration 없음.
- Security
  - Admin security 정책은 기존 `anyRequest().authenticated()` 구조를 유지한다. 단, `auth/login`, `auth/refresh` 최종 URL이 `/admin/api/v1/auth/*`로 유지되도록 mapping을 점검한다.
- API compatibility
  - 기존 Admin v1 API는 유지한다.
  - 기존 spec 기반 임시 endpoint는 호환 alias 없이 신규 `/v2`로 이동한다.

### Open Questions

- 없음. 사용자 확인 사항:
  - context-path는 `/admin/api`로 낮춘다.
  - 기존 spec 기반 v1 endpoint alias는 남기지 않는다.
  - spec 관리는 `/v2/curation-specs`로 간다.

## Dependency Analysis

1. `context-path`를 `/admin/api`로 낮추면 기존 Admin controller mapping이 그대로일 경우 최종 URL이 `/admin/api/{path}`로 바뀌어 v1 API가 깨진다.
2. 기존 v1 API를 보존하려면 모든 legacy Admin controller에 `/v1` prefix가 적용되어야 한다.
3. 모든 controller 파일을 직접 수정하면 변경량이 커지므로, 중앙 Web MVC path prefix 설정으로 legacy Admin controller에 `/v1`을 적용하고 spec 기반 curation v2 controller만 제외하는 방향을 우선한다.
4. v2 curation controller는 명시적으로 `/v2/curation-specs`, `/v2/curations`를 갖는다.
5. Security permit matcher는 context-path를 제외한 servlet path 기준으로 동작하므로 `/v1/auth/login`, `/v1/auth/refresh` 기준으로 갱신한다.
6. MockMvc 기반 docs/integration test는 context-path가 아니라 controller mapping을 직접 호출하므로 기존 Admin v1 테스트 URI도 `/v1/**`로 갱신해야 한다.

## Tasks

### Task 1: Admin API versioning foundation

- Acceptance:
  - admin-api `server.servlet.context-path`가 `/admin/api`로 변경된다.
  - legacy Admin presentation controller에는 중앙 설정으로 `/v1` prefix가 적용된다.
  - spec 기반 curation v2 controller는 `/v1` prefix 대상에서 제외된다.
  - Admin security permit matcher가 `/v1/auth/login`, `/v1/auth/refresh`를 허용한다.
- Verification:
  - `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`
- Files:
  - `bottlenote-admin-api/src/main/resources/application.yml`
  - `bottlenote-admin-api/src/main/kotlin/app/global/security/SecurityConfig.kt`
  - `bottlenote-admin-api/src/main/kotlin/app/global/config/*`
- Size: S
- Status: [x] done

### Task 2: Curation v2 admin endpoint remapping

- Acceptance:
  - `AdminCurationSpecController`가 `/v2/curation-specs`를 제공한다.
  - `AdminSpecBasedCurationController`가 `/v2/curations`를 제공한다.
  - `/v1/curation-specs`, `/v1/spec-based-curations` alias는 만들지 않는다.
- Verification:
  - `./gradlew :bottlenote-admin-api:test --tests 'app.docs.curation.AdminSpecBasedCurationControllerDocsTest'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'`
- Files:
  - `bottlenote-admin-api/src/main/kotlin/app/bottlenote/curation/presentation/AdminCurationSpecController.kt`
  - `bottlenote-admin-api/src/main/kotlin/app/bottlenote/curation/presentation/AdminSpecBasedCurationController.kt`
  - `bottlenote-admin-api/src/test/kotlin/app/docs/curation/AdminSpecBasedCurationControllerDocsTest.kt`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/curation/AdminSpecBasedCurationIntegrationTest.kt`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2

- [ ] Admin Kotlin compile/test compile passes
- [ ] Curation v2 docs test passes
- [ ] Curation v2 admin integration test passes

### Task 3: Preserve core Admin v1 auth/common/user/help routes in tests

- Acceptance:
  - Auth docs/integration tests call `/v1/auth/**`.
  - File upload tests call `/v1/s3/**`.
  - User and help tests call `/v1/users`, `/v1/helps/**`.
  - Existing final URLs remain `/admin/api/v1/**`.
- Verification:
  - `./gradlew :bottlenote-admin-api:test --tests 'app.docs.auth.AuthControllerDocsTest' --tests 'app.docs.file.AdminImageUploadControllerDocsTest' --tests 'app.docs.user.AdminUsersControllerDocsTest' --tests 'app.docs.help.AdminHelpControllerDocsTest'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.auth.AdminAuthIntegrationTest' --tests 'app.integration.file.AdminImageUploadIntegrationTest' --tests 'app.integration.user.AdminUsersIntegrationTest' --tests 'app.integration.help.AdminHelpIntegrationTest'`
- Files:
  - `bottlenote-admin-api/src/test/kotlin/app/docs/auth/*`
  - `bottlenote-admin-api/src/test/kotlin/app/docs/file/*`
  - `bottlenote-admin-api/src/test/kotlin/app/docs/user/*`
  - `bottlenote-admin-api/src/test/kotlin/app/docs/help/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/auth/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/file/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/user/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/help/*`
- Size: M
- Status: [x] done

### Task 4: Preserve Admin v1 alcohol/reference routes in tests

- Acceptance:
  - Alcohol, distillery, region, tasting-tag docs/integration tests call `/v1/**`.
  - Reference data endpoints remain under `/admin/api/v1`.
- Verification:
  - `./gradlew :bottlenote-admin-api:test --tests 'app.docs.alcohols.*'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.alcohols.*' --tests 'app.integration.region.*'`
- Files:
  - `bottlenote-admin-api/src/test/kotlin/app/docs/alcohols/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/alcohols/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/region/*`
- Size: M
- Status: [x] done

### Task 5: Preserve Admin v1 banner and legacy curation routes in tests

- Acceptance:
  - Banner docs/integration tests call `/v1/banners/**`.
  - Legacy Admin curation docs/integration tests call `/v1/curations/**`.
  - Spec 기반 curation v2와 legacy curation v1이 `/admin/api/v2/curations` vs `/admin/api/v1/curations`로 명확히 분리된다.
- Verification:
  - `./gradlew :bottlenote-admin-api:test --tests 'app.docs.banner.AdminBannerControllerDocsTest' --tests 'app.docs.curation.AdminCurationControllerDocsTest'`
  - `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.banner.AdminBannerIntegrationTest' --tests 'app.integration.curation.AdminCurationIntegrationTest'`
- Files:
  - `bottlenote-admin-api/src/test/kotlin/app/docs/banner/*`
  - `bottlenote-admin-api/src/test/kotlin/app/docs/curation/AdminCurationControllerDocsTest.kt`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/banner/*`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/curation/AdminCurationIntegrationTest.kt`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 3-5

- [x] Existing Admin v1 docs tests pass
- [x] Existing Admin v1 integration tests pass
- [x] Legacy `/admin/api/v1/curations` and new `/admin/api/v2/curations` are both represented in tests

### Task 6: Demo and planning documents update

- Acceptance:
  - `.example/display` Admin base URL and endpoint calls use `/admin/api` + `/v1` or `/v2` consistently.
  - Spec 기반 curation demo calls `/v2/curation-specs` and `/v2/curations`.
  - Architecture/demo text no longer points to `/admin/api/v1/spec-based-curations`.
  - `plan/spec-based-curation-v2-graphql-sdl.md` records the endpoint decision change with 취소선 + 정정.
- Verification:
  - `curl -s http://localhost:8098/curation-architecture.html`
  - `rg -n "spec-based-curations|/admin/api/v1/curation-specs" .example/display plan/spec-based-curation-v2-graphql-sdl.md` returns no stale canonical references except historical struck-through notes.
- Files:
  - `.example/display/js/config.js`
  - `.example/display/js/api.js`
  - `.example/display/js/*.js`
  - `plan/spec-based-curation-v2-graphql-sdl.md`
  - `plan/admin-api-v2-curation-endpoints.md`
- Size: M
- Status: [x] done

### Task 7: Final verification and commit

- Acceptance:
  - Admin API versioning change is committed as one coherent commit after verification.
  - No unintended `bottlenote-mono`, Product API, DB changelog, or GraphQL SDL changes are included.
  - Git diff shows only Admin API endpoint/versioning docs and demo path updates.
- Verification:
  - `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`
  - `./gradlew :bottlenote-admin-api:test`
  - `./gradlew admin_integration_test`
  - `./gradlew check_rule_test`
  - `git diff --check`
- Files:
  - `bottlenote-admin-api/**`
  - `.example/display/**`
  - `plan/admin-api-v2-curation-endpoints.md`
  - `plan/spec-based-curation-v2-graphql-sdl.md`
- Size: S
- Status: [x] done

## Progress Log

- 2026-05-18: Task 1 완료. admin-api context-path를 `/admin/api`로 낮추고, `AdminApiVersionConfig`에서 legacy Admin presentation controller에 중앙 `/v1` prefix를 적용했다. spec 기반 curation controller 2개는 prefix 대상에서 제외했고, Security permit matcher를 `/v1/auth/login`, `/v1/auth/refresh`로 갱신했다. 검증: `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin` 성공.
- 2026-05-18: Task 2 완료. spec 기반 admin curation controller를 `/v2/curations`, spec 조회 controller를 `/v2/curation-specs`로 변경하고 RestDocs/integration 테스트 URI와 snippet 경로를 v2 기준으로 갱신했다. 검증: `./gradlew :bottlenote-admin-api:test --tests 'app.docs.curation.AdminSpecBasedCurationControllerDocsTest'` 성공, `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.curation.AdminSpecBasedCurationIntegrationTest'` 성공. root aggregate `admin_integration_test --tests ...`는 `--tests` 옵션을 받지 않아 모듈 Test 태스크 명령으로 정정했다.
- 2026-05-18: Task 3 완료. auth, file, user, help docs/integration 테스트 요청 URI를 `/v1/**` 기준으로 갱신해 기존 Admin v1 최종 URL 보존을 검증했다. 검증: `./gradlew :bottlenote-admin-api:test --tests 'app.docs.auth.AuthControllerDocsTest' --tests 'app.docs.file.AdminImageUploadControllerDocsTest' --tests 'app.docs.user.AdminUsersControllerDocsTest' --tests 'app.docs.help.AdminHelpControllerDocsTest'` 성공, `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.auth.AdminAuthIntegrationTest' --tests 'app.integration.file.AdminImageUploadIntegrationTest' --tests 'app.integration.user.AdminUsersIntegrationTest' --tests 'app.integration.help.AdminHelpIntegrationTest'` 성공.
- 2026-05-18: Task 4 완료. alcohol, distillery, region, tasting-tag docs/integration 테스트 요청 URI를 `/v1/**` 기준으로 갱신해 reference/admin alcohol API의 기존 v1 surface를 보존했다. 검증: `./gradlew :bottlenote-admin-api:test --tests 'app.docs.alcohols.*'` 성공, `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.alcohols.*' --tests 'app.integration.region.*'` 성공.
- 2026-05-18: Task 5 완료. banner docs/integration 테스트를 `/v1/banners/**`, legacy curation docs/integration 테스트를 `/v1/curations/**` 기준으로 갱신했다. spec 기반 신규 curation 테스트는 `/v2/curations`로 유지되어 legacy와 v2 surface가 분리된다. 검증: `./gradlew :bottlenote-admin-api:test --tests 'app.docs.banner.AdminBannerControllerDocsTest' --tests 'app.docs.curation.AdminCurationControllerDocsTest'` 성공, `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.integration.banner.AdminBannerIntegrationTest' --tests 'app.integration.curation.AdminCurationIntegrationTest'` 성공.
- 2026-05-18: Task 6 완료. `.example/display`의 admin base URL을 `/admin/api`로 낮추고, legacy 호출은 `/v1`, spec 기반 신규 호출은 `/v2/curation-specs`, `/v2/curations`로 분리했다. `plan/spec-based-curation-v2-graphql-sdl.md`에는 기존 `/admin/api/v1/curation-specs`, `/admin/api/v1/spec-based-curations` 결정을 취소선 + 정정으로 남겼다. 검증: `curl -s http://localhost:8098/curation-architecture.html` 성공, `rg -n "spec-based-curations|/admin/api/v1/curation-specs|/admin/api/v1/spec-based-curations" .example/display plan/spec-based-curation-v2-graphql-sdl.md` 결과는 plan 문서의 취소선/정정 이력 3건만 남고 `.example/display`에는 stale canonical reference가 없음을 확인했다.
- 2026-05-18: Task 7 완료. 최종 검증으로 admin compile, admin 전체 RestDocs/test, 전체 admin integration, rule test, diff whitespace check를 수행했고 모두 성공했다. 검증: `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin` 성공, `./gradlew :bottlenote-admin-api:test` 성공, `./gradlew admin_integration_test` 성공, `./gradlew check_rule_test` 성공, `git diff --check` 성공.
