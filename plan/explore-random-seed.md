# Plan: explore RANDOM 정렬 seed 파라미터 지원

## Overview

`GET /api/v1/alcohols/explore/standard` 의 `sortType=RANDOM` 정렬이 현재 `ORDER BY RAND()` 만 사용해 매 요청마다 순서가 재생성된다. 그 결과 커서(offset) 기반 페이지네이션에서 "다음 페이지" 요청 시 이전 페이지와 중복/누락이 발생한다.

본 기능은 요청 파라미터에 **optional `Long seed`** 를 추가하여, 클라이언트가 세션 내내 동일한 seed 를 재사용하면 순서 일관성이 유지되도록 한다. seed 가 없으면 서버가 생성하여 응답 meta 에 실어 내려주므로, 클라이언트는 첫 응답의 seed 를 이후 페이지 요청에 그대로 태워 보내면 된다.

## Assumptions

1. seed 는 `SearchSortType.RANDOM` 일 때만 의미 있음. 다른 정렬 타입에서는 무시한다.
2. 파라미터는 `ExploreStandardRequest` 에 `Long seed` 필드로 추가 (query string).
3. seed 미전송 시 서버가 `ThreadLocalRandom.current().nextLong()` 으로 생성하여 응답 meta 에 포함.
4. seed 전송 시 그대로 사용: `ORDER BY RAND(:seed), alcohol.id ASC` (tiebreaker 로 id 추가).
5. 응답 구조: 기존 `MetaInfos` 에 `meta.add("seed", seed)` 로 추가. 별도 응답 DTO 신설하지 않음.
6. 페이지네이션은 현재 offset 기반 유지. `cursor` 파라미터는 기존대로 offset 으로 사용.
7. seed 미전송 시 연속 호출마다 새 seed 가 생성되므로 1~2페이지 중복 노출 가능 — "seed 없이 = 새 탐색 세션" 계약으로 문서화하고 수용.
8. 검증: seed 는 `Long` 전 범위 허용 (음수/0 모두 유효).
9. Breaking change 없음. 기존 클라이언트는 seed 없이 호출하던 대로 동작, 응답 meta 에 seed 가 추가될 뿐이다.

## Success Criteria

- **SC-1**: `GET /api/v1/alcohols/explore/standard?sortType=RANDOM&seed=123&size=10` 을 동일 seed 로 두 번 호출하면 결과 알코올 ID 리스트가 완전히 동일하다.
- **SC-2**: `GET /api/v1/alcohols/explore/standard?sortType=RANDOM&seed=123&cursor=0&size=10` 과 `cursor=10&size=10` 의 결과를 합치면 `cursor=0&size=20` 의 결과와 일치한다 (페이지 간 중복/누락 없음).
- **SC-3**: seed 를 전송하지 않으면 응답 `meta.seed` 에 Long 값이 포함된다.
- **SC-4**: seed 를 전송하면 응답 `meta.seed` 에 요청 seed 가 그대로 에코된다.
- **SC-5**: `sortType=POPULAR` 등 RANDOM 이외 정렬에 seed 를 보내도 쿼리/결과에 영향 없음 (무시됨).
- **SC-6**: 생성된 SQL 에 `RAND(?)` 파라미터 바인딩이 포함되고, seed 가 없을 때도 서버 생성 seed 가 바인딩된다.
- **SC-7**: REST Docs (`explore.standard.adoc`) 에 seed 파라미터 및 응답 meta 필드가 문서화된다.
- **SC-8**: 기존 통합 테스트가 모두 통과하고, seed 동작을 검증하는 신규 통합 테스트 시나리오가 추가된다.

## Impact Scope

### 영향 모듈
- `bottlenote-mono` — DTO, Criteria, Repository, Supporter
- `bottlenote-product-api` — Controller, RestDocs, 통합 테스트

