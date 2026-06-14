================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-06-04 (커밋 e5457a8c 선언화, eed8b74c/3f142a6f 정책 매칭·에러 경로 보완)

** Core Achievements **
- @SecurityPolicy 기반 public/optional-auth/required-auth 선언화, Task 4/4 완료
- 어노테이션 누락 handler는 required-auth fallback으로 수집, 악성 경로 denyAll 유지

** Key Components **
- app.bottlenote.global.annotation.SecurityPolicy
- app.bottlenote.global.security.policy.SecurityPolicyRegistry / SecurityPolicyRoute
- bottlenote-product-api app.global.security.SecurityPolicyConfig
================================================================================

# JWT 접근 정책 선언화

## 목표

product/admin JWT 인증은 이미 동작 중이다. 이번 작업은 흩어진 접근 정책을 `@SecurityPolicy` 기반 `public`, `optional-auth`, `required-auth`로 선언화하고, 어노테이션 누락 시 기본 인증 필수로 닫아 새 endpoint 추가 시 정책 누락을 줄이는 것이다.

## 성공 기준

- product-api와 admin-api의 공개/선택 인증/필수 인증 경로가 `@SecurityPolicy` 수집 결과로 판별된다.
- `JwtAuthenticationFilter`는 public 경로만 건너뛰고, optional-auth와 required-auth에서는 토큰이 있으면 사용자 컨텍스트를 세팅한다.
- optional-auth는 무토큰 접근을 허용하고, 유효 토큰은 개인화 응답을 유지한다.
- 어노테이션이 없는 handler는 fallback `required-auth`로 수집된다.
- 수집된 handler가 없는 경로는 handler fallback과 분리해 404/정적 경로에 인증을 강제하지 않는다.
- 여러 mapping이 동시에 매칭될 때 Spring `PathPattern` specificity 기준으로 가장 구체적인 정책을 선택한다.
- admin-api의 기존 login/refresh public, 나머지 authenticated 흐름은 회귀하지 않는다.
- 악성 경로 denyAll과 GraphQL/GraphiQL 외부 차단은 유지된다.
- `/error`는 product/admin 모두 public extra route로 유지해 익명 error dispatch가 401로 덮이지 않는다.
- 필터 내부 anonymous 판단은 request 기반 lookup path를 사용해 context-path 처리와 권한 판정을 일치시킨다.

## Tasks

- [x] Task 1. `@SecurityPolicy` 접근 정책 모델 추가
  - 현재 실제로 사용되는 `auth` 축만 `@SecurityPolicy`로 선언한다.
  - 기존 owner/access 축은 사용 사례가 없어 이번 PR에서 제거하고, enforcement 없는 no-op 필드는 남기지 않는다.
  - handler mapping 수집 단위 테스트를 먼저 작성한다.
  - 어노테이션 누락 handler의 fallback `required-auth`를 검증한다.

- [x] Task 2. product/admin SecurityConfig와 JWT filter 연결
  - SecurityConfig의 required-auth matcher를 `SecurityPolicyRegistry`로 변경한다.
  - JWT filter의 `shouldNotFilter`를 public 정책 기준으로 변경한다.
  - admin login/refresh public도 `@SecurityPolicy` 수집 결과로 처리한다.

- [x] Task 3. product 통합 보안 회귀 테스트 추가
  - public, optional-auth, required-auth, invalid token, malicious path를 실제 Spring filter chain으로 검증한다.
  - TestContainers 기반 기존 `IntegrationTestSupport`를 사용한다.

- [x] Task 4. admin/batch 영향 검증
  - admin public/required-auth 기존 흐름을 대표 통합 테스트로 고정한다.
  - batch context가 product/admin web security bean을 끌어오지 않는지 확인한다.

## 검증 명령

- 빠른 검증: `./gradlew :bottlenote-mono:test --tests '*SecurityPolicyRegistryTest' --tests '*JwtAuthenticationFilterPolicyTest' --console=plain`
- 컴파일: `./gradlew :bottlenote-product-api:compileJava :bottlenote-admin-api:compileKotlin --console=plain`
- product 통합: `./gradlew :bottlenote-product-api:integration_test --console=plain`
- admin 통합: `./gradlew :bottlenote-admin-api:admin_integration_test --console=plain`
- 최종: `./gradlew unit_test integration_test admin_integration_test check_rule_test --console=plain`

## 진행 로그

- 2026-06-03: 전용 worktree `feat/jwt-access-policy-declaration`에서 시작.
- 2026-06-03: `JwtAuthenticationFilter`의 public skip 기준을 정책 객체로 교체하고, required-auth 무토큰 요청에는 프로젝트 anonymous 인증을 주입하지 않도록 변경.
- 2026-06-03: `SecurityConfig` required-auth 판단을 정책 predicate로 연결. Spring pattern matcher와 정책 해석이 어긋나는 문제를 피하기 위해 문자열 matcher 생성은 제거.
- 2026-06-03: `JwtAuthenticationEntryPoint`가 인증 실패를 resolver로 넘기며 500으로 변환하던 문제를 401 JSON 응답 직접 생성으로 수정.
- 2026-06-03: 단위 테스트 37개 통과, product `SecurityConfigIntegrationTest` 13개 통과.
- 2026-06-03: admin `AdminAuthIntegrationTest` 통과, batch `compileJava` 통과.
- 2026-06-03: product 전체 `integration_test` 219개 통과.
- 2026-06-03: 사용자 요청에 따라 기존 접근 어노테이션과 static product 정책을 제거하고 `@SecurityPolicy` 기반 handler mapping 수집 구조로 전환.
- 2026-06-03: `SecurityPolicyRegistryTest`, `JwtAuthenticationFilterPolicyTest`, `MaliciousPathPatternTest` 통과.
- 2026-06-03: optional-auth API에 invalid token이 들어오면 401로 차단되도록 `SecurityConfigIntegrationTest` 회귀 테스트 추가.
- 2026-06-03: product 전체 `integration_test` 220개 통과, admin 전체 `admin_integration_test` 189개 통과, batch `compileJava` 통과.
- 2026-06-03: 리뷰 보완으로 route specificity 우선순위, 수집되지 않은 경로와 handler fallback 분리, product/admin registry matrix 테스트를 추가.
- 2026-06-03: `SecurityPolicy`의 no-op `access/key` 필드를 제거하고 required product endpoint는 `auth = REQUIRED_AUTH`로 명시.
- 2026-06-03: malicious path가 no-match public 처리로 JWT 필터를 skip해 401로 회귀하던 문제를 악성 경로 필터 실행 유지로 복원.
- 2026-06-03: 보완 후 mono test 180개, product integration 233개, admin integration 197개, rule test 62개 모두 통과. batch `compileJava`, `git diff --check`, legacy policy 명칭 잔여 검색도 통과.
- 2026-06-03: PR 리뷰 후 product/admin `resolve("GET", "/error")`가 `REQUIRED_AUTH`로 잡히는 회귀를 테스트로 재현하고, `/error` explicit public route를 추가.
- 2026-06-03: `BusinessSupportController`는 모든 메서드가 user context를 요구하므로 class-level `REQUIRED_AUTH`로 의도를 명시하고 익명 목록/상세 조회 401 테스트를 추가.
- 2026-06-03: product/admin JWT 필터의 path 산출을 `SecurityPolicyRegistry.lookupPath(request)`로 통일하고 문자열 기반 anonymous 판단 오버로드를 제거.
- 2026-06-03: 익명 required endpoint의 400 → 401 변경은 의도된 보안계층 차단이므로 릴리즈 노트와 FE 공지 대상.
