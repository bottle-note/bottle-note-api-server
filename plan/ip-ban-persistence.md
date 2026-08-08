# Plan: IP 밴 DB 영속화와 Redis 동기화

## Overview

관리자가 등록한 IP 밴의 원본과 변경 이력, 공격 판단 근거를 MySQL에 영속화한다. Redis는 요청 경로에서 빠르게 차단하는 실행 계층으로 유지하며, 관리 명령 직후 반영과 주기적 재조정으로 DB 상태에 수렴시킨다. 이를 통해 Redis 데이터 유실 복구, 관리자·Agent 행위 감사, 공격/false positive 분석이 가능해야 한다.

### Assumptions

1. MySQL이 IP 밴 상태의 SSOT이고 Redis는 재생성 가능한 enforcement projection이다.
2. `ip_bans`는 정규화 IP당 현재 상태 1행을 유지하고, 재밴·연장·해제는 해당 행을 갱신한다.
3. `ip_ban_events`는 BAN, EXTEND, UNBAN, EXPIRE 행위를 append-only로 보존한다.
4. `ip_security_signals`는 밴과 독립적으로 존재할 수 있는 탐지 근거이며 endpoint, HTTP method, rule code, 관찰 구간·횟수, Agent 식별자·버전, 판정 상태를 저장한다.
5. Agent 여부는 인증된 Admin ID를 기존 `agents.admin_user_id`와 매핑하여 판별하고, 일반 관리자는 ADMIN으로 기록한다.
6. 자동 탐지와 자동 밴은 이번 범위에서 제외한다. Agent나 관리자가 밴 API에 선택적 signal 메타데이터를 전달할 수 있다.
7. 현재 정책을 유지해 밴은 1초~30일이고 영구 밴은 허용하지 않는다.
8. 관리 명령은 DB 상태·감사 이벤트를 먼저 커밋하고 Redis에 즉시 반영한다. Redis 반영 실패 시 DB 상태는 유지하고 `PENDING_RECONCILE`로 노출한다.
9. 주기 동기화는 다중 인스턴스 중복 실행을 막는 JDBC-backed Quartz 작업으로 1분마다 수행하며 DB의 활성·해제·만료 상태를 Redis에 수렴시킨다.
10. 기존 Admin IP 밴 API의 IP·사유·TTL·banned 필드는 호환하고, 목록의 원본은 Redis SCAN에서 DB 조회로 변경한다.
11. 공격 근거에는 요청 본문, Authorization, Cookie, API key 등 비밀·민감 헤더를 저장하지 않는다.
12. 서브모듈은 최신 `origin/main`에서 `Whale0928/blacklist` 브랜치를 만들고 다음 Flyway 버전인 V9를 추가한다.
13. 기존 미커밋 access-control 리뷰 수정은 보존하며 별도 선행 커밋으로 분리한 뒤, DB 영속화 커밋을 이어서 만든다.
14. 종료된 밴·이벤트·signal의 원본 IP와 메타데이터는 180일 보존 후 삭제한다.
15. Redis 즉시 반영 실패 시 관리 API는 `202 Accepted`와 `PENDING_RECONCILE`을 반환한다.
16. 초기 마이그레이션은 저장소·운영 로그에서 실제 침해 근거가 확인된 IP/CIDR만 seed한다. 외부 가변 위협 목록이나 검색엔진 대역은 Flyway에 고정하지 않으며 검증 가능한 대상이 없으면 초기 밴은 0건이다.

### Success Criteria