### 변경 파일
**production**
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/request/ExploreStandardRequest.java` — `Long seed` 필드 추가
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/dsl/ExploreStandardCriteria.java` — `Long seed` 필드 추가
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/AlcoholQuerySupporter.java` — `sortByRandom(Long seed)` 오버로드 (기존 시그니처 변경 또는 신규 메서드)
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/CustomAlcoholQueryRepositoryImpl.java` — RANDOM 분기에 seed 전달 + `alcohol.id.asc()` tiebreaker 추가
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AlcoholQueryService.java` — seed 미전송 시 생성, 결과에 seed 반영 경로
- `bottlenote-product-api/src/main/java/app/bottlenote/alcohols/controller/AlcoholExploreController.java` — 응답 meta 에 seed 추가
- `bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc` — seed 파라미터/meta 문서화

**test**
- `bottlenote-product-api/src/test/java/.../alcohols/integration/AlcoholExploreControllerIntegrationTest.java` — seed 일관성/페이지 분할 시나리오 추가
- `bottlenote-product-api/src/test/java/.../alcohols/RestAlcoholExploreControllerTest.java` — RestDocs 테스트에 seed 필드 반영

### 도메인/엔티티/이벤트/캐시
- 엔티티 스키마 변경: **없음** (Liquibase 마이그레이션 불필요)
- 도메인 이벤트: **없음**
- 캐시: **없음** (RANDOM 정렬은 캐시 대상 아님)

### 테스트 종류
- 통합 테스트: seed 동일성, 페이지 분할 합산, seed 미전송 시 meta 포함 검증 (TestContainers)
- RestDocs 테스트: 파라미터/응답 필드 문서화

### 리스크
- `RAND(seed)` 는 MySQL 전용 함수 — TestContainers 로 MySQL 사용 중이므로 문제 없음. H2 는 테스트에서 사용하지 않음을 확인.
- seed tiebreaker 로 `alcohol.id.asc()` 를 추가하면 동일 `RAND(seed)` 값 충돌 시에도 결정론적 순서 보장.
- offset 기반 페이징 유지이므로 대규모 offset 시 성능 저하는 기존과 동일 (본 기능이 악화시키지 않음).

## Tasks

### Task 1: DTO/Criteria 에 seed 필드 추가 [완료]
- 수용 기준:
  - `ExploreStandardRequest` 에 `Long seed` 필드 추가 (optional, `@Nullable`)
  - `ExploreStandardCriteria` 에 동일 필드 전파, `of(request, userId)` 팩토리에서 매핑
  - seed 미전송 시 `null` 유지 (기본값 주입은 Service 계층에서 처리)
- 검증: `./gradlew :bottlenote-mono:compileJava :bottlenote-mono:compileTestJava`
- 파일:
  - `bottlenote-mono/.../alcohols/dto/request/ExploreStandardRequest.java`
  - `bottlenote-mono/.../alcohols/dto/dsl/ExploreStandardCriteria.java`
- 크기: S
- 상태: [x] 완료

### Task 2: Supporter + Repository RANDOM 분기에 seed 바인딩 + id tiebreaker
- 수용 기준:
  - `AlcoholQuerySupporter.sortByRandom(long seed)` 시그니처 변경 (또는 오버로드) — `function('rand', :seed)` 바인딩
  - `CustomAlcoholQueryRepositoryImpl.fetchCandidateIds` 의 RANDOM 분기에서 seed 전달 + `.orderBy(sortByRandom(seed), alcohol.id.asc())` 로 tiebreaker 추가
  - 기존 `sortBy(sortType, sortOrder)` 경로의 RANDOM 케이스(`AlcoholQuerySupporter:167`)도 일관되게 정리
  - 생성 SQL 에 `rand(?)` 파라미터 바인딩 확인
- 검증: `./gradlew :bottlenote-mono:compileJava`
- 파일:
  - `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java`
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java`
- 크기: S
- 상태: [x] 완료

### Checkpoint: Task 1-2 완료 후
- [ ] `./gradlew :bottlenote-mono:compileJava` 통과
- [ ] `./gradlew check_rule_test` 아키텍처 규칙 통과

### Task 3: Service + Controller 에서 seed 생성 및 응답 meta 반영
- 수용 기준:
  - `AlcoholQueryService.getStandardExplore` 가 request.seed 가 null 이면 `ThreadLocalRandom.current().nextLong()` 으로 생성, 최종 사용된 seed 를 반환 구조에 담는다 (반환 타입은 `CursorResponse` 유지 — seed 는 Service 밖으로 꺼내는 경로만 확보; 예: 결과 record 에 `usedSeed` 필드 추가 또는 Controller 가 request 를 후처리)
  - `AlcoholExploreController.getStandardExplore` 가 최종 seed 를 `meta.add("seed", seed)` 로 응답 meta 에 포함
  - 비-RANDOM 정렬에서는 seed 가 쿼리에 영향 주지 않음 (Service 에서 분기)
  - seed 생성 시점은 Service 진입 직후 1회만 — 동일 요청 내 재계산 없음
- 검증: `./gradlew :bottlenote-mono:compileJava :bottlenote-product-api:compileJava`
- 파일:
  - `bottlenote-mono/.../alcohols/service/AlcoholQueryService.java`
  - `bottlenote-product-api/.../alcohols/controller/AlcoholExploreController.java`
  - (필요 시) `bottlenote-mono/.../alcohols/dto/dsl/ExploreStandardCriteria.java` — `resolvedSeed` 반영
- 크기: M
- 상태: [x] 완료

