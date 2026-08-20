# Plan: Admin·Product 큐레이션 선택 정렬 계약

> **For Hermes:** Use delegated workers for implementation; the parent manages this ADR and final delivery review.

**Goal:** Admin 큐레이션 목록과 Product 큐레이션 feed가 노출 시작일 또는 수동 노출 순서를 요청 방향으로 정렬하도록 공통 계약을 제공한다.

**Architecture:** 공통 `CurationSortType(EXPOSURE_START_DATE, DISPLAY_ORDER)`과 기존 `SortOrder(ASC, DESC)`를 Admin/Product request에서 받고 repository ordering과 Product cursor context/seek에 수직 전달한다. 기본값은 `EXPOSURE_START_DATE + DESC`이며, nullable 날짜는 방향과 무관하게 null-last, tie-breaker는 요청 방향의 ID다.

**Tech Stack:** Java 21, Spring Boot, JPA/JPQL, QueryDSL, keyset cursor codec, MockMvcTester/통합 테스트, OpenAPI, GitHub Actions

---

## Overview

workspace #410과 Draft PR #715를 재개봉한다.

live main의 기존 동작은 Admin/Product 모두 `displayOrder ASC, id ASC`였다. #715 첫 구현은 이를 고정 `exposureStartDate DESC, null-last, id DESC`로 바꿨으나, 사용자가 정렬 선택 request를 요구해 해당 가정은 폐기됐다.

재승인된 목표는 Admin/Product 모두 `sortType`과 `sortOrder`를 받아 동일한 정렬 matrix를 제공하는 것이다. 기본 동작은 `EXPOSURE_START_DATE + DESC`이고, 기존 수동 노출 순서는 `DISPLAY_ORDER`로 명시적으로 선택할 수 있다.

관련 이슈:
- bottle-note/workspace#410
- bottle-note/bottle-note-api-server#715

## Approved Assumptions

1. 공통 정렬 enum은 `EXPOSURE_START_DATE`, `DISPLAY_ORDER` 두 값만 지원한다.
2. 방향은 기존 공통 `SortOrder.ASC`, `SortOrder.DESC`를 사용한다.
3. Admin/Product request의 누락 기본값은 `EXPOSURE_START_DATE + DESC`다.
4. “큐레이션 날짜”는 `Curation.exposureStartDate`이며 spec payload 날짜는 정렬 SSoT가 아니다.
5. `EXPOSURE_START_DATE`는 ASC/DESC 모두 null-last이고, 동일 날짜/null bucket은 요청 방향의 `id`로 결정한다.
6. `DISPLAY_ORDER`는 요청 방향으로 정렬하고 동일 값은 같은 방향의 `id`로 결정한다.
7. Admin page/size와 Product opaque cursor envelope는 유지한다.
8. Product cursor context에 sortType/sortOrder를 포함해 다른 정렬 cursor의 재사용을 거부한다.
9. 기존 배포 cursor 호환 의무는 없으며 진행 중 페이지는 재조회할 수 있다.
10. DB migration·데이터 보정은 없다.
11. visibility와 keyword/spec code 필터는 정렬 전에 그대로 적용한다.

## Success Criteria

- Admin/Product가 sortType·sortOrder를 동일 enum과 기본값으로 받는다.
- `EXPOSURE_START_DATE` ASC/DESC가 null-last와 같은 방향 ID tie-breaker로 동작한다.
- `DISPLAY_ORDER` ASC/DESC가 같은 방향 ID tie-breaker로 동작한다.
- Product cursor가 네 정렬 조합에서 중복·누락 없이 이어진다.
- 다른 sortType/order로 생성한 cursor는 기존 invalid-cursor contract로 거부된다.
- keyword/code/visibility 필터 적용 후 정렬·pagination한다.
- Admin page metadata와 Product cursor JSON envelope는 회귀하지 않는다.
- OpenAPI에 query enum/default/null/tie-breaker가 명시된다.

## Impact Scope

- `bottlenote-mono`: 공통 sort enum, Admin/Product request·criteria, repository, cursor context/service, Fake/InMemory, tests
- `bottlenote-admin-api`: Admin curation HTTP/OpenAPI/tests
- `bottlenote-product-api`: Product feed HTTP/OpenAPI/tests
- Draft PR #715 ADR·제목·본문