1. Admin이 IP를 밴하면 `ip_bans` 현재 상태와 `ip_ban_events` BAN 이벤트가 하나의 DB 트랜잭션으로 기록된다.
2. 밴 요청에 signal이 있으면 `ip_security_signals`에 공격 지점, 규칙, 관찰 수치, Agent 메타데이터가 저장되고 밴과 연결된다.
3. Admin 또는 Agent가 수행한 밴·연장·해제의 주체, 사유와 시점이 구분되어 조회된다.
4. signal을 `UNKNOWN`, `CONFIRMED_ATTACK`, `FALSE_POSITIVE`로 판정하고 검토 주체·시점·메모를 기록할 수 있다.
5. DB 커밋 직후 Redis가 갱신되며 Product/Admin 요청 필터는 계속 Redis만 조회한다.
6. Redis가 비었거나 일시 장애 후 복구되면 1분 주기 작업이 활성 밴을 복원하고 해제·만료된 밴을 제거한다.
7. Redis 반영 실패가 성공으로 위장되지 않고 관리 API와 저장 상태에서 `PENDING_RECONCILE`로 확인된다.
8. 동일 IP의 중복·역순 동기화가 최신 DB 상태를 덮어쓰지 않는다.
9. 기존 403 밴, 429 rate limit, fail-open 200ms 계약과 기존 Admin API 필드가 유지된다.
10. V9 마이그레이션이 Product/Admin 리소스에 포함되고 Testcontainers Flyway migrate/validate를 통과하며 Batch jar에는 포함되지 않는다.
11. 단위·DB/Redis 통합·Product/Admin Security chain·ArchUnit 검증이 모두 통과한다.
12. 서브모듈 브랜치와 백엔드 브랜치가 각각 의도적인 커밋으로 원격에 push되고 부모 gitlink가 원격에 존재하는 서브모듈 커밋을 가리킨다.

### Impact Scope

- `git.environment-variables`: V9 Flyway 마이그레이션, `Whale0928/blacklist` 브랜치
- `bottlenote-mono`: accesscontrol 도메인 상태·감사·signal 모델, 포트/JPA 구현, Redis projection/reconcile
- `bottlenote-admin-api`: 밴·해제·목록 DB 전환, 이벤트/signal 조회와 판정 API, 통합 테스트
- `bottlenote-product-api`: JDBC Quartz reconcile 실행 구성, Security chain 회귀 테스트
- `bottlenote-test-support`: Fake 포트와 DB/Redis 테스트 지원
- 부모 저장소: 서브모듈 gitlink 갱신

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push
- publication: 서브모듈과 현재 백엔드 브랜치 push, 새 PR 생성 제외
- stop-conditions: 가정 붕괴, verify 3회 실패, 승인 scope 밖 행동

## Tasks

### Task 1: 기존 access-control 리뷰 수정 확정

- Acceptance: Redis 장애 격리, HTTP method별 review rate-limit, 테스트 더블 분리와 Product/Admin Security chain 회귀 검증을 하나의 선행 변경으로 확정한다.
- Acceptance: 기존 Admin IP 밴 API와 403·429·200ms fail-open 계약을 유지한다.
- Verification: access-control 단위·Redis 통합·Product/Admin 통합·ArchUnit 테스트
- Files (advisory): 현재 작업 트리의 access-control 리뷰 수정 경로와 `plan/access-control-review-fixes.md`
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: V9 IP 보안 스키마 게시

- Acceptance: `Whale0928/blacklist` 브랜치에 current state, append-only event, signal/verdict 스키마와 제약·인덱스를 추가한다.
- Acceptance: 검증 가능한 침해 대상이 없으므로 초기 차단 seed는 0건이고 민감 요청 데이터 컬럼을 만들지 않는다.
- Verification: Flyway SQL 검토, Product/Admin Testcontainers migrate·validate, Batch migration resource 제외
- Files (advisory): `git.environment-variables/storage/db/migration/V9__*.sql`
- Depends: Task 1
- Size: S
- Status: [x] done

### Task 3: DB current-state와 append-only 이벤트 수직 슬라이스

- Acceptance: 정규화 IP별 현재 상태 1행과 BAN·EXTEND·UNBAN·EXPIRE 이벤트를 같은 DB 트랜잭션으로 저장한다.
- Acceptance: 밴·연장·해제·만료 상태와 주체를 DB에서 조회하며 Redis를 목록 원본으로 사용하지 않는다.
- Verification: mono 단위·DB 통합 테스트
- Files (advisory): `app.bottlenote.accesscontrol` domain/repository/service/facade와 test-support Fake
- Depends: Task 2
- Size: M
- Status: [x] done

