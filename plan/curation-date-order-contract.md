# Plan: Admin·Product 큐레이션 날짜 최신순 정렬 계약

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Admin 큐레이션 목록과 Product 큐레이션 feed를 노출 시작일 최신순으로 일관되게 반환한다.

**Architecture:** `Curation.exposureStartDate`를 공통 정렬 SSoT로 사용하고 null-last 및 ID tie-breaker를 쿼리와 cursor seek에 동일하게 적용한다. Admin page/size와 Product opaque keyset cursor의 공개 구조는 유지하되 내부 정렬 key를 날짜 기준으로 전환한다.

**Tech Stack:** Java 21, Spring Boot, JPA/JPQL, QueryDSL, keyset cursor codec, MockMvcTester/통합 테스트, REST Docs/OpenAPI, GitHub Actions

---

## Overview

workspace #410을 단독 PR로 처리한다.

live main 확인 결과 현재 동작은 이슈 본문의 `createdAt` 정렬이 아니라 다음과 같다.

- Admin: `displayOrder ASC, id ASC`
- Product feed: `displayOrder ASC, id ASC` keyset seek

승인된 목표는 두 경로 모두 `exposureStartDate DESC`, null-last, `id DESC`로 교체하는 것이다. 요청자가 선택하는 sort 파라미터는 추가하지 않고 해당 목록의 고정 계약으로 노출한다.

관련 이슈:
- bottle-note/workspace#410

## Approved Assumptions

1. “큐레이션 날짜”는 공통 엔티티 컬럼 `Curation.exposureStartDate`다.
2. PROGRAM payload의 `eventStartDate` 등 spec별 payload 날짜는 정렬 SSoT가 아니다.
3. 최신 날짜가 먼저다: `exposureStartDate DESC`.
4. exposureStartDate가 null인 항목은 날짜가 있는 모든 항목 뒤에 위치한다.
5. 동일 날짜 또는 둘 다 null이면 `id DESC`가 tie-breaker다.
6. 기존 displayOrder는 저장·수정 필드로 남지만 Admin 목록/Product feed의 이 조회 순서에는 사용하지 않는다.
7. Admin은 기존 page/size 구조를 유지한다.
8. Product feed는 현재 opaque keyset cursor 구조를 유지하되 cursor가 새 정렬 key와 null bucket을 안전하게 표현해야 한다.
9. 이전 displayOrder cursor를 새 구현이 해석해야 하는 호환 의무는 없다. 배포 시점에 진행 중이던 페이지 cursor는 재조회할 수 있다.
10. sort 선택 query parameter는 추가하지 않는다. OpenAPI 설명으로 고정 정렬 계약을 노출한다.
11. DB migration과 데이터 보정은 없다.
12. visibility 필터와 keyword/spec code 필터는 정렬 전에 그대로 적용한다.

## Success Criteria

- Admin 목록이 날짜 DESC → null-last → id DESC 순으로 반환된다.
- Product feed가 같은 순서로 반환된다.
- 날짜와 createdAt/displayOrder가 충돌하는 fixture에서도 날짜 기준 결과가 우선한다.
- 동일 날짜와 null 날짜의 순서가 결정적이다.
- Product 다음 cursor가 날짜가 있는 구간, 날짜→null 경계, null 구간에서 중복·누락 없이 이어진다.
- keyword/code/visibility 필터 적용 후 정렬·pagination한다.
- Admin 응답 page metadata와 Product feed 공개 cursor JSON 구조는 회귀하지 않는다.
- OpenAPI/REST Docs에 고정 정렬 key, 방향, null, tie-breaker가 명시된다.

## Impact Scope

- `bottlenote-mono`: JpaCurationRepository Admin JPQL, Product feed criteria/repository, cursor payload/codec 또는 관련 cursor context, service
- `bottlenote-admin-api`: Admin curation HTTP/통합/문서 테스트
- `bottlenote-product-api`: Product feed HTTP/통합/문서 테스트
- DB schema, curation payload/spec JSON, batch, FE는 영향 없음

## Ordering Contract

논리 순서:

```txt
1. exposureStartDate IS NOT NULL
2. exposureStartDate DESC
3. id DESC
4. exposureStartDate IS NULL
5. id DESC
```

SQL/QueryDSL은 DB별 기본 null ordering에 의존하지 않고 명시적 null rank/case를 사용한다.

### Admin

- JPQL 또는 QueryDSL에서 명시적 null-last를 표현한다.
- Pageable의 외부 Sort가 고정 계약을 덮어쓰지 않도록 현재 controller/service 생성 방식을 확인한다.
- page 경계에서 동일 날짜 ID tie-breaker가 결정적이어야 한다.

### Product keyset cursor

