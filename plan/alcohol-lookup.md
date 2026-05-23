# Plan: Alcohol Lookup

## Overview

FE 위스키 선택/세팅 컴포넌트에서 사용할 고속 조회 API를 추가한다.

기존 `GET /api/v1/alcohols/search`는 일반 검색 화면까지 포함하는 무거운 조회 경로다. 신규 lookup API는 최대 20,000개 수준의 위스키 핵심 필드만 대상으로 하며, 별점, 좋아요, 리뷰 수, 사용자 pick 여부 같은 실시간 집계성 데이터는 제외한다.

기본 아키텍처는 DB를 원천 데이터로 두고, Redis에 lookup snapshot을 주기적으로 동기화한 뒤, 요청 시 BE에서 snapshot을 읽어 메모리 stream 필터링과 cursor pagination을 수행하는 방식이다. DB 직접 조회 방식은 k6 비교 검증용 임시 경로로만 사용하고, 검증 후 외부 API 계약에서는 제거한다.

### Frontend Usage Findings

- Product FE 리뷰 작성 선택 컴포넌트는 `keyword`, `category`, `cursor`, `pageSize=20` 조건으로 술을 선택한다.
- Product FE 일반 검색은 `keyword`, `category`, `regionId`, `sortType`, `sortOrder`, `cursor`, `pageSize=10` 조건을 사용한다.
- Product FE 탐색 API는 `keywords[]`, `regionIds[]`, `category`, `sortType`, `sortOrder`, `cursor`, `size` 구조를 사용한다.
- Admin 위스키 선택 컴포넌트는 `keyword`, `size=10`을 사용하며 300ms debounce가 있다.
- Admin 위스키 목록 검색은 `keyword`, `category`, `page`, `size`, `includeDeleted` 조건을 사용한다.

### Assumptions

- Endpoint는 product 기존 alcohol 컨트롤러에 `GET /api/v1/alcohols/lookup`으로 추가한다.
- Admin도 동일 공통 서비스인 `AlcoholLookupService`를 사용하되, 컨트롤러는 기존 admin alcohol 컨트롤러에 필요한 엔드포인트를 추가하는 방식으로 확장한다.
- 서비스 레이어 이름은 `AlcoholLookupService`로 고정한다.
- Redis는 원천 저장소가 아니라 읽기 전용 lookup snapshot 저장소로 사용한다.
- Redis snapshot은 약간의 동기화 지연을 허용한다.
- Redis에 저장하는 필드는 위스키 선택에 필요한 핵심 필드로 제한한다.
- 핵심 필드는 `alcoholId`, `korName`, `engName`, `korCategory`, `engCategory`, `regionId`, `korRegion`, `engRegion`, `distilleryId`, `korDistillery`, `engDistillery`, `imageUrl`, `searchText`를 우선 후보로 한다.
- 별점, 좋아요, 리뷰 수, pick 여부, 개인화 필드는 lookup 응답에서 제외한다.
- 검색 대상은 `keyword`, `category`, `regionId`, `distilleryId`를 우선 지원한다.
- 페이지네이션은 cursor 기반을 기본으로 한다.
- 최대 데이터 규모는 lookup 대상 20,000건으로 본다.
- 일반 Redis만 사용 가능한 환경을 우선 가정하고, RediSearch는 이번 구현 범위에서 제외한다.
- DB 직접 조회 방식은 운영 기본 경로가 아니라 k6 비교 검증을 위한 임시 대체 경로로만 준비하고, 검증 후 외부 API 계약에서 제거한다.

### Success Criteria

