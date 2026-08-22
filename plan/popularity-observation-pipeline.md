# Plan: 인기도 관측 파이프라인

## Overview

주류 인기도를 네 축(관심도·평가도·선호도·참여도)으로 나누어 시간별로 관측하고, 네 축이 모두 관측된 버킷에만 최종 인기도를 적재하는 배치 파이프라인을 신설한다.

핵심은 **사실과 정책의 분리**다. 관측 테이블에는 원시 카운트(사실)만 담고, 가중치·정규화·감쇠(정책)는 최종 적재 단계에서만 적용한다. 산식을 바꿔도 관측 데이터는 살아남고 최종 테이블만 다시 계산하면 된다. 이 구조가 성립해야 축별 시계열 그래프를 근거로 인기도 산식을 사후에 정할 수 있다.

파이프라인은 `배치 시작 → 버킷 시각 확정 → 4축 병렬 관측 → 합류 → 최종 적재` 순으로 흐른다.

설계 검토용 시뮬레이터: https://claude.ai/code/artifact/d2204e3e-9982-4f73-a0be-c7c617d01389

### Assumptions

대화에서 확정된 것을 가정으로 고정한다. 이 중 하나라도 깨지면 재개봉 프로토콜을 따른다.

1. **신규 추가만 한다.** 기존 `popularity.sql` / `PopularAlcoholSelectionJobConfig` / `popular_alcohols` / `/popular/*` API는 이번 범위에서 **건드리지 않는다.** 제거는 신규 파이프라인이 개발 환경에서 검증된 뒤 별도 작업으로 뺀다. 신구가 당분간 공존한다.
2. **테이블은 5개다.** 축별 관측 4개 + 최종 인기도 1개. 마이그레이션은 서브모듈 `git.environment-variables`의 V14로 추가하며, 서브모듈은 별도 저장소(`bottle-note/environment-variables`)이므로 별도 PR이 필요하다.
3. **버킷은 1시간이다.** 배치는 시간별로 돌고, 잡 전체가 하나의 버킷 시각(정시 절삭)을 공유한다. 관측 시각이 축마다 수십 초 어긋나는 것은 무시한다.
4. **관측 테이블은 희소하게 쓴다.** 직전 관측과 값이 같은 주류는 행을 남기지 않는다. 대신 각 행은 직전 관측 시각을 함께 들고 있어 갭을 계산할 수 있다.
5. **관측값은 누적과 구간 증가분을 모두 담는다.** 누적을 정본으로 두고 증가분은 직전 행과의 차이로 계산해 적재한다.
6. **최종 테이블은 조밀하게 쓴다.** 조회 기준이자 정렬 대상이므로 매 버킷 전 주류를 적재한다. 관측 테이블(희소)과 최종 테이블(조밀)의 밀도가 다른 것은 의도된 비대칭이다.
7. **결합은 합집합이다.** 어느 한 축이라도 관측된 주류가 대상이며, 이번 버킷에 값이 없는 축은 직전 관측을 끌어오고 그 값이 몇 시간 전 것인지를 함께 적재한다.
8. **한 축이라도 실패하면 최종 적재를 건너뛴다.** 성공한 축의 관측 행은 남긴다. 최종 테이블이 갱신되지 않으면 조회는 직전 버킷을 계속 본다.
9. **관심도는 흐름 축이다.** 조회 이벤트를 새로 남기지 않고, 기존 `alcohols_view_histories`의 `view_at`이 직전 구간에 속한 행을 센다. 배치를 놓친 구간은 영구 손실이며 이를 감수한다. 나머지 세 축은 원본 누적을 다시 세므로 실패 후 다음 회차에 자동으로 정합된다.
10. **가중치는 이번에 정하지 않는다.** 네 축을 균등(0.25)으로 합산하고, 가중치와 정규화 기준은 설정으로 분리해 실제 데이터를 본 뒤 조정한다. 정규화는 전역 최댓값을 쓰지 않고 설정된 고정 기준을 쓴다 — 전역 최댓값은 자기 값이 그대로인데도 점수가 흔들려 시계열을 무의미하게 만든다.
11. **롤업과 보존 정책은 이번 범위 밖이다.** 초기에는 삭제하지 않고 쌓는다. 볼륨이 문제가 되는 시점에 별도 작업으로 다룬다.
12. **조회 이력 동기화 잡의 소유권은 그대로 둔다.** 현재 product-api에서 1분마다 도는 `ViewHistorySyncJob`을 batch로 옮기지 않는다.

### Success Criteria

