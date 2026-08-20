# Plan: 리뷰 둘러보기 위치 정보 응답 계약

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 리뷰 둘러보기 카드 응답에 리뷰 작성 시 저장된 위치 정보를 조건부로 제공한다.

**Architecture:** 별도 Bar 도메인이나 외부 조인을 만들지 않고 `Review.reviewLocation`을 기존 `LocationInfo` 응답 계약으로 투영한다. QueryDSL projection과 Product HTTP serialization을 최소 변경하며 위치 미입력 상태는 명시적 null 계약으로 고정한다.

**Tech Stack:** Java 21, Spring Boot, JPA/QueryDSL, MockMvcTester/통합 테스트, REST Docs/OpenAPI, GitHub Actions

---

## Overview

workspace #419는 “바 엔티티 연결”이 아니라 리뷰에 이미 저장된 주소/상호 정보를 리뷰 둘러보기 응답에 제공하는 작업이다.

- 소스는 `Review.reviewLocation`이다.
- 기존 `app.bottlenote.review.facade.payload.LocationInfo`의 필드 구조를 재사용한다.
- 위치가 없는 리뷰는 `locationInfo: null`을 반환한다.
- 검색·정렬·별점·pagination 의미는 변경하지 않는다.

관련 이슈:
- bottle-note/workspace#419

## Approved Assumptions

1. 별도 Bar 엔티티, bar ID, bar repository, 외부 API는 없다.
2. 위치 정보는 review row의 embedded `ReviewLocation` 컬럼이 SSoT다.
3. 응답 필드명은 기존 상세 계약과 일관된 `locationInfo`다.
4. 필드 구조는 `locationName`, `zipCode`, `address`, `detailAddress`, `category`, `mapUrl`, `latitude`, `longitude`를 가진 기존 `LocationInfo`를 재사용한다.
5. 모든 위치 컬럼이 비어 있으면 `locationInfo`는 빈 객체가 아니라 null이다.
6. 일부 컬럼만 존재하면 객체를 반환하고 각 미입력 필드는 null이다.
7. 위치 정보는 keyword 검색 범위에 추가하지 않는다.
8. 기존 리뷰 데이터 backfill/migration은 없다.
9. 인증 정책, endpoint 경로, pagination, 정렬은 바꾸지 않는다.
10. #414/#417 PR과 분리하며, 구현 시 live main에 해당 PR이 아직 없더라도 현재 응답 경로에 독립적으로 적용 가능해야 한다.

## Success Criteria

- 위치가 완전하게 저장된 리뷰의 둘러보기 item에 locationInfo 전체가 반환된다.
- 위치가 없는 리뷰는 JSON에 `"locationInfo": null`로 반환된다.
- 일부 위치 필드만 있는 리뷰는 존재하는 값을 보존하며 빈 문자열을 임의 생성하지 않는다.
- 목록 QueryDSL join/group cardinality와 review item 수가 변하지 않는다.
- keyword·현재 정렬·pagination 결과가 위치 projection 추가 전과 동일하다.
- REST Docs/OpenAPI에 locationInfo와 nullable 규칙이 명시된다.
- 기존 review detail/create/update 위치 계약은 변경하지 않는다.

## Impact Scope

- `bottlenote-mono`: ReviewLocation → LocationInfo 변환, ReviewExploreItem, QueryDSL projection
- `bottlenote-product-api`: review explore HTTP/REST Docs와 통합 테스트
- DB schema, review write path, Admin API, FE는 영향 없음

## Contract Decisions

### Response shape

```json
{
  "locationInfo": {
    "locationName": "도시술",
    "zipCode": "12345",
    "address": "서울 송파구 송파대로 145",
    "detailAddress": "2층",
    "category": "음식점 > 술집",
    "mapUrl": "https://example.com/place",
    "latitude": "37.0000",
    "longitude": "127.0000"
  }
}
```

위치가 없을 때:

```json
{
  "locationInfo": null
}
```

- class-wide NON_NULL 설정으로 다른 필드의 serialization을 바꾸지 않는다.
- DTO 생성 시 embedded 객체가 존재해도 모든 값이 null이면 null로 정규화한다.
- address 또는 locationName 하나만 존재하는 partial state도 그대로 반환한다.

### Query behavior

