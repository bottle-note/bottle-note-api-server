# Plan: 어드민 리뷰 목록 조회 API

## Overview

어드민이 리뷰 운영 상태를 한 화면에서 확인할 수 있도록 `bottlenote-admin-api`에 리뷰 목록 조회 `GET` API를 추가한다. API 표면은 Kotlin `presentation` 패키지에 두고, 조회 조건 조합과 목록 응답 생성은 `bottlenote-mono`의 어드민 전용 리뷰 조회 서비스와 QueryDSL 기반 repository 확장으로 처리한다.

범위는 목록 조회만 포함한다. 단건 상세 조회, 리뷰 상태 변경, DB 스키마 변경, 기존 product 리뷰 조회 정책 변경은 포함하지 않는다.

### Assumptions

- endpoint는 admin context path `/admin/api/v1` 아래 `GET /reviews`로 둔다.
- 컨트롤러는 `bottlenote-admin-api/src/main/kotlin/app/bottlenote/review/presentation/AdminReviewController.kt`에 신설한다.
- 요청 DTO, 응답 DTO, sort enum, 어드민 전용 조회 서비스는 `bottlenote-mono`의 `app.bottlenote.review` 하위에 둔다.
- 어드민 목록 조회는 인증된 admin API surface로 동작하며, 현재 `bottlenote-admin-api`의 SecurityConfig 정책을 그대로 따른다.
- 페이징은 기존 어드민 검색 API와 맞춰 `page`, `size` 기반 offset pagination과 `GlobalResponse.fromPage(Page<T>)` 응답을 사용한다.
- 필터는 `alcoholId`, `userId`, `activeStatus`, `displayStatus`, `keyword`, `createdFrom`, `createdTo`를 지원한다.
- `keyword`는 리뷰 본문과 작성자 식별 정보 중 기존 join으로 안정적으로 제공 가능한 값에 적용한다. 정확한 대상 컬럼은 `/plan`에서 기존 user/alcohol join 비용을 보고 확정한다.
- 정렬은 `CREATED_AT`, `REPLY_COUNT`, `UPDATED_AT` 3종과 `sortOrder`를 지원하며 기본값은 `CREATED_AT DESC`다.
- 모든 상태 노출 요구사항에 따라 기본 조회는 `ReviewActiveStatus.ACTIVE/DELETED/DISABLED`와 `ReviewDisplayStatus.PUBLIC/PRIVATE`를 모두 포함하고, 상태 파라미터가 있을 때만 좁힌다.
- 응답 항목에는 운영 목록에서 식별과 판단에 필요한 최소 필드인 `reviewId`, `alcoholId`, `alcoholName`, `userId`, `userNickname`, `content`, `reviewRating`, `activeStatus`, `displayStatus`, `replyCount`, `createAt`, `lastModifyAt`를 포함한다.
- 댓글 수 정렬은 기존 `ReviewReply` 데이터를 집계해서 산출하며, 별도 counter column이나 schema migration은 추가하지 않는다.

### Success Criteria

- `GET /admin/api/v1/reviews`가 `GlobalResponse` 형식으로 리뷰 목록과 page meta를 반환한다.
- 필터 7종 중 각각을 단독 적용했을 때 결과가 해당 조건으로 좁혀진다: `alcoholId`, `userId`, `activeStatus`, `displayStatus`, `keyword`, `createdFrom`, `createdTo`.
- `activeStatus` 미지정 시 `ACTIVE`, `DELETED`, `DISABLED` 리뷰가 모두 조회 대상에 포함된다.
- `displayStatus` 미지정 시 `PUBLIC`, `PRIVATE` 리뷰가 모두 조회 대상에 포함된다.
- 정렬 `CREATED_AT`, `REPLY_COUNT`, `UPDATED_AT`가 각각 `ASC`/`DESC` 방향으로 동작한다.
- 단건 상세 API나 상태 변경 API가 추가되지 않는다.
- DB migration 파일이 추가되지 않는다.
- 기존 product 리뷰 목록/상세 조회는 계속 `ACTIVE`와 공개 정책을 유지한다.
- admin controller docs 또는 admin integration 테스트에서 필터, 전체 상태 노출, 정렬, page meta를 검증한다.
- `./gradlew :bottlenote-admin-api:test` 또는 관련 admin integration/docs 테스트 태스크로 변경 범위를 검증할 수 있다.