1. 마이그레이션 V14가 관측 4개 + 최종 1개, 총 5개 테이블을 생성하고 각 테이블에 `(alcohol_id, bucket_at)` 유니크 제약이 있다.
2. mono에 5개 엔티티와 각각의 도메인 레포지토리 + JPA 구현이 존재하고, `./gradlew compileJava`가 통과한다.
3. batch에 시간별 Quartz 트리거로 등록된 Job이 있고, 네 축 관측 Step이 병렬로 실행된 뒤 합류해 최종 적재 Step이 돈다.
4. 한 축 Step이 실패하면 최종 적재 Step이 실행되지 않는다 — 단위 테스트로 검증한다.
5. 직전 관측과 값이 같은 주류는 관측 행이 추가되지 않는다 — 단위 테스트로 검증한다.
6. 이번 버킷에 관측이 없는 축은 직전 값과 갭이 최종 행에 기록된다 — 단위 테스트로 검증한다.
7. `./gradlew compileJava compileTestJava unit_test check_rule_test`가 로컬에서 통과한다.
8. 개발 환경 DB에 스키마가 반영되고, 실제 수집 사이클이 **최소 2회 이상** 돌아 관측 테이블과 최종 테이블에 행이 쌓인 것을 확인한다.
9. PR이 리뷰 가능한 단위로 계층 분할되어 있고, 각 PR이 독립적으로 컴파일된다.

### Impact Scope

| 대상 | 내용 |
|---|---|
| `git.environment-variables` (서브모듈) | V14 마이그레이션 — **별도 저장소 PR** |
| `bottlenote-mono` | 엔티티 5, 도메인 레포지토리 5, JPA 구현 5 (alcohols 도메인) |
| `bottlenote-batch` | 관측 Step 4, 최종 적재 Step 1, Job/Quartz 등록, 설정 프로퍼티 |
| `bottlenote-test-support` | InMemory 레포지토리 / TestFactory |
| 기존 인기도 코드 | **건드리지 않음** (가정 1) |
| 운영 배포 | **범위 밖** — 개발 환경까지만 |

cross-domain 결합은 없다. 관측은 배치가 각 원본 테이블을 직접 집계하며, 신규 Facade는 만들지 않는다.

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr, deploy-dev
- stop-conditions: 기본 3종 + 아래 2건
  1. 가정 붕괴 — 재개봉 프로토콜
  2. `/verify` 3회 시도 내 미해결
  3. 선언된 scope 밖의 되돌리기 어려운 행동
  4. **운영(production) 배포가 필요해지는 경우** — 개발 환경까지만 위임받았다
  5. **서브모듈 PR이 머지되지 않아 본 저장소 빌드가 막히는 경우** — 서브모듈 포인터 갱신은 상대 저장소 머지 이후에만 가능하다

로컬 검증은 컴파일·unit·rule까지만 수행한다. 통합 테스트는 Docker 자원 문제로 로컬에서 돌리지 않고 GitHub Actions에 맡긴다.

## Tasks

PR 계층은 리뷰 단위로 나눈다. 한 PR 안에 Task 여러 개가 들어가며, 각 PR은 독립적으로 컴파일된다.

| PR | 범위 | Task |
|---|---|---|
| A | 서브모듈 — 스키마 | 1 |
| B | mono — 영속 계층 | 2, 3, 4 |
| C | batch — 축별 관측 | 5, 6, 7 |
| D | batch — 결합과 조립 | 8, 9 |
| — | 개발 배포·검증 | 10, 11 |

### Task 1: V14 마이그레이션으로 관측 4개·최종 1개 테이블을 만든다
- Acceptance: 5개 테이블이 각각 `(alcohol_id, bucket_at)` 유니크 제약과 조회용 인덱스를 갖는다. 관측 테이블은 누적값·구간 증가분·직전 관측 시각 컬럼을 갖고, 최종 테이블은 축별 원시값·축별 갭·점수 컬럼을 갖는다.
- Verification: 서브모듈에서 `V14__*.sql` 파일이 기존 V13까지의 DDL 스타일(주석·명명·제약 명명)과 일치하는지 육안 확인. 본 저장소 빌드에는 아직 영향 없음.
- Files (advisory): `git.environment-variables/storage/db/migration/V14__add_popularity_observations.sql`
- Depends: 없음
- Size: S
- Status: [x] done — PR https://github.com/bottle-note/environment-variables/pull/15

### Task 2: 관심도·평가도 관측 엔티티와 레포지토리를 만든다
- Acceptance: 두 엔티티가 마이그레이션 스키마와 필드 단위로 대응하고, 도메인 레포지토리 인터페이스에 Spring Data 타입이 노출되지 않으며, JPA 구현이 `@JpaRepositoryImpl`을 단다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Files (advisory): `bottlenote-mono/.../alcohols/domain/`, `bottlenote-mono/.../alcohols/repository/`
- Depends: 1
- Size: M
- Status: [ ] not done

### Task 3: 선호도·참여도 관측 엔티티와 레포지토리를 만든다
- Acceptance: Task 2와 동일한 규약을 따르고, 참여도 엔티티가 리뷰·좋아요·싫어요·댓글 카운트를 각각 별도 컬럼으로 갖는다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Depends: 1
- Size: M
- Status: [ ] not done

### Task 4: 최종 인기도 스냅샷 엔티티와 레포지토리를 만든다
- Acceptance: 축별 원시값과 축별 갭(직전 관측으로부터 경과 시간), 점수를 담는다. 최신 버킷을 전역으로 하나 고정해 조회하는 메서드가 있다 — 주류별 최신을 각자 고르지 않는다.
- Verification: `./gradlew :bottlenote-mono:compileJava`
- Depends: 1
- Size: S
- Status: [ ] not done