- seek predicate와 orderBy는 반드시 같은 tuple 순서를 사용한다.
- cursor context 최소 정보: null bucket 여부, last exposureStartDate(nullable), last id.
- 날짜가 있는 bucket에서는 `(date < lastDate) OR (date = lastDate AND id < lastId)`.
- 마지막 날짜 row 다음에는 더 과거 날짜와 null bucket을 포함한다.
- null bucket에서는 `date IS NULL AND id < lastId`.
- cursor signature/HMAC와 외부 opaque string은 유지한다.
- 잘못된 cursor는 기존 semantic error contract로 거부한다.

## Test Strategy

Strict RED → GREEN → REFACTOR.

1. Admin RED: 서로 다른 date/displayOrder/createdAt, 동일 날짜, null, page 경계.
2. Product repository RED: date order, filter-before-pagination, size+1, 날짜→null 경계, null 내부 다음 cursor.
3. Cursor codec/context RED: round-trip, tamper/invalid, null date.
4. Product/Admin HTTP·문서 RED: 공개 JSON 유지, 고정 정렬 설명.
5. focused GREEN 후 `git diff --check` 및 accidental payload/spec/migration 변경 없음 확인.

Mockito 대신 실제 repository/fixture 상태와 결과 순서를 검증한다.

## Compatibility and Data

- API 응답 item shape는 변하지 않지만 목록 순서는 의도적으로 바뀐다.
- Product cursor는 opaque이므로 내부 payload 변경은 공개 JSON 구조 변경이 아니다.
- 배포 이전 cursor 재사용은 보장하지 않는다. PR 본문에 명시한다.
- 기존 displayOrder 값은 삭제·수정하지 않는다.
- exposureStartDate null 데이터는 그대로 보존한다.

## Non-goals

- 정렬 선택 파라미터
- displayOrder 컬럼 삭제
- curation payload의 eventStartDate 정렬
- DB migration/backfill
- visibility/만료 정책 변경
- FE 정렬 UI
- 다른 Product 목록 pagination 변경

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
  4. 현재 Product cursor가 nullable date tuple을 안전하게 표현할 수 없고 공개 계약 변경이 필요함
  5. exposureStartDate가 실제 소비자 의미의 날짜가 아니라는 근거 발견
- prohibited: merge, deploy/release, workspace issue mutation, Kubernetes/infra/secrets

## OOM Safety Policy

- 이 Hermes 머신에는 swap이 없으므로 로컬 `./gradlew`, Java compile/test, Testcontainers, Docker, IDE indexer를 실행하지 않는다.
- 병렬 에이전트는 파일 조사·편집·가벼운 JSON/텍스트 정적 검사만 수행한다.
- `git diff --check`, JSON parser, 소스 검색처럼 JVM을 띄우지 않는 검증만 로컬에서 허용한다.
- 실제 unit/integration/rule/compile 검증은 Draft PR GitHub Actions를 유일한 실행 근거로 사용한다.
- 세 구현 worker는 병렬 가능하지만 push/PR/CI watch는 한 번에 하나만 수행한다.
- 메모리 압박, 예상 밖 JVM/Docker 프로세스, CI 무한 대기가 보이면 즉시 정지하고 Progress Log에 기록한다.

## Tasks

### Task 1: Admin 날짜 고정 정렬 수직 경로
- Acceptance: Admin 결과가 date DESC/null-last/id DESC이며 page 경계가 안정적이다.
- Verification: Admin repository/HTTP 통합 테스트 RED 후 GREEN.
- Files (advisory): JpaCurationRepository 또는 query impl, Admin service/docs/tests
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: Product 날짜 keyset cursor 수직 경로
- Acceptance: feed가 같은 순서를 사용하고 날짜→null 및 null bucket cursor가 중복·누락 없이 이어진다.
- Verification: cursor/repository/service focused tests RED 후 GREEN.
- Files (advisory): CurationFeedSearchCriteria, cursor context/codec, CustomCurationFeedRepositoryImpl, tests
- Depends: 없음
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] Admin/Product ordering matrix 일치
- [x] nullable date seek 경계 통과

### Task 3: 문서·호환성·Draft PR CI verify
- Acceptance: 공개 item/cursor envelope는 유지되고 정렬 계약이 문서화되며 Draft PR CI가 통과한다.
- Verification: Product/Admin HTTP/docs tests, `git diff --check`, Draft PR 후 Actions watch.
- Files (advisory): API docs/tests, PR body
- Depends: Tasks 1, 2
- Size: M
- Status: [ ] delivery-stage pending (local worker static verification complete)

## Progress Log

- 2026-08-20: live main에서 기존 displayOrder keyset 확인.
- 2026-08-20: 사용자 승인. exposureStartDate DESC/null-last/id DESC, #410 단독 PR, PR-first GitHub Actions verify 확정.
- 2026-08-20: worker-stage 완료. Admin/Product test source를 먼저 추가해 기존 displayOrder ASC 구현과의 정적 RED 불일치를 확인했고, nullable date tuple seek·명시적 null rank·문서를 반영했다. OOM 정책에 따라 JVM/Gradle/컨테이너 검증과 delivery-stage push/PR은 수행하지 않았다.