### Impact Scope

- `bottlenote-admin-api`
  - 새 admin review presentation controller 추가
  - admin docs/integration 테스트 추가 가능
- `bottlenote-mono`
  - admin review search request/response DTO 추가
  - admin review sort enum 추가
  - admin 전용 read-only service 추가
  - `ReviewRepository` 또는 `CustomReviewRepository` 계열에 admin 목록 조회 query 추가
  - `Review`, `ReviewReply`, `User`, `Alcohol` join 및 댓글 수 집계 사용
- Persistence
  - DB 스키마 변경 없음
  - 기존 `reviews.active_status`, `reviews.status`, `reviews.create_at`, `reviews.last_modify_at` 필드 사용
- Async / Events
  - 새 이벤트 발행 또는 소비 없음
- Cache
  - 신규 캐시와 invalidation 정책 없음
- API Contract / Docs
  - admin API 계약 추가
  - product API 계약 변경 없음
- Tests
  - QueryDSL 조건 조합 검증
  - admin controller 문서 또는 통합 테스트
  - 상태 미지정 시 전체 상태 노출 회귀 방지

## Tasks

의존성 순서는 `bottlenote-mono` 조회 경로를 먼저 만들고, `bottlenote-admin-api` HTTP surface를 같은 기본 목록 slice에 연결한 뒤, 필터/정렬과 검증 범위를 확장한다. 모든 Task는 L 사이즈(8개 이상 파일)를 피하고, 한 번에 커밋 가능한 수직 slice로 유지한다.

### Task 1: 기본 어드민 리뷰 목록 조회 slice
- Acceptance: `GET /admin/api/v1/reviews`가 인증된 admin 요청에서 기본 `page=0`, `size=20`, `CREATED_AT DESC` 목록을 `GlobalResponse.fromPage(Page<T>)` 형태로 반환한다.
- Acceptance: 응답 항목에 `reviewId`, `alcoholId`, `alcoholName`, `userId`, `userNickname`, `content`, `reviewRating`, `activeStatus`, `displayStatus`, `replyCount`, `createAt`, `lastModifyAt`가 포함된다.
- Acceptance: 상태 파라미터가 없을 때 `ACTIVE`, `DELETED`, `DISABLED` 및 `PUBLIC`, `PRIVATE` 리뷰가 조회 대상에서 제외되지 않는다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/review/dto/request/AdminReviewSearchRequest.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/dto/response/AdminReviewListResponse.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/service/AdminReviewQueryService.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/domain/ReviewRepository.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/repository/CustomReviewRepository.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/repository/CustomReviewRepositoryImpl.java`, `bottlenote-admin-api/src/main/kotlin/app/bottlenote/review/presentation/AdminReviewController.kt`
- Size: M
- Status: [x] done

### Task 2: 필터와 정렬 query slice
- Acceptance: `alcoholId`, `userId`, `activeStatus`, `displayStatus`, `keyword`, `createdFrom`, `createdTo`가 단독 적용될 때 QueryDSL where 조건으로 결과를 좁힌다.
- Acceptance: `keyword`는 리뷰 본문, 작성자 닉네임, 작성자 이메일, 주류 한글명, 주류 영문명에 적용한다.
- Acceptance: `CREATED_AT`, `REPLY_COUNT`, `UPDATED_AT` 정렬이 `ASC`/`DESC`로 동작하고 동일 정렬값에서는 최신 리뷰가 보조 정렬된다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/review/constant/AdminReviewSortType.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/dto/request/AdminReviewSearchRequest.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/repository/CustomReviewRepositoryImpl.java`, `bottlenote-mono/src/main/java/app/bottlenote/review/repository/ReviewQuerySupporter.java`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] `bottlenote-mono` Java compile passes
- [x] `bottlenote-admin-api` Kotlin compile passes
- [x] No DB migration file is added
- [x] Product review query policy remains unchanged