- `GET /api/v1/alcohols/lookup`이 `keyword`, `category`, `regionId`, `distilleryId`, `cursor`, `pageSize` 조건을 받아 cursor 기반 목록을 반환한다.
- 응답 항목은 위스키 선택/세팅에 필요한 핵심 필드만 포함하고, 별점, 좋아요, 리뷰 수, pick 여부를 포함하지 않는다.
- Redis snapshot 기반 조회 경로가 기본 서빙 경로이며, Redis miss 또는 snapshot 부재 시 DB fallback을 수행한다.
- Redis miss 또는 snapshot 부재 상황에서 정의된 fallback 동작이 있다.
- 최대 20,000건 기준으로 k6 검증을 수행하고, 외부 `source` 선택 경로는 검증 후 제거한다.
- Product/Admin 양쪽에서 공통 `AlcoholLookupService`를 사용할 수 있다.
- 기존 `/api/v1/alcohols/search`의 응답 계약과 동작은 변경하지 않는다.

### Impact Scope

- `bottlenote-mono`
  - `app.bottlenote.alcohols.service`: `AlcoholLookupService` 추가
  - `app.bottlenote.alcohols.dto.request`: lookup request DTO 추가
  - `app.bottlenote.alcohols.dto.response`: lookup item/response DTO 추가
  - `app.bottlenote.alcohols.domain` 또는 `repository`: DB lookup 조회 계약 추가
  - `app.bottlenote.alcohols.repository`: DB lookup QueryDSL 구현 추가
  - Redis snapshot 저장소 구현 위치 검토
- `bottlenote-product-api`
  - 기존 `AlcoholQueryController`에 `/lookup` endpoint 추가
  - REST Docs 테스트와 asciidoc include 추가 검토
- `bottlenote-admin-api`
  - 기존 `AdminAlcoholsController`에 admin lookup endpoint 추가 여부 검토
  - Admin Docs 테스트 추가 검토
- Redis
  - lookup snapshot key 설계 필요
  - snapshot 갱신 주기와 원자적 교체 방식 필요
  - Redis 3 replica 환경에서 read 경로와 consistency 기대치 명시 필요
- Batch/Scheduler
  - 5분 주기 동기화가 product/admin API 모듈 내부 scheduler로 충분한지, batch 모듈로 분리할지 결정 필요
- Tests
  - `AlcoholLookupService` 단위 테스트
  - Redis 저장소 fake 또는 TestContainers 기반 통합 테스트
  - Product lookup API 통합/문서 테스트
  - Admin lookup API가 추가될 경우 admin 통합/문서 테스트
- Performance Verification
  - 이번 define 단계에서는 k6 스크립트를 작성하지 않는다.
  - 다음 세션에서 Redis snapshot 조회와 DB 직접 조회를 같은 조건으로 비교할 수 있도록 경로와 조건을 명확히 유지한다.

### Open Questions

- Admin lookup endpoint도 기존 `AdminAlcoholsController`에 추가한다.
- Redis snapshot 갱신은 공통 `AlcoholLookupService`에 동기화 메서드를 두고, scheduler binding 위치는 구현 중 현재 배포 구조를 확인해 결정한다. 중복 실행 방지를 위해 property guard를 둔다.
- cursor 기준은 Redis snapshot 결과 순서의 다음 index cursor를 기본으로 하고, DB fallback도 같은 cursor 의미를 유지한다.
- `keyword` 검색은 `searchText contains` 기반으로 시작하되, 다중 단어 입력은 공백 분리 후 AND 조건으로 처리한다.
- category `ALL` 또는 null은 category 필터 없음으로 처리한다.

## Tasks

