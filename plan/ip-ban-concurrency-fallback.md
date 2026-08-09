# Plan: IP 차단 동시 처리 최적화와 로컬 Fallback

## Overview

동시 500 요청에서도 IP 차단 조회가 서버 병목이나 Redis 연결 오류를 유발하지 않도록 정상 조회 비용을 줄이고, Redis 장애 시 마지막 정상 로컬 차단 목록으로 기존 차단을 유지한다. DB 요청별 fallback과 503 응답은 사용하지 않으며, 로컬 목록에 없는 요청과 rate limit은 fail-open한다.

### Assumptions

1. 변경 대상은 product-api 요청 경로와 mono의 access-control 구현이며, Admin 관리 API 계약과 DB 스키마는 변경하지 않는다.
2. Redis 정상 시 기존 IP ban 403 및 rate limit 429 계약을 유지한다.
3. Redis 장애 시 expiry-aware 로컬 ban snapshot에 존재하는 IP만 403으로 차단하고, 그 밖의 요청과 rate limit은 fail-open한다.
4. 로컬 snapshot은 Redis를 원본으로 30초마다 갱신하며, 마지막 성공 후 3분이 지나면 stale로 간주하고 전체 fail-open한다.
5. 요청 경로에서 DB fallback을 호출하거나 Redis 장애를 이유로 503을 반환하지 않는다.
6. 개발 서버 성능 검증은 feature branch를 수동 배포하고 완료 후 main을 다시 배포해 공유 개발 환경을 복원한다.

### Success Criteria

- Redis 정상 시 기존 Security chain의 allow, ban 403, rate limit 429 동작이 유지된다.
- Redis 장애 시 snapshot에 남아 있고 아직 만료되지 않은 IP는 403, 나머지 IP는 허용된다.
- snapshot은 읽기 요청을 블로킹하지 않고 원자적으로 교체되며, 30초 갱신·3분 stale 정책과 expiry를 테스트로 증명한다.
- Redis 장애 시 rate limit은 fail-open하며 요청 경로에서 JDBC 호출 및 503 응답이 발생하지 않는다.
- 정상 IP ban 조회는 기존 다중 Redis 명령보다 적은 단일 키 조회 중심으로 동작하고 Redis Cluster 단일 슬롯 규칙을 지킨다.
- CI에서 단위·Redis 통합·Product Security 통합·아키텍처 규칙·포맷 검증이 통과한다.
- 개발 서버 1차 고정 경량 API 시험에서 동시 500개 요청 모두 HTTP 응답을 받고, 통신 실패 0건, 5xx 0건, p95 1초 이하, p99 2초 이하를 충족한다.
- 개발 서버 2차 안전한 읽기 API 무작위 시험에서도 동시 500개 요청의 통신 실패와 5xx가 0건이며 상태 코드 분포를 기록한다.
- feature branch가 push되고 main 대상 PR이 생성되며, PR CI 결과와 개발 서버 검증 결과가 본문에 기록된다.

### Impact Scope

- `bottlenote-mono`: Redis ban 조회, 장애 분류, 로컬 snapshot, 설정·메트릭, 단위/Redis 통합 테스트
- `bottlenote-product-api`: 실제 Security chain 및 Redis 장애 fallback 통합 테스트, 설정
- `.github` 또는 부하 테스트 리소스: 재현 가능한 k6 시나리오가 필요할 경우 추가
- DB/Flyway 및 `git.environment-variables`: 변경 없음

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr, development-deploy, load-test, development-restore
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 행동

## Tasks

### Task 1: 단일 키 IP ban 조회 경로 최적화
- Acceptance:
  - 정상 Redis에서 projected ban 조회가 존재 여부와 남은 TTL을 한 명령 결과로 판정한다.
  - projected key가 없을 때만 legacy key를 조회하고 기존 ban 403 의미를 유지한다.
  - Redis 기술 장애는 명시적인 access-control 저장소 장애로 변환되며 프로그래밍 오류와 구분된다.
- Verification: mono 단위 테스트, Redis Testcontainers 통합 테스트, `spotlessCheck`, `check_rule_test`
- Files (advisory): `AccessControlStore`, `RedisAccessControlStore`, 장애 예외, Redis 통합 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: 만료 인식 로컬 ban snapshot 추가
- Acceptance:
  - Redis에서 읽은 ban 목록을 immutable snapshot으로 원자 교체한다.
  - 30초 갱신, 개별 ban expiry, 마지막 성공 후 3분 stale 시 fail-open 규칙을 제공한다.
  - 갱신 실패가 기존 snapshot을 지우거나 요청 읽기를 블로킹하지 않는다.
