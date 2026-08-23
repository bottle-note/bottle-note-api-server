# Plan: Redis Sentinel 연결 지원

## Overview

개발 환경의 Redis 장애 전환을 애플리케이션이 직접 인식할 수 있도록 Spring Data Redis Sentinel 연결을 지원한다. 기존 standalone/cluster 연결과 Redis 반열림 복원력 설정은 유지하며, Sentinel과 데이터 노드의 인증 정보를 분리해 주입한다.

### Assumptions

1. 적용 대상은 `product-api`, `admin-api`, `batch`이며 개발 환경부터 Sentinel mode를 사용한다.
2. 운영 환경은 이번 변경 대상이 아니며 기존 Redis 연결 방식을 유지한다.
3. Sentinel master group은 `bottlenote-master`, Sentinel 노드는 개발 클러스터의 headless Service DNS 3개를 사용한다.
4. 개발 환경에서는 Sentinel 비밀번호와 Redis 데이터 노드 비밀번호가 동일한 `redis-dev-secret/password`를 사용하지만 애플리케이션 속성은 분리한다.
5. standalone과 cluster mode의 기존 동작 및 TCP keepalive, TCP_USER_TIMEOUT, command timeout 정책은 유지한다.
6. 로컬 포트포워드만으로 Sentinel이 반환하는 클러스터 내부 master 주소까지 라우팅할 수 없으므로, 실제 Sentinel 인증·master 조회와 애플리케이션 연결 검증을 분리하되 최종적으로 클러스터 네트워크에서 backend 연결을 확인한다.

### Success Criteria

1. `REDIS_MODE=sentinel`에서 product-api, admin-api, batch가 Sentinel을 통해 master를 발견할 수 있다.
2. Sentinel username/password와 Redis 데이터 노드 username/password가 각각 Spring Boot 공식 속성으로 바인딩된다.
3. lookup 및 access-control 전용 Lettuce factory도 Sentinel 구성을 보존하고 기존 2초/200ms timeout 정책을 적용한다.
4. standalone과 cluster mode 회귀 테스트가 통과한다.
5. 개발 Secret은 평문 노출 없이 Sentinel 환경변수를 제공하고 기존 `REDIS_HOST`, `REDIS_PORT` 의존을 제거한다.
6. 실제 개발 Sentinel에 대해 인증, master group 조회, Redis 데이터 노드 연결이 검증된다.
7. 전체 compile, unit, rule, integration, build 검증이 통과하고 독립 리뷰에서 병합 차단 이슈가 없다.

### Impact Scope

- `bottlenote-mono`: Redis connection factory와 전용 Lettuce factory
- `bottlenote-product-api`, `bottlenote-admin-api`, `bottlenote-batch`: Spring Redis Sentinel 속성 바인딩
- `git.environment-variables`: 개발 backend Secret 환경변수
- Redis 관련 단위·통합 테스트와 실제 개발 Sentinel 연결 검증

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 행동
- deployment: excluded

## Tasks

### Task 1: Sentinel connection factory 지원
- Acceptance: Sentinel master/nodes와 Sentinel·데이터 노드 인증을 사용해 `LettuceConnectionFactory`를 생성하고 standalone/cluster 동작을 유지한다.
- Verification: Redis configuration 단위 테스트와 `./gradlew compileJava check_rule_test`
- Files (advisory): `RedisConfig`, Redis configuration tests
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: 전용 Lettuce factory의 Sentinel 구성 보존
- Acceptance: lookup 및 access-control 전용 factory가 Sentinel master/nodes/인증과 공용 socket 복원력 정책을 유지한다.
- Verification: `LettuceClientSupportTest`와 관련 lookup/access-control 단위 테스트
- Files (advisory): `LettuceClientSupport`, `LettuceClientSupportTest`
- Depends: Task 1
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] 컴파일 통과 / Redis 단위 테스트 8건 통과 / ArchUnit 룰 통과

### Task 3: backend Sentinel 속성 바인딩
- Acceptance: product-api, admin-api, batch가 동일한 Spring Boot Sentinel 환경변수 계약을 제공하고 standalone/cluster 설정과 호환된다.
- Verification: 모듈별 configuration compile 및 Spring context 속성 바인딩 테스트
- Files (advisory): 세 backend의 `application-datasource.yml`, 관련 configuration tests
- Depends: Task 1
- Size: M
- Status: [x] done

### Task 4: 개발 Secret의 Sentinel 환경변수 전환
- Acceptance: 개발 product/admin 공유 Secret과 batch Secret이 Sentinel master/nodes/인증 및 데이터 노드 인증을 평문 노출 없이 제공하고 기존 host/port 의존을 제거한다.
- Verification: SOPS MAC 검증, `kubectl kustomize --enable-alpha-plugins --enable-exec` 또는 프로젝트 기존 렌더 명령
- Files (advisory): 개발 SOPS Secret 2개, parent submodule pointer
- Depends: Task 3
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 3-4
- [x] 설정 바인딩 검증 통과 / SOPS 검증 통과 / KSOPS 렌더 결과의 환경변수 key 확인

### Task 5: 실제 개발 Sentinel 연결 검증
- Acceptance: 실제 개발 Sentinel 인증·master 조회가 성공하고 클러스터 네트워크에서 backend가 Sentinel을 통해 데이터 노드에 연결한다.
- Verification: 포트포워드 Sentinel 명령, backend smoke test, 연결 모드 및 master 로그 확인
- Files (advisory): 없음
- Depends: Task 2, Task 4
- Size: S
- Status: [ ] not done

## Progress Log

- 2026.08.23: WHAT과 delegated 실행 범위 승인
- 2026.08.23: Tasks 1-5 분해 완료
- 2026.08.23: Task 1 완료 - Sentinel master/nodes 및 분리 인증 factory 구현, 관련 단위 테스트 2건과 rule 검증 통과
- 2026.08.23: Task 2 완료 - 전용 Lettuce factory의 Sentinel 설정 보존, Redis 관련 단위 테스트 8건 통과
- 2026.08.23: Task 3 완료 - 세 backend의 Sentinel 및 데이터 노드 환경변수 바인딩 추가, YAML 파싱과 processResources 통과
- 2026.08.23: Task 4 완료 - 개발 SOPS Secret 2개 전환, 복호화 계약 검증과 KSOPS 렌더 검증 통과
