# 에이전트 키 기반 User/Admin 토큰 교환

## Status

- 상태: 완료 (2026-08-01)
- 시작일: 2026-08-01
- 관련 이슈: `bottle-note/workspace#340`

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 되돌리기 어려운 행동

## 결정 사항

- 독립 에이전트 프로필 6개를 만들고 `profile_code`는 `0001`~`0006`으로 고정한다.
- 각 프로필은 Agent UUID, 비밀 UUID 키, Product `ROLE_USER` 계정, Admin `ROOT_ADMIN` 계정을 하나씩 가진다.
- Product `POST /api/v2/auth/agent`와 Admin `POST /auth/agent`에서 기존 JWT 계약으로 교환한다.
- 처리 순서는 `UUID 정규화 -> SHA-256 -> 활성 Agent 조회 -> agent_id 계정 조회 -> 기존 JWT/refresh/마지막 로그인 갱신`이다.
- 잘못된 UUID는 400, 미등록·비활성 Agent·매핑 누락·비활성 계정은 같은 401로 응답한다.
- 기존 JWT 필터·SecurityContext·감사 주체 타입은 변경하지 않는다.
- HMAC, scope, cache, 요청별 API-key 필터, 관리 UI·키 관리 API는 제외한다.

## Tasks

### Task 1. V6 스키마와 시드

- [x] `agents` 테이블과 `users.agent_id`, `admin_users.agent_id` FK/UNIQUE를 추가한다.
- [x] Agent/User/Admin 각 6개 행을 시드하고 키 원문은 SQL·커밋·로그에 남기지 않는다.
- [x] 환경변수 서브모듈 브랜치와 PR을 만들고 부모 포인터를 PR HEAD로 갱신한다.

### Task 2. 공통 Agent 도메인과 Product 교환

- [x] Agent 도메인·포트·JPA 구현과 SHA-256/UUID 정규화 로직을 추가한다.
- [x] Product 사용자 매핑 조회와 기존 OAuth 동일 토큰·refresh-cookie 발급을 구현한다.
- [x] 성공/400/통합 401/refresh 회전 및 RestDocs 계약 테스트를 추가한다.

### Task 3. Admin 교환

- [x] Admin 매핑 조회와 기존 로그인 동일 `GlobalResponse<TokenItem>` 발급을 구현한다.
- [x] 성공/400/통합 401/refresh 회전 및 RestDocs 계약 테스트를 추가한다.

### Task 4. 통합·보안 검증과 공개

- [x] 새 DB Flyway V1~V6 및 Hibernate validate를 확인한다.
- [x] 6개 프로필의 시드·매핑·권한과 Product/Admin 기존 API 접근을 확인한다.
- [x] 감사 주체가 기존 `USER`/`ADMIN`으로 기록되는지 확인한다.
- [x] security-policy/rule/unit/integration/RestDocs와 전체 로컬 CI를 통과한다.
- [x] 서브모듈 PR·부모 포인터·부모 PR·이슈 링크를 일치시킨다.

## Progress Log

- 2026-08-01: 사용자 제공 계획을 승인된 delegated 실행 계약으로 기록했다.
- 2026-08-01: 환경변수 PR #6을 열고 V1~V6 migrate/validate, 18개 시드와 FK/UNIQUE를 검증했다. 독립 Sonnet 리뷰는 Critical 0, Important 0, Nit 1이었다.
- 2026-08-01: 공통 Agent 조회와 Product/Admin 토큰 교환을 구현하고 unit/integration/RestDocs/security-policy 테스트를 추가했다.
- 2026-08-01: Product 토큰의 기존 API 접근·USER 감사·refresh 회전, Admin 토큰의 보호 API 접근·ADMIN 감사·refresh 회전을 통합 테스트로 검증했다.
- 2026-08-01: 전체 CI에서 발견된 OpenAPI bare-response 목록 누락 1건을 수정하고 unit 531, rule 63, Product integration 277, Admin integration 220, admin test 74, RestDocs 138개를 실패 0건으로 통과했다.
- 2026-08-01: 환경변수 PR #6, 백엔드 PR #688, workspace 이슈 #340을 상호 연결하고 작업을 완료했다.
