# ADR: Alcohol explore 임시 동적 인기순 (#403 / PR #719)

## Status

- Re-opened / Accepted
- 기준: PR #719 HEAD `7531d913`, 사용자 기술 결정(2026-08-22)
- 기존 `plan/alcohol-explore-random-time-seed.md` 결정은 유지한다.

## Assumption collapse

PR #719는 RANDOM 첫 페이지 seed만 바꾸고 `POPULAR`은 유지하는 범위였다. 실제 Product FE는 기본 요청에 `sortType=POPULAR&sortOrder=DESC`를 명시하므로 백엔드 RANDOM 기본값과 시간 seed가 기본 화면에 적용되지 않는다. 현재 explore POPULAR는 `AVG(rating) + COUNT(DISTINCT review.id)`인 누적값이라 리뷰 수가 순위를 지배하고 상위 노출이 수 주간 고정된다.

처음에는 최근 24시간·7일 `alcohols_view_histories`를 요청마다 직접 집계하는 임시 산식을 검토했다. 그러나 실제 스키마는 `(user_id, alcohol_id)` PK 외에 `alcohol_id`/`view_at` 선두 인덱스가 없고, rating × review × history 1:N JOIN이 곱집합을 만든다. 기본 화면마다 이 경로를 실행하는 것은 긴급 완화로도 허용하지 않는다. 이 구현 시도는 push하지 않고 폐기한다.

## Decision

캐시 테이블 도입 전 임시 POPULAR 점수는 이미 일일 배치가 계산해 저장하는 `popular_alcohols.popular_score`의 **주류별 최신 스냅샷**을 사용한다.

- candidate query에서 `popular_alcohols`를 주류별 `MAX(created_at)` 행으로 LEFT JOIN한다.
- 점수가 없으면 `0.0`으로 처리한다.
- 기존 `sortOrder`를 적용하고 동점은 기존처럼 `alcohol.id ASC`다.
- `RATING`, `PICK`, `REVIEW`, `RANDOM` 정렬은 변경하지 않는다.
- POPULAR candidate query에서 점수 계산용 rating/review JOIN을 제거한다. rating range가 있으면 기존처럼 rating JOIN과 HAVING을 유지한다.
- RANDOM epoch-second seed 변경도 유지한다.

현재 `popular_alcohols` 배치 점수는 review score 40%, rating score 30%, pick score 30%이며 review score에 30일 시간 감쇠가 있다. 최근 조회 활동 D/W는 아직 포함하지 않지만, explore의 누적 review-count 고정식보다 현시점 배치 snapshot을 직접 소비하므로 즉시 교체 가능한 최소 위험 완화다.

## Rejected temporary approach

아래 요청 시점 직접 집계는 장기 cache formula 후보로만 남기고 PR #719에는 구현하지 않는다.

```text
D = 최근 24시간 고유 조회자
W = 최근 7일 고유 조회자
R = 평균 별점
C = 리뷰 수
score = 0.60*LN(1+D) + 0.25*LN(1+W/7) + 0.10*(R/5) + 0.05*LN(1+C)
```

거절 사유:
- history에 `(alcohol_id, view_at)` 인덱스가 없다.
- API 요청마다 rating/review/history 곱집합 집계 비용이 발생한다.
- 요청마다 `asOf`가 달라져 cursor에 기준 시각을 추가하지 않으면 페이지 중복·누락 위험이 있다.
- 신규 index/cursor-extra까지 추가하면 임시 변경 범위를 벗어난다.

## Long-term replacement

별도 작업에서 `alcohol_popularity_score_cache`를 도입한다. 배치가 현시점 D/W/R/C와 formula version을 계산해 주류당 최신 1행을 원자적으로 upsert하고, explore는 최신 score만 JOIN한다. `popular_alcohols`는 일별 이력으로 유지한다. 그룹별 산식이 실제로 달라지기 전에는 category/region별 행을 복제하지 않는다.

## Compatibility invariants

- endpoint, request parameter, enum, default, response JSON을 변경하지 않는다.
- cursor의 context, sort key 이름(`sort`, `id`), HMAC/error contract를 유지한다.
- filter-before-page, rating range, size+1, hasNext/nextCursor를 유지한다.
- POPULAR 외 정렬의 SQL JOIN과 점수 의미를 변경하지 않는다.

## Test strategy

1. POPULAR sortScore가 latest `popular_alcohols.popular_score` expression을 사용하고 null을 0으로 처리하는 실제 QueryDSL expression unit test를 먼저 추가한다.
2. 이미 계산한 score 객체가 select/HAVING/order에 동일하게 사용되는지 unit/static review로 확인한다.
3. source file 문자열을 읽는 테스트는 사용하지 않는다.
4. 다른 sortType의 기존 표현식과 RANDOM seed 테스트를 유지한다.
5. 로컬 JVM/Gradle은 메모리 제약으로 실행하지 않고 CI에서 compile → rule → unit을 확인한다.
6. `git diff --check`, POPULAR 외 분기 diff, API DTO/serialization 무변경을 독립 리뷰한다.

## Execution Mode

- mode: delegated
- scope: implement, test, verify, commit, push, pr-update
- stop-conditions: migration/infra 필요, 공개 API·cursor key 변경, POPULAR 외 동작 변경, latest snapshot JOIN SQL 불확실, compile/rule/unit 3회 내 미해결

## Progress Log

- 2026-08-22: FE가 POPULAR를 명시 전송하여 RANDOM 기본값이 기본 화면에 적용되지 않음을 확인했다.
- 2026-08-22: 요청 시점 D/W 직접 집계 구현을 검토했으나 cursor·곱집합·인덱스 blocker로 push 전 폐기했다.
- 2026-08-22: 임시 완화는 최신 `popular_alcohols.popular_score` 재사용으로 축소했다.