### Task 4: 보안 signal과 verdict 수직 슬라이스

- Acceptance: 밴과 독립적인 signal에 endpoint·method·rule·관찰 구간·횟수·Agent 메타데이터를 저장한다.
- Acceptance: UNKNOWN·CONFIRMED_ATTACK·FALSE_POSITIVE 판정과 검토 주체·시점·메모를 기록한다.
- Verification: mono 단위·DB 통합 테스트
- Files (advisory): accesscontrol signal domain/repository/service/facade와 Fake
- Depends: Task 3
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-4

- [ ] 기존 access-control 회귀 테스트와 V9 migrate/validate 통과
- [ ] current-state·event·signal·verdict DB 테스트 통과

### Task 5: Redis 즉시 enforcement projection

- Acceptance: DB 커밋 후 Redis를 즉시 갱신하고 Product/Admin filter는 Redis만 조회한다.
- Acceptance: Redis 반영 실패 시 DB 상태를 유지하고 `PENDING_RECONCILE` 결과를 반환하며 역순 갱신이 최신 상태를 덮지 않는다.
- Verification: mono 단위·Redis Testcontainers 통합 테스트
- Files (advisory): accesscontrol projection service/result와 Redis access-control store
- Depends: Task 3
- Size: M
- Status: [x] done

### Task 6: JDBC Quartz reconcile과 180일 retention

- Acceptance: 1분 주기 단일 실행이 DB 활성 상태를 Redis에 복원하고 해제·만료 상태를 제거한다.
- Acceptance: 종료된 밴·이벤트·signal은 180일 후 삭제하며 작업은 중복 실행에도 안전하다.
- Verification: Quartz/JDBC job 단위·통합 테스트와 Redis 복구 시나리오
- Files (advisory): accesscontrol reconcile/retention job, scheduler binding, Product 설정, tests
- Depends: Tasks 4-5
- Size: M
- Status: [x] done

### Task 7: Admin DB 명령·조회 API

- Acceptance: 기존 IP·사유·TTL·banned 응답 호환을 유지하며 밴·해제·목록을 DB 기반으로 처리한다.
- Acceptance: Redis 실패 시 `202 Accepted`와 `PENDING_RECONCILE`을 반환하고 event/signal/verdict 조회·판정 API를 제공한다.
- Verification: Admin controller/API 통합 테스트와 OpenAPI 계약 확인
- Files (advisory): Admin controller, request/response DTO, accesscontrol facade, Admin integration tests
- Depends: Tasks 4-6
- Size: M
- Status: [x] done

### Task 8: DB·Redis·Security chain 통합 검증

- Acceptance: DB 트랜잭션, Redis 유실·즉시 반영 실패·reconcile 복구와 기존 403·429·fail-open 계약을 실제 컨텍스트에서 검증한다.
- Acceptance: Product/Admin Flyway와 Batch resource 경계를 검증한다.
- Verification: mono/Product/Admin 통합 테스트와 Batch jar resource 검사
- Files (advisory): mono/Product/Admin access-control integration tests와 test profiles
- Depends: Task 7
- Size: M
- Status: [x] done

### Task 9: 전체 검증과 부모 gitlink 게시

- Acceptance: compile, Spotless, ArchUnit, unit, Product/Admin integration과 build가 모두 통과한다.
- Acceptance: 부모 커밋이 원격 V9 서브모듈 커밋을 가리키고 서브모듈·백엔드 브랜치를 각각 push한다.
- Verification: `/verify full`, `git ls-tree`, `git ls-remote`, 원격 branch 대조
- Files (advisory): 부모 `git.environment-variables` gitlink와 plan Progress Log
- Depends: Task 8
- Size: S
- Status: [ ] not done

## Progress Log

