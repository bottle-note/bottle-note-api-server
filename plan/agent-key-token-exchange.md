# 에이전트 API Key 기반 User/Admin 토큰 교환 V7 재작업

## Status

- 상태: 구현 완료, PR CI 대기
- 재개봉일: 2026-08-01
- 관련 이슈: `bottle-note/workspace#340`
- 이전 구현: V6 및 역방향 FK 설계 폐기

## Overview

기존 `users`와 `admin_users` 스키마를 변경하지 않고, `agents`가 전용 Product/Admin 계정을 참조하도록 FK 방향을 바로잡는다. 이미 사용 중인 V6 다음 버전인 V7에서 여섯 에이전트 프로필과 계정을 시드하며 기존 Product/Admin JWT 교환 계약은 유지한다.

### Assumptions

1. 기존 Agent V6는 적용 대상이 아니며 다른 마이그레이션이 V6를 선점했으므로 Agent 마이그레이션은 V7로 교체한다.
2. `users`와 `admin_users`는 스키마를 변경하지 않으며 전용 계정 6개 INSERT만 허용한다.
3. `agents.product_user_id`, `agents.admin_user_id`가 각각 기존 테이블의 bigint PK를 참조하고, 시드 시 이메일로 ID를 조회한다.
4. 두 계정 테이블에 `bottlenote.agent.a@email.com`부터 `bottlenote.agent.f@email.com`까지 동일한 이메일을 사용한다.
5. 최종 `bn_agent_` API Key 6개를 새로 생성하고 V7에는 SHA-256 해시만 기록한다. 원문 목록은 서브모듈 루트의 `agent/api-keys.sops.yaml`에 age 암호화해 보관한다.
6. 에이전트당 활성 API Key는 하나이며 Agent 프로필과 API Key 정보를 `agents` 단일 테이블에 둔다.
7. 기존 Product/Admin 엔드포인트, JWT, refresh, 마지막 로그인, 오류 및 감사 계약은 변경하지 않는다.

### Success Criteria

- Agent 마이그레이션 파일이 V7이며 `users` 또는 `admin_users`에 대한 `ALTER TABLE`이 없다.
- `agents`가 프로필 정보, Product/Admin FK, API Key 해시와 사용 시각을 소유한다.
- `0001`~`0006`과 a~f 전용 계정이 각각 정확히 매핑되고 계정 이메일의 UNIQUE 제약을 활용해 ID를 결정한다.
- API Key 원문은 SQL·코드·커밋·로그에 없고, `agent/api-keys.sops.yaml`에서만 SOPS 암호문으로 보관되며 6개 해시가 서로 다르고 원문과 6/6 대응한다.
- Product/Admin 토큰 교환의 성공, 400, 통합 401, refresh 및 감사 계약이 기존과 동일하다.
- 로컬에서는 Docker 없는 컴파일·단위/정적 검증만 수행하고, 통합 검증은 사용자가 승인한 PR CI에서 수행한다.

### Impact Scope

- 환경변수 서브모듈 Flyway V7·시드 및 루트 `agent/` SOPS 자격증명
- mono의 Agent/User/Admin 엔티티·Repository·Facade·Service
- product-api와 admin-api의 Fake·단위·통합·RestDocs 테스트
- API 요청·응답 및 JWT/SecurityContext/감사 타입은 변경 없음

## Execution Mode

- mode: step-by-step
- scope: plan, implement, test, verify
- commit, push, pr: 사용자 별도 승인 전 수행하지 않음
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 되돌리기 어려운 행동

## Tasks

### Task 1: V7 스키마와 시드 교체
- Acceptance: Agent V6를 폐기하고, `agents`가 프로필·계정 FK·API Key 정보를 소유하는 V7과 a~f 계정/프로필 시드를 제공한다. 기존 계정 테이블에는 컬럼을 추가하지 않고 원문 키 목록은 루트 `agent/` 아래 SOPS 암호문으로만 보관한다.
- Verification: SQL 정적 검사, SOPS decrypt 검사, 키 해시 6/6 대응·중복·원문 노출 검사
- Files (advisory): `git.environment-variables/storage/db/migration/V6__add_agent_key_auth.sql`, `git.environment-variables/storage/db/migration/V7__add_agent_key_auth.sql`, `git.environment-variables/agent/api-keys.sops.yaml`
- Depends: 없음
- Size: S
- Status: [x] done