- 별도 join을 추가하지 않는다. embedded 컬럼을 기존 review projection에 포함한다.
- group-by가 필요한 구현이면 선택한 모든 비집계 위치 컬럼을 올바르게 처리하되 row duplication을 만들지 않는다.
- total count query에는 위치 projection을 추가하지 않는다.

## Test Strategy

Strict RED → GREEN → REFACTOR.

1. Query projection RED: full location, no location, partial location.
2. HTTP serialization RED: object/null의 정확한 JSON shape.
3. Regression RED: 같은 fixture의 item count/order/cursor가 유지된다.
4. REST Docs/OpenAPI RED: 모든 중첩 필드와 nullable 설명.
5. focused GREEN 후 `git diff --check` 및 범위 확인.

Mockito interaction verify를 추가하지 않는다. 실제 Review fixture와 QueryDSL/HTTP 경로를 검증한다.

## Non-goals

- Bar 도메인·bar ID·bar 검색
- 위치 기반 검색/필터/정렬
- 주소 정규화·좌표 검증 변경
- 기존 review write API 변경
- DB migration/backfill
- FE 렌더링
- #414/#415/#417 구현

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
  4. 기존 LocationInfo 재사용이 외부 계약을 불필요하게 확장하거나 민감 필드를 노출함
- prohibited: merge, deploy/release, workspace issue mutation, Kubernetes/infra/secrets

## OOM Safety Policy

- 이 Hermes 머신에는 swap이 없으므로 로컬 `./gradlew`, Java compile/test, Testcontainers, Docker, IDE indexer를 실행하지 않는다.
- 병렬 에이전트는 파일 조사·편집·가벼운 JSON/텍스트 정적 검사만 수행한다.
- `git diff --check`, JSON parser, 소스 검색처럼 JVM을 띄우지 않는 검증만 로컬에서 허용한다.
- 실제 unit/integration/rule/compile 검증은 Draft PR GitHub Actions를 유일한 실행 근거로 사용한다.
- 세 구현 worker는 병렬 가능하지만 push/PR/CI watch는 한 번에 하나만 수행한다.
- 메모리 압박, 예상 밖 JVM/Docker 프로세스, CI 무한 대기가 보이면 즉시 정지하고 Progress Log에 기록한다.

## Tasks

### Task 1: 위치 projection과 null 정규화
- Acceptance: full/null/partial 위치가 ReviewExploreItem에서 승인된 shape로 표현된다.
- Verification: repository/service focused test RED 후 GREEN.
- Files (advisory): ReviewLocation, LocationInfo, ReviewExploreItem, CustomReviewRepositoryImpl, fixture/test
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: HTTP 문서·회귀·Draft PR CI verify
- Acceptance: 실제 JSON과 OpenAPI가 object/null 계약을 표현하며 item count/order가 회귀하지 않고 CI가 통과한다.
- Verification: Product HTTP/REST Docs 테스트, `git diff --check`, Draft PR 후 Actions watch.
- Files (advisory): review explore controller docs/tests, PR body
- Depends: Task 1
- Size: M
- Status: [x] worker complete (local JVM/CI verification은 delivery-stage)

## Progress Log

- 2026-08-20: 사용자 승인. #419 별도 PR, Bar 연결이 아닌 Review.reviewLocation 노출, PR-first GitHub Actions verify 확정.
- 2026-08-20: static RED 기록. `LocationInfoTest`는 아직 없는 `LocationInfo.from(ReviewLocation)`을 호출하고, `ReviewExploreLocationIntegrationTest`는 `$.data.items[0].locationInfo` object/null 및 partial-null JSON을 요구한다. 이 시점 `ReviewExploreItem`에는 `locationInfo` 필드가 없고 `CustomReviewRepositoryImpl#getStandardExplore` select/tuple 변환에도 `review.reviewLocation` 또는 위치 컬럼 투영이 없어 계약을 만족하지 않는다. OOM 정책상 JVM 테스트는 실행하지 않았다.
- 2026-08-20: static GREEN 검토. `ReviewExploreItem`에 `LocationInfo locationInfo`를 추가하고, 둘러보기 select/groupBy/tuple mapping에 ReviewLocation 8개 컬럼만 포함했다. keyword predicate, orderBy, cursor, fetch size, joins, count query는 변경하지 않았으며 `git diff --check`와 신규 파일 whitespace 검사를 통과했다. JVM/컨테이너 실행은 OOM 정책에 따라 미실행이다.