### Task 1: Lookup API 계약과 DTO 정의
- Acceptance: Product/Admin이 공유할 lookup request/response DTO가 `keyword`, `category`, `regionId`, `distilleryId`, `cursor`, `pageSize`를 표현한다.
- Acceptance: 응답 item은 핵심 필드만 포함하고 rating/review/pick 계열 필드를 포함하지 않는다.
- Acceptance: 기본값은 cursor `0`, pageSize는 product 선택 컴포넌트 기준 `20`을 우선한다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/request/*`, `bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/response/*`, 필요 시 `dto/dsl/*`
- Size: S
- Status: [x] done

### Task 2: DB lookup fallback 조회 경로 구현
- Acceptance: `AlcoholQueryRepository` 계열에 lookup 전용 조회 계약이 추가된다.
- Acceptance: QueryDSL projection은 alcohol, region, distillery, category 핵심 필드만 조회한다.
- Acceptance: `keyword`, `category`, `regionId`, `distilleryId`, cursor/pageSize 조건이 DB 경로에서 동작한다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Files: `AlcoholQueryRepository.java`, `CustomAlcoholQueryRepository.java`, `CustomAlcoholQueryRepositoryImpl.java`, `AlcoholQuerySupporter.java`, 관련 테스트 fake
- Size: M
- Status: [x] done

### Task 3: Redis snapshot store와 fallback 경계 구현
- Acceptance: lookup snapshot을 읽고 쓰는 저장소 경계가 생긴다.
- Acceptance: Redis snapshot miss/empty 시 DB fallback을 호출할 수 있는 결과 흐름이 정의된다.
- Acceptance: Redis snapshot miss/empty 시 DB fallback을 호출하되, 외부 API에서 DB source 선택은 노출하지 않는다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/*` 또는 `global/redis/*`, 관련 fake/test fixture
- Size: M
- Status: [x] done

### Task 4: AlcoholLookupService 서빙/동기화 유스케이스 구현
- Acceptance: `AlcoholLookupService`가 Redis snapshot 기반 조회를 기본 경로로 제공한다.
- Acceptance: 같은 서비스가 DB 원천 데이터를 Redis snapshot으로 동기화하는 메서드를 제공한다.
- Acceptance: 20,000건 snapshot을 대상으로 stream 필터링, 다중 keyword AND, cursor/pageSize slicing을 수행한다.
- Verification: `./gradlew unit_test --tests '*AlcoholLookupServiceTest*'`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AlcoholLookupService.java`, `bottlenote-mono/src/test/java/app/bottlenote/alcohols/service/*`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-4
- [x] `./gradlew :bottlenote-mono:compileJava`
- [x] `./gradlew unit_test --tests '*AlcoholLookupServiceTest*'`
- [x] Repository interface 변경 시 InMemory/Fake 구현체가 함께 갱신됐는지 확인

### Task 5: Product/Admin 기존 컨트롤러에 lookup endpoint 연결
- Acceptance: Product 기존 `AlcoholQueryController`에 `GET /api/v1/alcohols/lookup`이 추가된다.
- Acceptance: Admin 기존 `AdminAlcoholsController`에 admin lookup endpoint가 추가된다.
- Acceptance: 두 컨트롤러는 공통 `AlcoholLookupService`를 호출하고 별도 비즈니스 로직을 갖지 않는다.
- Verification: `./gradlew :bottlenote-product-api:compileJava :bottlenote-admin-api:compileKotlin`
- Files: `AlcoholQueryController.java`, `AdminAlcoholsController.kt`
- Size: S
- Status: [x] done

### Task 6: Lookup 동기화 scheduler binding 추가
- Acceptance: 5분 주기 snapshot 동기화가 property guard와 함께 등록된다.
- Acceptance: scheduler는 `AlcoholLookupService`의 동기화 메서드만 호출한다.
- Acceptance: product/admin/batch 중 실제 binding 위치 선택 이유가 코드 주석 또는 plan progress log에 남는다.
- Verification: `./gradlew :bottlenote-mono:compileJava` 또는 binding 모듈 compile task
- Files: scheduler binding 위치에 따라 `bottlenote-mono`, `bottlenote-product-api`, 또는 `bottlenote-batch`의 schedule config
- Size: S
- Status: [x] done

### Task 7: Product/Admin 문서와 API 테스트 추가
- Acceptance: Product lookup RestDocs 테스트가 query parameters, response fields, cursor meta를 문서화한다.
- Acceptance: Admin lookup Docs 테스트가 admin endpoint 계약을 문서화한다.
- Acceptance: asciidoc include가 필요한 경우 product/admin 문서 인덱스에 반영된다.
- Verification: `./gradlew :bottlenote-product-api:asciidoctor :bottlenote-admin-api:test`
- Files: product RestDocs test/adoc, admin Docs test/adoc, helper fixture
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 5-7
- [x] `./gradlew :bottlenote-product-api:compileJava :bottlenote-admin-api:compileKotlin`
- [x] `./gradlew :bottlenote-product-api:asciidoctor :bottlenote-admin-api:test`
- [x] Product/Admin controller가 기존 controller를 확장하고 신규 controller를 만들지 않았는지 확인

### Task 8: 통합 검증과 k6 참조 포인트 정리
- Acceptance: Redis snapshot 경로와 DB fallback 경로가 동일 조건에서 동일 응답 모델을 반환하는 통합 검증이 있다.
- Acceptance: 다음 세션 k6 플랜에서 사용할 비교 조건이 plan progress log 또는 코드 주석에 남는다.
- Acceptance: `verify full`에 필요한 명령 시퀀스가 통과한다.
- Verification: `./gradlew check_rule_test unit_test integration_test admin_integration_test :bottlenote-admin-api:test asciidoctor`
- Files: product/admin integration tests, 필요 시 plan progress log
- Size: M
- Status: [x] done

## Progress Log

- 2026-05-23: Added lookup request/response contracts and cursor/pageSize defaults.
- 2026-05-23: Added DB source projection through `AlcoholQueryRepository.findAllLookupItems()` for k6 Redis-vs-DB comparison.
- 2026-05-23: Added Redis snapshot store using key `alcohol:lookup:snapshot:v1`.
- 2026-05-23: Added `AlcoholLookupService` with Redis default path, DB fallback/source path, 20,000-item stream filtering scenario, multi-keyword AND, and snapshot sync.
- 2026-05-23: Added Product `/api/v1/alcohols/lookup` and Admin `/admin/api/v1/alcohols/lookup` to existing controllers.
- 2026-05-23: Added product-side 5-minute scheduler binding guarded by `schedules.alcohol.lookup.sync.enable`.
- 2026-05-23: Added Product/Admin RestDocs coverage and asciidoc includes for lookup API.
- 2026-05-23: Verification passed: `./gradlew :bottlenote-mono:compileJava :bottlenote-product-api:compileJava :bottlenote-admin-api:compileKotlin`.
- 2026-05-23: Verification passed: `./gradlew :bottlenote-mono:test --tests '*AlcoholLookupServiceTest*'` with 5 service scenarios.
- 2026-05-23: Verification passed: `./gradlew :bottlenote-product-api:asciidoctor :bottlenote-admin-api:test`.
- 2026-05-23: Verification passed: `./gradlew check_rule_test unit_test integration_test admin_integration_test :bottlenote-admin-api:test asciidoctor` in 9m 34s.
- 2026-05-23: Runtime smoke passed with product API `dev` profile, local Redis `localhost:16379`, and development DB. Redis snapshot sync stored 3,288 lookup items and default REDIS lookup returned HTTP 200.
- 2026-05-23: Completed clean PR repair. PR branch `codex/alcohol-lookup` now contains only lookup commit on top of `origin/main`, and GitHub CI passed.
- 2026-05-23: Local k6 comparison completed with temporary `/tmp` scripts only. DB load p95/p99 was 2128ms/2685ms, Redis load p95/p99 was 163ms/234ms, both with 0% failure. Redis stress at 50 VU exposed the local runtime limit with 8.3% failure, so spike was skipped.
- 2026-05-23: Removed public `source=DATABASE` selection from lookup request/docs/tests after comparison. Redis miss fallback and snapshot sync DB projection remain.
- 2026-05-23: Added experimental Redis snapshot normalized search text for local performance validation. API response shape remains unchanged; snapshot DTO/package boundary must be cleaned up in a follow-up.
- 2026-05-23: Local k6 normalized snapshot validation completed with temporary `/tmp` scripts only. Redis snapshot payload size was 2,243,998 bytes. Normalized no-sync load p95/p99 was 118ms/129ms with 0% failure. Normalized stress processed 20,166 requests with 1.98% failure and p99 timeout, so spike remains skipped.