### Task 2: 공통 Agent 계정 매핑 확장
- Acceptance: Agent 엔티티와 조회 계약이 V7 컬럼에 맞고 Product/Admin ID를 제공한다. 기존 `agentId` 호환 계약은 소비 서비스 전환 전까지 임시 유지한다.
- Verification: mono 및 test-support compile
- Files (advisory): Agent 엔티티·상태·Repository·Facade payload, InMemory Agent Repository
- Depends: Task 1
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] Docker 없는 컴파일 및 관련 단위 테스트 통과

### Task 3: Product 토큰 교환 조회 전환
- Acceptance: Product 교환이 Agent의 Product 사용자 ID로 기존 User를 조회하며 User의 `agent_id`와 역조회 계약을 제거하고 응답·쿠키·오류·refresh·감사 계약을 유지한다.
- Verification: Product compile, AuthService 단위 테스트 및 비-Docker 계약 검사
- Files (advisory): `AuthService`, Product Fake repository, Product Agent 로그인 테스트
- Depends: Task 2
- Size: M
- Status: [x] done

### Task 4: Admin 토큰 교환 조회 전환
- Acceptance: Admin 교환이 Agent의 Admin 사용자 ID로 기존 AdminUser를 조회하며 AdminUser의 `agent_id`와 역조회 계약 및 임시 Agent `agentId` payload를 제거하고 응답·오류·refresh·감사 계약을 유지한다.
- Verification: Admin/mono compile, AdminAuthService 단위 테스트 및 비-Docker 계약 검사
- Files (advisory): `AdminAuthService`, Admin Agent 로그인 테스트
- Depends: Task 2
- Size: M
- Status: [x] done

## Progress Log

- 2026-08-01: V6 선점, 계정 테이블 무변경, Agent 소유 FK, 단일 테이블 API Key 모델로 기존 완료 계획을 재개봉했다.
- 2026-08-01: 새 API Key 6개 생성, V7 해시 저장, 서브모듈 루트 `agent/api-keys.sops.yaml` 원문 암호화, 로컬 Docker 제외 및 커밋·푸시 별도 승인 조건을 확정했다.
- 2026-08-01: Task 1에서 Agent V6를 폐기하고 V7을 작성했다. `agent/api-keys.sops.yaml` 복호화 기준 키·코드·형식·해시가 각각 6/6, 중복 0건, V7 해시 일치 6/6, 평문 노출 0건, 계정 테이블 ALTER 0건을 확인했다.
- 2026-08-01: Task 2에서 Agent를 V7의 프로필·상태·계정 ID·API Key 메타데이터에 맞추고 Repository/Facade/InMemory 계약을 전환했다. mono/test-support 컴파일과 Product AuthService 및 AdminAuthService 단위 테스트 20개를 통과했다.
- 2026-08-01: Task 3에서 Product 교환을 `Agent.productUserId` 기반 `findById` 조회로 전환하고 User의 `agent_id` 및 역조회 계약을 제거했다. Product 테스트 컴파일과 AuthService 단위 테스트 14개를 통과했고 Product 범위의 기존 Agent 역참조가 0건임을 확인했다.
- 2026-08-01: Task 4에서 Admin 교환을 `Agent.adminUserId` 기반 `findById` 조회로 전환하고 AdminUser의 `agent_id`, 역조회 계약, Agent payload/빌더의 임시 호환 코드를 제거했다. 전체 관련 테스트 소스 컴파일과 Product/Admin 인증 단위 테스트 20개를 통과했으며 기존 Agent 역참조가 0건임을 확인했다.
