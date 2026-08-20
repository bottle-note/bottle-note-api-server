# Plan: 둘러보기 검색·정렬·별점 필터 계약

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 위스키와 리뷰 둘러보기의 검색·정렬·별점 필터 계약을 일관되게 확장하면서 기존 리뷰 다중 키워드 소비자를 점진적으로 전환한다.

**Architecture:** Product API 계약은 controller request → criteria → domain repository port → QueryDSL 구현의 수직 경로로 변경한다. 리뷰는 위스키 둘러보기 패턴을 참고하되 두 도메인의 평점 의미와 쿼리 구현은 분리하고, 공통화는 입력 값 규칙처럼 실제 중복이 확인된 부분에만 적용한다.

**Tech Stack:** Java 21, Spring Boot, QueryDSL, Jakarta Validation, MockMvcTester/통합 테스트, REST Docs/OpenAPI, GitHub Actions

---

## Overview

workspace #414, #415, #417을 하나의 Product 둘러보기 계약 PR로 처리한다.

- 리뷰 둘러보기에 단일 `keyword`, `sortType`, `sortOrder`를 추가한다.
- 기존 다중 `keywords`는 즉시 제거하지 않고 한시적으로 지원하며 OpenAPI에서 제거 예정으로 표시한다.
- 위스키 둘러보기에는 목록에 표시되는 집계 평점 기준 필터를 추가한다.
- 리뷰 둘러보기에는 각 리뷰의 작성 평점 기준 필터를 추가한다.
- 기존 공개 cursor 응답 구조와 인증 정책은 이 PR에서 바꾸지 않는다.

관련 이슈:
- bottle-note/workspace#414
- bottle-note/workspace#415
- bottle-note/workspace#417

## Approved Assumptions

1. Product API의 기존 endpoint 경로를 유지한다.
2. 신규 리뷰 검색 파라미터는 단일 `keyword`다.
3. legacy `keywords`는 `keyword`가 없을 때만 기존 다중 키워드 의미로 처리한다.
4. `keyword`와 `keywords`를 함께 보내면 모호성을 허용하지 않고 400 validation error를 반환한다.
5. OpenAPI에서 `keywords`는 `deprecated: true`로 표시하고 설명에 제거 예정임을 명시한다. 정확한 제거 버전·날짜는 이 PR에서 약속하지 않는다.
6. 별점 전체는 별도 문자열 값이 아니라 rating 파라미터 생략으로 표현한다.
7. rating은 `0.5` 이상 `5.0` 이하, `0.5` 단위만 유효하다. NaN, Infinity, 범위 밖 값, 0.5 단위가 아닌 값은 400이다.
8. 위스키 필터는 응답에 표시되는 집계 평점과 동일한 반올림 기준을 사용한다.
9. 리뷰 필터는 `review.reviewRating`의 정확한 값 기준이다.
10. 리뷰 기본 정렬과 지원 enum은 현재 `ReviewSortType`·`SortOrder`를 SSoT로 삼되 알 수 없는 값은 임의 기본값으로 폴백하지 않고 400으로 거부한다.
11. 각 정렬은 결정적 tie-breaker로 review/alcohol ID를 포함한다.
12. 공개 pagination 구조 전면 교체는 별도 pagination 작업이며 이 PR의 범위가 아니다. 다만 추가된 필터·정렬 조건에서 페이지 연속성과 중복·누락 방지는 검증한다.
13. DB migration, 기존 데이터 보정, FE 구현은 없다.

## Success Criteria

- 리뷰 둘러보기가 단일 keyword와 지원 정렬 type/order로 조회된다.
- keyword 미입력은 전체 리뷰 목록이며 공백 입력의 규칙이 테스트로 고정된다.
- legacy keywords 단독 요청은 기존 의미를 유지한다.
- keyword와 keywords 동시 요청은 문서화된 400 error contract를 반환한다.
- 위스키·리뷰 rating 필터가 생략/0.5/5.0 경계 및 중간값에서 동작한다.
- 유효하지 않은 rating과 sort 값은 semantic error code를 포함한 400 응답이다.
- 필터가 없으면 기존 결과 집합과 기본 정렬이 회귀하지 않는다.
- 검색·정렬·rating·cursor 조합에서 다음 페이지 중복·누락이 없다.
- `meta.searchParameters`는 실제 적용한 keyword/legacy keywords/sort/rating을 보존한다.
- REST Docs/OpenAPI가 신규 변수, 지원값, 기본값, legacy deprecation과 충돌 규칙을 명시한다.

## Impact Scope

- `bottlenote-product-api`: review/alcohol explore controller와 문서·HTTP 통합 테스트
- `bottlenote-mono`: request/criteria, sort parsing, repository port, QueryDSL 조회 구현, service
- `bottlenote-test-support` 또는 모듈 test fixture: 필요한 실제 상태 기반 fixture만 추가
- DB schema, Admin API, batch, curation, FE는 영향 없음

