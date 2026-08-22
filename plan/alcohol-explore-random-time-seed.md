# ADR: Alcohol explore RANDOM 시간 기반 seed (#403)

## Status

- Accepted
- 기준: 수정된 workspace #403, `main` `8cda193f`

## Context

현재 `main`은 이미 `SearchSortType.RANDOM`, CRC32 rank, HMAC cursor extra seed를 제공하며 RANDOM이 기본 정렬이다. 첫 페이지 seed만 `ThreadLocalRandom.current().nextLong()`으로 생성한다.

제품 방침은 인기·저노출 결과의 혼합 추천이 아니라 RANDOM을 일반 정렬 조건으로 제공하는 것이다. 실제 물리 난수와 페이지 간 중복 제거는 보장하지 않으며, 첫 페이지 seed는 서버 시각의 초 단위 값으로 단순화한다.

## Decision

- `sortType=RANDOM`과 현재 기본값을 유지한다.
- 첫 페이지 seed를 `Instant.now().getEpochSecond()` 의미의 서버 초 단위 시각으로 생성한다.
- 다음 페이지는 기존 HMAC cursor extra의 seed를 재사용한다.
- CRC32 rank, filter-before-page, sort context, response envelope는 유지한다.
- 테스트 가능성을 위해 시간 공급 경계를 최소 범위로 둔다. 프로젝트 패턴에 맞는 `Clock` 주입 또는 package-private seed resolver 중 더 작은 방식을 선택하되 production 생성자/DI 회귀를 만들지 않는다.

## Compatibility invariants

- `SearchSortType` enum과 다른 정렬값은 변경하지 않는다.
- 요청 `cursor`/`size`와 응답 `content`/`pagination` 구조는 변경하지 않는다.
- 필터, rating range, HMAC 검증, cursor context/error contract는 변경하지 않는다.
- 같은 초의 첫 페이지 요청은 같은 seed를 사용할 수 있다.
- cursor 페이지 간 중복 가능성은 제품상 허용하지만, 기존 안정화 로직을 일부러 제거하지 않는다.

## Non-goals

- 인기·저노출 혼합, 비율, 가중치, 개인화
- RAND() 물리 난수 또는 완전 무작위 보장
- 페이지 간 중복 제거 보장 강화
- cursor 알고리즘 재설계
- DB migration, API 문서 구조 변경

## Test strategy

1. RANDOM 첫 페이지가 고정된 서버 초를 seed로 사용하는 단위 테스트를 먼저 추가한다.
2. cursor가 있으면 cursor extra seed를 계속 재사용하는 기존 단위 테스트를 유지한다.
3. 비-RANDOM은 seed 0을 유지하는지 확인한다.
4. compile → rule test → unit test만 검증한다. 개발 API 호출, integration test, Testcontainers, 배포 검증은 하지 않는다.
5. diff에서 `ThreadLocalRandom` 제거와 시간 seed 경계 외 query/contract 변경이 없는지 확인한다.

## Execution Mode

- mode: delegated
- scope: implement, test, verify, commit, push, pr
- stop-conditions: 공개 계약/다른 sort/cursor 의미 변화, 예상 밖 DI 범위 확대, compile/rule/unit 3회 내 미해결

## Progress Log

- 2026-08-22: #403/#418 기술 계약을 정리하고 live `main`의 기존 RANDOM 구현을 확인했다.