- Verification: Fake/InMemory 기반 단위 테스트 및 동시 읽기·갱신 테스트
- Files (advisory): snapshot 계약/구현, 갱신 컴포넌트, 설정, 단위 테스트
- Depends: Task 1
- Size: M
- Status: [x] done

### Task 3: Redis 장애 요청 판정에 snapshot fallback 연결
- Acceptance:
  - Redis ban 조회 장애 시 snapshot hit는 403, miss·expired·stale은 allow가 된다.
  - rate limit Redis 장애는 fail-open하고 JDBC/503 경로를 추가하지 않는다.
  - 기존 allow/403/429와 access-control 메트릭 계약이 유지되고 fallback 상태가 관측된다.
- Verification: `AccessControlService` 단위 테스트와 기존 access-control 회귀 테스트
- Files (advisory): `AccessControlService`, configuration, metrics, Fake store/snapshot, 단위 테스트
- Depends: Task 2
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 1-3
- [ ] mono 컴파일 통과
- [ ] access-control 단위·Redis 통합 테스트 통과
- [ ] ArchUnit 룰 통과

### Task 4: 실제 Product Security chain 장애 복구 검증
- Acceptance:
  - 실제 Product filter chain에서 Redis 정상 allow/403/429 회귀가 유지된다.
  - Redis 장애 시 snapshot ban 403과 snapshot miss allow가 실제 HTTP 응답으로 검증된다.
  - Redis 복구 후 정상 Redis 판정으로 돌아가는 경로가 검증된다.
- Verification: Product Testcontainers Security 통합 테스트
- Files (advisory): Product access-control 통합 테스트, 전용 테스트 설정/fixture
- Depends: Task 3
- Size: M
- Status: [ ] not done

### Task 5: 동시 500 요청 부하 시나리오 추가
- Acceptance:
  - 고정 경량 API 500 동시 요청과 안전한 읽기 API 무작위 500 동시 요청을 재현할 수 있다.
  - 통신 실패·5xx·p95·p99 임계값이 자동 판정되고 상태 코드 분포가 출력된다.
  - 인증 키나 환경별 URL을 저장소에 하드코딩하지 않는다.
- Verification: k6 dry-run/구문 검증과 비밀정보 누출 검사
- Files (advisory): k6 스크립트, 실행 문서 또는 workflow 보조 파일
- Depends: Task 4
- Size: S
- Status: [ ] not done

### Task 6: CI와 개발 서버에서 최종 검증
- Acceptance:
  - PR CI 필수 체크가 모두 통과한다.
  - feature branch 개발 배포 후 두 k6 시험의 합격 기준을 충족하고 수치를 기록한다.
  - 시험 후 개발 환경을 main 배포로 복원하고 배포 상태를 확인한다.
- Verification: GitHub Actions 결과, 개발 배포 상태, k6 summary, main 복원 workflow
- Files (advisory): plan Progress Log 및 PR 본문
- Depends: Task 5
- Size: S
- Status: [ ] not done

## Progress Log

- 2026-08-09: 사용자 승인. 동시 500 요청 성능 기준과 Redis 장애 시 로컬 snapshot/fail-open 계약 확정.
- 2026-08-09: Sonnet 코드 병목 조사, Opus 안전·성능 계약 검토, Terra 테스트 구조 조사 완료. 초기 Opus model ID 재시도 및 Terra Dispatch capability 복구 이력 있음.
- 2026-08-09: delegated 계획 완료. Task 6개(S 2, M 4), 의존 순서 1→2→3→4→5→6.
- 2026-08-09: Task 1 완료. 요청 경로를 non-null `BanLookup`과 raw PTTL callback으로 전환해 projected ban은 PTTL 1회·EXISTS 0회로 판정한다. Redis 기술 장애만 typed exception으로 변환하고 프로그래밍 오류는 전파한다. 독립 검증: AccessControlService 단위 16/16, Redis 통합 12/12, Spotless, 전체 `check_rule_test` 통과. Sonnet 리뷰 Critical 1·Important 3은 커밋 전 모두 해소.
- 2026-08-09: Task 2 완료. Redis ban 목록을 `Map.copyOf` immutable snapshot과 `AtomicReference`로 원자 교체하며 30초 갱신·3분 stale·10,000건 상한·개별 TTL·무기한 TTL을 제공한다. 실패 시 이전 snapshot을 유지하고 Redis TTL 초 단위 절삭은 최대 1초 보수적으로 보정했다. Opus 설계 검토와 Sonnet diff 리뷰 후 유효 지적을 반영했으며, 집중 단위 14/14, Spotless, 전체 `check_rule_test`가 통과했다.