비영향:
- DB schema/migration/backfill
- curation payload/spec JSON
- visibility/만료 정책
- response item/envelope
- FE UI

## Ordering Contract

기본 request:

```txt
sortType=EXPOSURE_START_DATE
sortOrder=DESC
```

### EXPOSURE_START_DATE

- null rank는 ASC/DESC 모두 마지막이다.
- ASC: `exposureStartDate ASC, id ASC`
- DESC: `exposureStartDate DESC, id DESC`
- null bucket: `id ASC` 또는 `id DESC`로 이어진다.
- DB 기본 null ordering에 의존하지 않고 명시적 CASE/QueryDSL null rank를 사용한다.

### DISPLAY_ORDER

- ASC: `displayOrder ASC, id ASC`
- DESC: `displayOrder DESC, id DESC`
- `displayOrder`는 non-null 기존 컬럼이다.

### Product keyset cursor

- seek predicate와 orderBy는 같은 tuple 방향을 사용한다.
- cursor context에 최소 `sortType`, `sortOrder`, 기존 filter context가 포함된다.
- keys에는 sortType별 primary value와 `id`를 담는다.
- 날짜 정렬은 null bucket 전환과 null-tail을 처리한다.
- displayOrder는 방향별 값 비교 후 동률 ID를 같은 방향으로 비교한다.
- HMAC/opaque string과 invalid cursor error contract는 유지한다.

## Test Strategy

Strict RED → GREEN → REFACTOR. 이 Hermes 머신에서는 OOM 정책상 JVM을 실행하지 않으므로 worker는 test-source-first와 정적 RED 근거를 만들고, 실제 RED/GREEN은 Draft PR GitHub Actions에서 확인한다.

1. Request: Admin/Product enum binding, 기본값, invalid value 400.
2. Admin: 두 sortType × 두 방향, 동일 값, null 날짜, page 경계.
3. Product: 같은 4개 matrix, filter-before-pagination, size+1, 날짜→null/null-tail.
4. Cursor: context에 sortType/order 포함, 조합 불일치 거부, null date.
5. HTTP/OpenAPI: query schema/default와 공개 response envelope 유지.
6. 기존 고정 정렬 기대 테스트는 삭제하지 않고 선택 정렬 matrix의 해당 case로 전환한다.

Mockito를 사용하지 않고 실제 repository/fixture 또는 Fake/InMemory 상태·결과를 검증한다.

## Compatibility and Data

- query parameter는 additive 변경이다.
- 파라미터 미지정 기본 순서는 #410 목표인 날짜 최신순으로 바뀐다.
- 기존 수동 순서는 `DISPLAY_ORDER + ASC`로 명시적 요청 가능하다.
- Product cursor 내부 key/context 변경은 공개 JSON envelope 변경이 아니다.
- 배포 이전 cursor 재사용은 보장하지 않는다.
- 기존 displayOrder와 exposureStartDate 값은 수정하지 않는다.

## Non-goals

- `CREATED_AT` 등 추가 sortType
- displayOrder 컬럼 삭제
- payload의 eventStartDate 정렬
- DB migration/backfill
- visibility/만료 정책 변경
- FE 정렬 UI
- 다른 Product 목록 pagination 변경

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- worker-stage: test source 우선 → 정적 RED 근거 → 최소 구현 → 정적 검증 → App commit
- delivery-stage: 기존 Draft PR #715 push → GitHub Actions watch
- ci-watch: `gh-app pr checks 715 --watch --interval 30`, 최대 10분
- stop-conditions:
  1. 승인 가정 붕괴
  2. verify/CI 실패를 3회 안에 해결하지 못함
  3. scope 밖 행동 필요
  4. 네 정렬 조합을 opaque cursor로 안전하게 표현할 수 없어 공개 envelope 변경이 필요함
- prohibited: merge, deploy/release, workspace issue mutation, Kubernetes/infra/secrets

## OOM Safety Policy

- 로컬 `./gradlew`, Java/JVM, Testcontainers, Docker를 실행하지 않는다.
- `git diff --check`, 소스 assertion, JSON/text parser만 허용한다.
- 실제 compile/unit/integration/rule 검증은 Draft PR GitHub Actions를 실행 근거로 사용한다.