- 2026-08-09: Terra와 Grok read-only 탐색 완료. 서브모듈 V1~V8/Flyway 관례, DB·Redis·Quartz·감사 패턴 및 기존 미커밋 상태 확인.
- 2026-08-09: 사용자 승인. 180일 보존, Redis 실패 시 202/PENDING, commit·push 위임 및 검증된 초기 차단 데이터만 허용하는 계약 확정.
- 2026-08-09: Grok 4.5와 공식 출처 조사 결과 저장소에 사고 확정 IP/CIDR이 없음을 확인하여 V9 초기 차단 seed를 0건으로 결정. 검색엔진은 공식 검증 절차를 사용하고 외부 위협 목록은 운영 동기화 대상으로 분리.
- 2026-08-09: Terra Task 초안을 검토해 기존 리뷰 수정, V9, DB 상태·감사·signal, Redis projection, reconcile/retention, Admin API, 통합 검증 순서의 9개 Task로 확정.
- 2026-08-09: Task 1 완료 — 커밋 `22794b04`. access-control 단위 28/28, Redis 통합 4/4, Product 3/3, Admin 5/5, ArchUnit 64/64 통과.
- 2026-08-09: Task 2 완료 — 서브모듈 커밋 `7aea7d8b`를 `origin/Whale0928/blacklist`에 push. V9 3테이블, seed 0건, Product/Admin Flyway와 Batch resource guard 통과.
- 2026-08-09: Task 3 완료 — `app.bottlenote.accesscontrol`에 IpBan current-state·IpBanEvent append-only·IpBanService/Facade 수직 슬라이스. AgentFacade adminUserId 조회 추가. 단위 14/14, Product DB 통합 5/5 통과. 필터/Admin API/signal/projection/scheduler 미변경.
- 2026-08-09: Task 4 완료 — `IpSecuritySignal`과 verdict 수직 슬라이스를 추가. signal은 query string 없는 endpoint, method/rule, 관찰 구간·횟수, Admin/활성 Agent UUID·입력 agent version을 저장하며 UNKNOWN에서 확정 판정으로 한 번만 전이한다. 단위 6/6, Product DB 통합 2/2, Spotless 통과.
- 2026-08-09: Task 5 완료 — facade는 DB 트랜잭션 종료 뒤 최신 append-only event ID로 Redis projection을 수행하고 실패 시 DB 상태를 유지한 채 `PENDING_RECONCILE`을 반환한다. Redis/InMemory projection은 역순 이벤트를 무시하며 unban version은 180일 보존한다.
- 2026-08-09: Task 6 완료 — Product JDBC Quartz cluster에서 1분 단일 재조정과 일일 180일 보존 작업을 분리했다. 활성 상태는 bounded cursor로 Redis에 복원하고, 만료 ACTIVE는 SYSTEM EXPIRE 이벤트 뒤 versioned unban한다. signal→event→종료 ban 순서로 FK를 정리하며 access-control 비활성 테스트 프로필에는 Job을 등록하지 않는다.
- 2026-08-09: Task 7 완료 — Admin IP ban API를 DB facade 기반으로 전환하고 기존 ip/reason/ttlSeconds/banned 응답에 id/projectionStatus를 추가했다. 활성 목록은 DB SSOT이며 history, signal 등록·조회, UNKNOWN 확정 판정 API를 OpenAPI와 함께 제공한다. 인증 admin ID를 ban/event/signal reporter·reviewer에 명시적으로 전달하고, projection 실패 API는 HTTP 202/PENDING_RECONCILE과 DB ban/event 보존을 통합 테스트로 검증했다.
- 2026-08-09: Task 8 완료 — 실제 Product MySQL·Redis 컨텍스트에서 Redis 유실 뒤 ACTIVE DB ban 복원, DB UNBAN stale projection 제거, DB 만료 ACTIVE의 SYSTEM EXPIRE 이력과 Redis 제거를 3개 통합 테스트로 검증했다. reconciliation/job 조건을 access-control enabled property로 정렬하고 `NOT_SUPPORTED` 경계로 만료 DB commit 뒤 projection을 유지했다. Product 대상 12/12, Admin 대상 7/7, mono unit 3개 클래스와 Redis 통합 8/8, Batch context/resource guard가 통과했다. `check_rule_test`는 accesscontrol 기존 DTO/Event/enum 네이밍 위반 4개 rule test(10 reported violations, source 8 classes)가 남아 Task 9 전 후속 커밋으로 정리한다.