### Task 3: 어드민 리뷰 목록 통합 테스트 slice
- Acceptance: admin integration 테스트가 기본 목록, 전체 active/display 상태 노출, 7종 필터 단독 적용을 검증한다.
- Acceptance: admin integration 테스트가 `CREATED_AT`, `REPLY_COUNT`, `UPDATED_AT` 정렬과 `ASC`/`DESC` 방향, page meta를 검증한다.
- Acceptance: 테스트 데이터는 기존 Testcontainers 기반 factory를 사용하고 Mock 기반 repository 대체를 추가하지 않는다.
- Verification: `./gradlew admin_integration_test --tests '*AdminReviewIntegrationTest'`
- Files: `bottlenote-admin-api/src/test/kotlin/app/integration/review/AdminReviewIntegrationTest.kt`, `bottlenote-mono/src/test/java/app/bottlenote/review/fixture/ReviewTestFactory.java`
- Size: S
- Status: [x] done

### Task 4: 어드민 리뷰 목록 RestDocs 계약 slice
- Acceptance: RestDocs 테스트가 `GET /reviews` query parameters와 응답 필드, page meta를 문서화한다.
- Acceptance: 문서에는 목록 조회만 포함하고 단건 상세 조회, 상태 변경 API, DB migration 내용은 추가하지 않는다.
- Acceptance: admin default test에서 새 DocsTest가 함께 통과한다.
- Verification: `./gradlew :bottlenote-admin-api:test --tests '*AdminReviewControllerDocsTest'`
- Files: `bottlenote-admin-api/src/test/kotlin/app/docs/review/AdminReviewControllerDocsTest.kt`, `bottlenote-admin-api/src/docs/asciidoc/admin-review.adoc`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 3-4
- [ ] `./gradlew admin_integration_test --tests '*AdminReviewIntegrationTest'` passes
- [x] `./gradlew :bottlenote-admin-api:test --tests '*AdminReviewControllerDocsTest'` passes
- [x] `./gradlew :bottlenote-admin-api:test` passes or any unrelated failure is recorded with evidence
- [x] API contract still exposes only `GET /admin/api/v1/reviews`

## Progress Log

### 2026-05-14 — Task 1
- Implemented admin review basic list slice for `GET /admin/api/v1/reviews`.
- Added mono request/response DTOs, read-only admin query service, `ReviewRepository.searchAdminReviews`, QueryDSL projection, and admin Kotlin controller.
- Updated `InMemoryReviewRepository` implementations in mono/product test fixtures because the domain repository interface changed.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin` passed.

### 2026-05-14 — Task 2
- Added admin review sort type and request defaults for `CREATED_AT DESC`.
- Added QueryDSL filters for `alcoholId`, `userId`, `activeStatus`, `displayStatus`, `keyword`, `createdFrom`, and `createdTo`.
- Added admin review ordering for `CREATED_AT`, `REPLY_COUNT`, and `UPDATED_AT` with latest-review tie breakers.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin` passed.

### 2026-05-14 — Task 3
- Added `AdminReviewIntegrationTest` with `@Tag("admin_integration")` for default list response fields, all active/display state exposure, 7 standalone filters, keyword targets, 3 sort types with both directions, and page meta.
- Extended `ReviewTestFactory` with a Testcontainers-backed admin review fixture method that persists real `Review` rows and fixes `create_at` / `last_modify_at` via the test database.
- Verification note: root aggregate command `./gradlew admin_integration_test --tests '*AdminReviewIntegrationTest'` failed because the aggregate task does not accept `--tests`.
- Verification: `git submodule update --init --recursive` completed for missing `git.environment-variables`; `./gradlew :bottlenote-admin-api:admin_integration_test --tests '*AdminReviewIntegrationTest'` passed.

### 2026-05-14 — Task 4
- Added `AdminReviewControllerDocsTest` for `GET /admin/api/v1/reviews` query parameters, response fields, and page meta.
- Added `admin-review.adoc` and linked it from `admin-api.adoc`; the document includes only the admin review list API.
- Verification: `./gradlew :bottlenote-admin-api:test --tests '*AdminReviewControllerDocsTest'` passed.
- Verification: `./gradlew :bottlenote-admin-api:test` passed, confirming the new admin DocsTest runs in the default admin test task.
- Verification: `./gradlew :bottlenote-admin-api:asciidoctor` passed.
- Contract check: `rg -n "@(?:Get|Post|Put|Patch|Delete)Mapping|/reviews|admin/reviews|Review API|admin-review" ...` found only `@GetMapping` on `/reviews`, the list DocsTest, and the list document include.