## Contract Decisions

### Review keyword compatibility

- `keyword != null`이고 legacy `keywords`가 비어 있으면 단일 검색을 적용한다.
- `keyword == null`이고 `keywords`만 있으면 기존 다중 검색을 적용한다.
- 둘 다 비어 있으면 검색 조건이 없다.
- 둘 다 존재하면 400이다.
- 공백-only keyword는 검색 없음으로 정규화하거나 400 중 기존 프로젝트 패턴을 조사해 하나로 고정한다. 기존 패턴과 충돌하면 가정 붕괴로 정지한다.
- legacy `keywords`의 기존 OR/AND 의미는 현재 코드와 테스트에서 확인해 그대로 보존한다. 조용히 의미를 바꾸지 않는다.

### Rating semantics

- 요청 파라미터 이름은 `rating`을 기본안으로 한다. 현재 API 명명 충돌이 발견되면 ADR을 갱신한다.
- 값은 `BigDecimal` 기반 또는 등가의 정확한 0.5-step 검증을 사용해 부동소수점 나머지 오차를 피한다.
- 위스키는 화면 표시 집계 평점 표현식을 WHERE 이전 집계에 맞는 HAVING 조건으로 적용한다. 필터 후 pagination한다.
- 리뷰는 행의 작성 평점 predicate를 pagination 전에 적용한다.

### Sort and pagination

- ReviewSortType의 각 값이 실제 QueryDSL order expression으로 연결되어야 한다.
- 방향이 바뀌면 tie-breaker ID 방향도 같은 정렬 방향으로 맞춘다.
- likes/replies 집계 정렬은 group-by cardinality와 중복 row를 검증한다.
- 현재 cursor가 offset 의미라면 공개 구조를 이번 PR에서 재설계하지 않는다. 정렬·검색·필터 context가 다음 페이지 요청에서 보존되는지는 회귀 테스트한다.

## Error Contract

- 새 Jakarta constraint의 `message`는 실제 `ValidExceptionCode` enum 이름이어야 한다.
- 알 수 없는 sort를 기존 enum의 기본값으로 숨기지 않는다.
- HTTP 400 테스트는 `errors[].code`까지 검증한다.
- keyword/keywords 충돌에 전용 코드가 필요하면 최소 enum 추가와 handler 경로를 함께 테스트한다.

## Test Strategy

Strict RED → GREEN → REFACTOR.

1. request binding/validation RED: legacy 단독, 신규 단독, 동시 입력, invalid rating/sort.
2. review query RED: keyword 없음/단일 검색, 각 sort, rating, 조합, 동률, 다음 페이지.
3. alcohol query RED: 표시 집계 평점과 rating filter 일치, 0.5/5.0, 무평점, 조합, 다음 페이지.
4. HTTP/REST Docs RED: query parameter와 meta.searchParameters, semantic 400 envelope, deprecated 문서.
5. focused GREEN 후 인접 suite와 `git diff --check`.

Mockito interaction verify를 추가하지 않는다. QueryDSL·serialization 계약은 실제 fixture/통합 경로로 검증한다.

## Non-goals

- legacy `keywords` 제거
- endpoint major version 변경
- 전체 Product pagination 전환
- RANDOM 정렬 신규 추가 또는 복원
- DB migration/데이터 보정
- FE 상태·URL 구현
- #419 위치 정보 응답

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- worker-stage: 구현 에이전트는 test source 우선 작성, 정적 RED 근거, 구현, 정적 검증, commit까지만 수행한다.
- delivery-stage: 별도 delivery 에이전트가 다른 PR CI와 겹치지 않게 push → Draft PR open → GitHub Actions CI watch를 직렬 수행한다.
- verification-order: test source/정적 RED → 구현 → static diff checks → commit → 직렬 push/PR → GitHub Actions CI watch
- ci-watch: `gh-app pr checks <PR> --watch --interval 30`, 최대 10분
- stop-conditions:
  1. 승인 가정 붕괴
  2. verify/CI 실패를 3회 안에 해결하지 못함
  3. scope 밖 행동 필요
  4. legacy keywords 실제 의미가 이 ADR과 충돌
  5. pagination 전면 재설계 없이는 정렬 연속성을 보장할 수 없음
- prohibited: merge, deploy/release, workspace issue mutation, Kubernetes/infra/secrets

## OOM Safety Policy

