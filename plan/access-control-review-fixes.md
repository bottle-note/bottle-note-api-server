# Plan: Access Control 리뷰 결함 수정

## Overview

PR #698 리뷰에서 확인된 Critical 1건과 Important 4건을 모두 해소한다. Redis 장애가 API 가용성 장애로 전파되지 않게 하고, review 읽기/쓰기 rate limit 계약을 실제 요청 방식과 일치시키며, ban 목록 조회의 Redis 호출 증폭을 제거한다. 또한 실제 Security filter chain과 Redis 동작을 통합 테스트로 검증하고 테스트 전용 InMemory 구현을 운영 코드에서 분리한다.

### Assumptions

1. 수정 범위는 리뷰에서 확정된 Critical 1건과 Important 4건으로 제한하며, auto-ban·사용자별 rate limit·edge rate limit은 추가하지 않는다.
2. Redis 장애 시 access-control은 짧은 제한 시간 안에 fail-open하고 요청 처리를 계속해야 한다.
3. `/api/v1/reviews/**`의 GET 요청은 읽기 한도, POST·PATCH·DELETE 요청은 쓰기 한도를 사용하며 각 한도는 IP별로 별도 버킷을 사용한다.
4. ban 목록 API는 최대 500건 계약을 유지하되 Redis 왕복을 항목 수에 비례해 순차 수행하지 않는다.
5. product/admin 테스트 기본 프로필은 그대로 격리하되, access-control을 명시적으로 활성화한 통합 테스트에서 실제 Redis와 Security chain을 검증한다.
6. DB 스키마와 Flyway 마이그레이션 변경은 없다.

### Success Criteria

- Redis가 응답하지 않을 때 설정된 짧은 제한 시간 안에 fail-open하며 product/admin 요청 스레드가 15초 동안 점유되지 않는다.
- review GET과 쓰기 요청이 서로 다른 한도 및 Redis 버킷을 사용하고 테스트로 증명된다.
- ban 500건 목록 조회가 항목별 순차 `EXISTS`·`TTL`·`GET` 호출 없이 제한된 Redis 왕복으로 완료된다.
- product/admin 실제 Security filter chain에서 허용, ban 403, rate limit 429, 관리 API 인증 및 unban 탈출 경로가 검증된다.
- `InMemoryAccessControlStore`가 운영 main source에서 제거되고 `bottlenote-test-support` 또는 모듈 테스트 소스가 소유한다.
- 기존 rule/unit/integration 검증과 product/admin compile이 모두 통과한다.

### Impact Scope

- `bottlenote-mono`: access-control 설정, 정책 선택, Redis store와 단위 테스트
- `bottlenote-product-api`: review rate-limit 설정과 Security chain 통합 테스트
- `bottlenote-admin-api`: 관리 API와 Security chain 통합 테스트
- `bottlenote-test-support`: InMemory access-control store
- 스키마·Flyway·batch: 영향 없음

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify
- excluded: commit, push, pr
- stop-conditions: 기본 3종

## Tasks

### Task 1: Redis 장애 격리와 ban inventory 조회 최적화
- Acceptance: access-control Redis 명령이 전역 15초 timeout과 분리되어 최대 200ms 안에 실패하고, ban 500건 목록 조회가 항목별 순차 Redis 왕복 없이 완료된다.
- Verification: mono compile, Redis store 단위·통합 테스트, 호출 횟수 또는 pipeline 결과 검증
- Files (advisory): AccessControlConfiguration, RedisAccessControlStore, access-control properties/config, Redis store tests
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: HTTP method별 review rate-limit 계약 적용
- Acceptance: `/api/v1/reviews/**` GET은 600/min, POST·PATCH·DELETE는 60/min을 사용하고 서로 다른 Redis 버킷을 소비한다. 기존 longest-prefix 규칙과 unknown IP 규칙은 유지한다.
- Verification: mono compile, AccessControlService/Filter 단위 테스트
- Files (advisory): AccessControlProperties, AccessControlService, AccessControlFilter, product application.yml, access-control unit tests
- Depends: 없음
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] mono/product compile 통과
- [x] access-control 단위 테스트 통과

### Task 3: 테스트 더블 분리와 실제 Security chain 통합 검증
- Acceptance: InMemoryAccessControlStore는 운영 main source에 남지 않으며, 실제 Redis를 사용하는 product/admin 통합 테스트가 허용·403·429·관리 API 인증·unban 탈출 경로를 검증한다.
- Verification: product integration_test, admin_integration_test, check_rule_test
- Files (advisory): bottlenote-test-support fake, mono unit tests, product/admin security integration tests와 테스트 설정
- Depends: Task 1, Task 2
- Size: M
- Status: [x] done

## Progress Log

- 2026-08-08: 사용자가 fail-open 200ms, review GET 600/min, 쓰기 60/min과 delegated(plan, implement, test, verify; commit/push/pr 제외) 계약을 승인했다.
- 2026-08-08: Task 1 완료 — access-control 전용 Lettuce factory `redis-command-timeout: 200ms`, `listBans` SCAN+pipeline, mono unit 22 / Redis IT 4 통과. commit/push 없음.
- 2026-08-08 Task 2: PathRateLimitRule에 methods 추가, Filter→Service method 전달, review GET 600/write 60 별도 Redis 버킷. mono/product compile 통과, AccessControlService/Filter 단위 테스트 20/20 통과. commit/push 없음.
- 2026-08-08 Task 3: InMemoryAccessControlStore를 bottlenote-test-support fixture로 이동, product/admin Security chain 통합 테스트(허용·403·429·관리 API 인증·unban 탈출) 추가. commit/push 없음.
- 2026-08-08 Verify L3: compile, Kotlin compile, rule 64/64, unit 594/594, build, mono integration 4/4, product integration 300/300 통과. admin access-control 전용 5/5 및 한 차례 전체 246/246 통과를 확인했다.
- 2026-08-08 Verify stop: 잔여 Grok 테스트 프로세스의 동시 실행을 제거한 뒤 재검증했으나, 범위 밖 기존 admin 테스트에서 TastingTag 2건과 AgentAuth 2건이 실패했다. TastingTag 20/20은 단독 재현에서 통과했고 AgentAuth는 단독 35건 중 기존 last-writer-wins 1건이 500으로 재현되어 3회 실패 stop-condition에 따라 중단했다.
- 2026-08-09 Task 1 확정 커밋: 5축 self-review(Critical 0) 후 코드 보완 없이 검증 — mono access-control unit 28/28, Redis IT 4/4, product AC IT 3/3, admin AC IT 5/5, check_rule_test 64/64 통과. `plan/ip-ban-persistence.md`·서브모듈 제외 pathspec 커밋.