## Tasks

### Task 1: 공통 request·enum 계약
- Acceptance: Admin/Product가 두 sortType과 두 방향을 받고 기본값과 invalid contract를 공유한다.
- Status: [x] worker static complete (JVM 미실행)

### Task 2: Admin/Product 정렬 matrix와 Product keyset
- Acceptance: 네 조합의 실제 순서·tie-breaker·cursor가 공통 계약과 일치한다.
- Status: [x] worker static complete (JVM 미실행)

### Checkpoint
- [x] source-level Admin/Product 2×2 ordering matrix 반영
- [x] source-level nullable date seek 및 cursor context 경계 반영

### Task 3: 문서·호환성·Draft PR CI verify
- Acceptance: OpenAPI와 PR 설명이 선택 정렬 계약을 노출하고 #715 CI가 통과한다.
- Status: [x] 문서 static complete; [ ] CI/delivery pending

## Progress Log

- 2026-08-20: live main에서 기존 `displayOrder ASC, id ASC` keyset을 확인했다.
- 2026-08-20: 최초 승인에 따라 고정 `exposureStartDate DESC/null-last/id DESC` 구현과 Draft PR #715를 만들었다.
- 2026-08-20: 첫 #715 CI는 기존 displayOrder 기대 통합 테스트에서 실패했다.
- 2026-08-20: 사용자가 고정 정렬이 아니라 정렬 방향+속성 request를 요구해 가정이 붕괴했다. 실패 테스트의 단순 기대값 수정은 중단했다.
- 2026-08-20: `sortType={EXPOSURE_START_DATE, DISPLAY_ORDER}`, `sortOrder={ASC,DESC}`, 기본 날짜 DESC 계약으로 재개봉·재승인했다.
- 2026-08-21: worker commit `10871a13` 후 fresh-context 독립 리뷰에서 Product 일반 목록 `/api/v2/curations`의 범위 밖 정렬 변경과 OpenAPI query enum/default schema 누락을 blocker로 확인했다. 원격 push를 중단하고 일반 목록 displayOrder 계약 복원, OpenAPI artifact 계약, 네 조합 cursor 연속성 및 invalid sortOrder 400만 보완한다.
- 2026-08-21: test-source-first로 request 기본값/enum, Admin·Product ordering matrix, Product cursor 연속성·context 불일치, Product HTTP enum binding/400, OpenAPI 설명을 추가했다. 기존 고정 DESC 구현에는 enum/overload/context/matrix가 없어 정적 RED를 확인했다.
- 2026-08-21: QueryDSL/JPA/InMemory seek·order·cursor keys를 선택 정렬로 전환하고 정적 GREEN source/model/diff 검증을 수행했다. OOM 정책에 따라 Gradle/JVM/Testcontainers/Docker는 실행하지 않았으며 compile/unit/integration/OpenAPI 생성/CI는 delivery-stage 검증 gap으로 남긴다.
- 2026-08-21: 선택 정렬 PR 갱신 후 첫 CI가 `ComparableExpressionBase<T>`에 존재하지 않는 `gt/lt` 호출로 compile 단계에서 실패했다. 날짜·숫자 QueryDSL path별 typed seek helper로 분리하는 최소 수정 후 2차 CI를 수행한다.
- 2026-08-21: 2차 CI는 compile을 통과하고 7개 규칙/계약 실패를 노출했다. enum constant 패키지, service 최대 5개 인자, OpenAPI enum 순서 비의존 assertion, 기본 날짜 DESC 페이지 기대값, 기존 invalid-enum 400 의미를 수정한 뒤 fresh 리뷰를 거쳐 세 번째 최종 CI를 수행한다.
- 2026-08-21: 세 번째 최종 CI는 `ProductSpecBasedCurationService` request-object 전환 후 lambda가 제거된 지역 변수 `keyword`를 참조해 mono compile 단계에서 실패했다. verify 3회 stop-condition에 도달했으므로 추가 수정·push를 중단한다.
- 2026-08-21: 사용자가 Terra level PR 수정을 명시해 정지를 해제했다. `keyword`를 `request.keyword()`로 교체하는 확인된 한 줄만 수정하고 CI를 재검증한다.