- 이 Hermes 머신에는 swap이 없으므로 로컬 `./gradlew`, Java compile/test, Testcontainers, Docker, IDE indexer를 실행하지 않는다.
- 병렬 에이전트는 파일 조사·편집·가벼운 JSON/텍스트 정적 검사만 수행한다.
- `git diff --check`, JSON parser, 소스 검색처럼 JVM을 띄우지 않는 검증만 로컬에서 허용한다.
- 실제 unit/integration/rule/compile 검증은 Draft PR GitHub Actions를 유일한 실행 근거로 사용한다.
- 세 구현 worker는 병렬 가능하지만 push/PR/CI watch는 한 번에 하나만 수행한다.
- 메모리 압박, 예상 밖 JVM/Docker 프로세스, CI 무한 대기가 보이면 즉시 정지하고 Progress Log에 기록한다.

## Tasks

### Task 1: 리뷰 request 호환성과 validation 계약
- Acceptance: keyword/keywords 단독 및 충돌, rating/sort 유효성, semantic 400이 executable test로 고정된다.
- Verification: focused controller/request tests RED 후 GREEN.
- Files (advisory): review explore controller/docs/request, validation enum, Product HTTP tests
- Depends: 없음
- Size: M
- Status: [x] worker complete (CI pending)

### Task 2: 리뷰 검색·정렬·별점 조회 수직 경로
- Acceptance: keyword·sort·rating 조합과 결정적 순서가 repository/service/HTTP에서 일치한다.
- Verification: QueryDSL 통합 테스트와 cursor 연속성 테스트.
- Files (advisory): review criteria, repository port/impl, service, fixtures
- Depends: Task 1
- Size: M
- Status: [x] worker complete (CI pending)

### Checkpoint: after Tasks 1-2
- [x] local static contract verification complete; compile/test는 OOM 정책에 따라 CI pending
- [x] legacy 단독·신규 단독·충돌 계약을 test source와 정적 검사로 확인

### Task 3: 위스키 표시 평점 필터 수직 경로
- Acceptance: 표시 집계 평점 기준 0.5-step 필터가 기존 검색·정렬·cursor와 조합된다.
- Verification: alcohol query/HTTP 통합 테스트.
- Files (advisory): ExploreStandard request/criteria, alcohol query supporter/repository, tests/docs
- Depends: 없음
- Size: M
- Status: [x] worker complete (CI pending)

### Task 4: 문서·회귀·Draft PR CI verify
- Acceptance: OpenAPI/REST Docs에 신규 계약과 keywords deprecated가 나타나고 Draft PR CI가 통과한다.
- Verification: JSON/OpenAPI 생성물 검사, `git diff --check`, Draft PR 후 Actions watch.
- Files (advisory): docs annotations/tests, PR body
- Depends: Tasks 2, 3
- Size: M
- Status: [ ] delivery-stage pending (worker static verification complete)

## Progress Log

- 2026-08-20: 사용자 승인. #414+#415+#417 단일 PR, legacy keywords 점진 전환, PR-first GitHub Actions verify 확정.
- 2026-08-20: worker-stage 완료. 기존 legacy `keywords`는 AND 결합임을 확인해 `keyword` 미입력 시에만 보존했고, 단일 keyword·conflict validation·review sort/rating·alcohol 표시 집계 rating 필터와 문서/test source를 반영했다. 정적 RED/GREEN·diff 검사는 통과했으며 OOM 정책에 따라 JVM/Gradle/컨테이너는 실행하지 않았다. delivery-stage push/PR/CI는 대기 중이다.
- 2026-08-20: delivery 전 정적 리뷰에서 alcohol `RANDOM` 후보 조회만 `criteria.rating()` join/HAVING을 적용하지 않아 rating을 무시하는 조합 결함을 발견했다. RANDOM+rating 회귀 test source를 먼저 추가했고, rating이 있을 때만 기존 `ratingMatches(criteria.rating())` HAVING을 쓰는 `rating` LEFT JOIN/GROUP BY 분기를 추가했다. rating 미지정 RANDOM의 기존 CRC32 order/keyset 경로는 보존했으며, 전체 정적 재검토·CI는 delivery-stage에서 수행한다.
- 2026-08-20: Draft PR #714 첫 CI는 신규 review query parameter 5개의 OpenAPI schema 누락과, ASC tie-breaker를 과거 DESC로 기대하는 기존 단위 테스트 3건 때문에 실패했다. 운영 `ORDER BY createAt ASC, id ASC`와 seek `>`는 일치하므로 query는 유지하고 OpenAPI schema·기존 테스트·상충 주석만 ADR 방향에 맞게 보정한다.
- 2026-08-20: 두 번째 CI는 `@Schema.multipleOf`에 문자열 `"0.5"`를 지정해 현재 OpenAPI annotation의 `double` 타입과 맞지 않는 컴파일 오류로 중단됐다. 세 번째 최종 시도는 이를 숫자 literal `0.5`로 고치는 한 줄 외에 코드 계약을 변경하지 않는다.