### Task 4: 기존 통합 테스트 확장 + seed 시나리오 추가
- 수용 기준:
  - `AlcoholExploreControllerIntegrationTest` 에 다음 시나리오 추가 (기존 클래스 확장):
    - 동일 seed 두 번 호출 시 결과 알코올 ID 리스트 동일 (SC-1)
    - 동일 seed, `cursor=0&size=N` + `cursor=N&size=N` 결과 합 = `cursor=0&size=2N` 결과 (SC-2)
    - seed 미전송 시 응답 `meta.seed` 에 Long 값 포함 (SC-3)
    - seed 전송 시 응답 `meta.seed` 에 요청값 에코 (SC-4)
    - 비-RANDOM 정렬(예: POPULAR) 에 seed 를 보내도 결과 영향 없음 (SC-5)
  - 기존 RANDOM 관련 시나리오(L79-80) 가 seed 변경 후에도 통과
  - 신규 시나리오는 `@ParameterizedTest` 또는 `@Nested` 로 기존 구조 따라감
- 검증: `./gradlew :bottlenote-product-api:integration_test --tests "*AlcoholExploreControllerIntegrationTest*"`
- 파일:
  - `bottlenote-product-api/src/test/.../alcohols/integration/AlcoholExploreControllerIntegrationTest.java`
- 크기: M
- 상태: [x] 완료

### Task 5: RestDocs 테스트 확장 + `explore.standard.adoc` 문서 주석 확장
- 수용 기준:
  - `RestAlcoholExploreControllerTest` 에 seed 필드 반영 — 요청 파라미터 snippet 과 응답 meta snippet 에 `seed` 필드 문서화
  - `bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc` 에 다음 추가:
    - seed 파라미터 설명 (RANDOM 정렬 시에만 유효, 미전송 시 서버 생성)
    - 응답 meta.seed 필드 설명
    - seed 재사용을 통한 페이지 일관성 확보 패턴 주석 (클라이언트 가이드)
    - 샘플 HTTP 요청/응답에 seed 포함
  - `./gradlew asciidoctor` 실행 시 문서 생성 성공
- 검증: `./gradlew :bottlenote-product-api:test --tests "*RestAlcoholExploreControllerTest*" && ./gradlew :bottlenote-product-api:asciidoctor`
- 파일:
  - `bottlenote-product-api/src/test/.../alcohols/RestAlcoholExploreControllerTest.java`
  - `bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc`
- 크기: M
- 상태: [x] 완료

### Checkpoint: Task 3-5 완료 후 (최종)
- [ ] `./gradlew unit_test` 통과
- [ ] `./gradlew integration_test` 통과
- [ ] `./gradlew check_rule_test` 통과
- [ ] `./gradlew asciidoctor` 문서 생성 성공
- [ ] `/verify full` (또는 `/verify l3`) 통과

## Progress Log

### 2026-04-24

- **Task 1**: `ExploreStandardRequest.seed` 필드 추가, `ExploreStandardCriteria` 에 3-arg `of(request, userId, seed)` 도입. 컴파일 통과.
- **Task 2**: `AlcoholQuerySupporter.sortByRandom(long seed)` 오버로드 추가(`function('rand', :seed)` 바인딩). `CustomAlcoholQueryRepositoryImpl.fetchCandidateIds` RANDOM 분기에 seed 전달 + `alcohol.id.asc()` tiebreaker 적용. 컴파일 통과.
- **Task 3**: `AlcoholQueryService.getStandardExplore` 가 `ExploreStandardResponse`(seed + page) 반환하도록 변경. RANDOM 일 때만 `ThreadLocalRandom.nextLong()` 생성, 그 외엔 0 고정. 컨트롤러가 `meta.add("seed", ...)` 로 에코. 아키텍처 규칙에 맞춰 래퍼 네이밍 `Result` → `Response` 정정.
- **Task 4**: `AlcoholExploreControllerIntegrationTest` 에 `@Nested RandomSeed` 추가(5 시나리오): 동일 seed 순서 일치 / 페이지 분할 합산 / seed 미전송 시 meta 생성 / seed 에코 / 비-RANDOM 정렬 무시. 21/21 통합 테스트 통과.
- **Task 5**: RestDocs 테스트 요청 파라미터/응답 필드에 `seed`, `meta.seed`, `meta.searchParameters.seed` 추가. `explore.standard.adoc` 에 seed 운용 가이드 섹션 확장. `asciidoctor` 빌드 성공.

### 검증 결과

- `./gradlew unit_test check_rule_test` → BUILD SUCCESSFUL
- `./gradlew :bottlenote-mono:test --tests "*ExploreStandardQueryStructureTest*"` → PASSED (heavy 서브쿼리 1단계 침투 방지 유지)
- `./gradlew :bottlenote-product-api:test --tests "*RestAlcoholExploreControllerTest*"` → PASSED
- `./gradlew :bottlenote-product-api:integration_test --tests "*AlcoholExploreControllerIntegrationTest*"` → 21 tests, 0 failures
- `./gradlew :bottlenote-product-api:asciidoctor` → BUILD SUCCESSFUL
- `./gradlew build -x test -x integration_test -x asciidoctor` → BUILD SUCCESSFUL