### Checkpoint: after Tasks 1-4
- [ ] `./gradlew compileJava` 통과
- [ ] `./gradlew check_rule_test` 통과 (ArchUnit)
- [ ] PR A(서브모듈) 생성, PR B(mono) 생성

### Task 5: 버킷 시각 확정과 관측 설정을 만든다
- Acceptance: 잡 파라미터의 실행 시각을 정시로 절삭한 버킷 시각을 네 축이 공유한다. 행 단위로 `now()`를 다시 부르지 않는다. 가중치·정규화 기준을 설정 프로퍼티로 노출하고 기본값은 균등 0.25다.
- Verification: `./gradlew :bottlenote-batch:compileJava` + 버킷 절삭 단위 테스트
- Depends: 없음 (2·3·4와 병렬 가능)
- Size: S
- Status: [ ] not done

### Task 6: 관심도·평가도 관측 Step을 만든다
- Acceptance: 축별로 전체 대상을 집합 연산 한 번으로 집계한다(주류별 루프 금지). 관심도는 `view_at`이 직전 구간에 속한 행을 세고, 평가도는 `rating > 0.0`만 집계한다. 직전 관측과 값이 같으면 행을 쓰지 않는다.
- Verification: 희소 저장 단위 테스트 — 같은 값 두 번 관측 시 행이 하나만 늘어난다
- Depends: 2, 5
- Size: M
- Status: [ ] not done

### Task 7: 선호도·참여도 관측 Step을 만든다
- Acceptance: 선호도는 `status = 'PICK'`만, 참여도는 리뷰 `ACTIVE`+`PUBLIC`·좋아요 `LIKE`/`DISLIKE` 분리·댓글 `NORMAL`만 집계한다. Task 6과 동일한 희소 저장 규칙을 따른다.
- Verification: 상태 필터 단위 테스트 — UNPICK·DISLIKE·PRIVATE가 집계에서 빠진다
- Depends: 3, 5
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 5-7
- [ ] `./gradlew compileJava compileTestJava unit_test` 통과
- [ ] PR C(batch 관측) 생성

### Task 8: 최종 적재 Step에서 네 축을 결합한다
- Acceptance: 어느 한 축이라도 관측된 주류의 합집합을 대상으로 하고, 이번 버킷에 값이 없는 축은 직전 관측값과 갭을 함께 적재한다. 정규화는 설정된 고정 기준을 쓰고 전역 최댓값을 쓰지 않는다.
- Verification: 결합 단위 테스트 — 한 축만 관측된 주류도 최종 행이 생기고 나머지 축에 갭이 기록된다
- Depends: 4, 6, 7
- Size: M
- Status: [ ] not done

### Task 9: 병렬 Job을 조립하고 시간별 트리거로 등록한다
- Acceptance: 네 관측 Step이 병렬로 실행된 뒤 합류해 최종 적재 Step이 돈다. 한 축 Step이 실패하면 최종 적재 Step이 실행되지 않는다. Quartz 트리거가 시간별이며 클러스터 환경에서 단일 실행된다.
- Verification: Job 흐름 단위 테스트 — 축 Step 실패 시 최종 Step 미실행
- Depends: 8
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 8-9
- [ ] `./gradlew compileJava compileTestJava unit_test check_rule_test` 통과
- [ ] PR D(조립) 생성

### Task 10: 개발 환경에 반영하고 수집 사이클을 검증한다
- Acceptance: 서브모듈 PR 머지 후 포인터를 갱신하고 개발 배포한다. 실제 수집 사이클이 **2회 이상** 돌아 관측 테이블과 최종 테이블에 행이 쌓인 것을 확인한다. 버킷 시각이 네 축에서 동일한지, 희소 저장으로 행이 실제로 절감되는지 데이터로 확인한다.
- Verification: 개발 DB 조회 — 버킷별 행 수, 축별 버킷 시각 일치, 최종 테이블 갭 분포
- Depends: 9 (그리고 서브모듈 PR 머지)
- Size: S
- Status: [ ] not done

### Task 11: 다중 에이전트 교차 검증
- Acceptance: 구현 전체에 대해 독립 에이전트들이 스키마-엔티티 정합, 결합 규칙, 실패 처리, 희소 저장, 클러스터 동시성, 쿼리 성능, 경계 조건을 각각 검증하고 발견 사항을 보고한다. 확인된 결함은 수정하고 재검증한다.
- Verification: 각 검증 축의 보고서와 수정 커밋
- Depends: 10
- Size: M
- Status: [ ] not done

## Progress Log

- 2026-08-23 define 승인 — delegated, scope에 pr·deploy-dev 포함
- 2026-08-23 Task 1 완료. 서브모듈에 V14 마이그레이션 추가, 테이블 5개(관측 4 + 최종 1) 생성. `alcohol_id`는 `alcohols.id`와 같은 signed BIGINT로 맞췄고 대량 삽입 경로라 FK는 걸지 않았다. 기존 V11~V13의 DDL 스타일에 맞췄다. 서브모듈 PR #15 오픈 — 머지 전까지 본 저장소 포인터 갱신은 불가(stop-condition 5).
