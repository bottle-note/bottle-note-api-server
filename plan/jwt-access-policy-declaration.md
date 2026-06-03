# JWT 접근 정책 선언화

## 목표

product/admin JWT 인증은 이미 동작 중이다. 이번 작업은 기존 동작을 바꾸는 것이 아니라, 흩어진 접근 정책을 `public`, `optional-auth`, `required-auth`로 선언화해 새 endpoint 추가 시 정책 누락을 줄이는 것이다.

## 성공 기준

- product-api의 공개/선택 인증/필수 인증 경로가 단일 정책 소스에서 판별된다.
- `JwtAuthenticationFilter`는 public 경로만 건너뛰고, optional-auth와 required-auth에서는 토큰이 있으면 사용자 컨텍스트를 세팅한다.
- optional-auth는 무토큰 접근을 허용하고, 유효 토큰은 개인화 응답을 유지한다.
- required-auth 후보 중 컨트롤러에서만 막히던 `/likes`, `/rating` 계열도 정책에 명시된다.
- admin-api의 기존 login/refresh public, 나머지 authenticated 흐름은 회귀하지 않는다.
- 악성 경로 denyAll과 GraphQL/GraphiQL 외부 차단은 유지된다.

## Tasks

- [x] Task 1. product 접근 정책 모델 추가
  - 정책 enum과 경로 매칭 객체를 추가한다.
  - public/optional-auth/required-auth 대표 경로 단위 테스트를 먼저 작성한다.
  - 기존 `skipFilter` 하드코딩 목록을 대체할 수 있는 API를 제공한다.

- [x] Task 2. product SecurityConfig와 JwtAuthenticationFilter 연결
  - SecurityConfig의 required-auth matcher를 정책 소스로 변경한다.
  - JwtAuthenticationFilter의 `shouldNotFilter`를 public 정책 기준으로 변경한다.
  - 미사용 `getAuthentication/skipFilter` 제거 또는 명확한 역할로 축소한다.

- [x] Task 3. product 통합 보안 회귀 테스트 추가
  - public, optional-auth, required-auth, invalid token, malicious path를 실제 Spring filter chain으로 검증한다.
  - TestContainers 기반 기존 `IntegrationTestSupport`를 사용한다.

- [x] Task 4. admin/batch 영향 검증
  - admin public/required-auth 기존 흐름을 대표 통합 테스트로 고정한다.
  - batch context가 product/admin web security bean을 끌어오지 않는지 확인한다.

## 검증 명령

- 빠른 검증: `./gradlew :bottlenote-mono:test --tests '*ProductApiAccessPolicyTest' --console=plain`
- 컴파일: `./gradlew :bottlenote-product-api:compileJava :bottlenote-admin-api:compileKotlin --console=plain`
- product 통합: `./gradlew :bottlenote-product-api:integration_test --console=plain`
- admin 통합: `./gradlew :bottlenote-admin-api:admin_integration_test --console=plain`
- 최종: `./gradlew unit_test integration_test admin_integration_test check_rule_test --console=plain`

## 진행 로그

- 2026-06-03: 전용 worktree `feat/jwt-access-policy-declaration`에서 시작.
- 2026-06-03: `ProductApiAccessPolicy` 추가. product 접근 정책을 `public`, `optional-auth`, `required-auth`로 선언화.
- 2026-06-03: `JwtAuthenticationFilter`의 public skip 기준을 정책 객체로 교체하고, required-auth 무토큰 요청에는 프로젝트 anonymous 인증을 주입하지 않도록 변경.
- 2026-06-03: `SecurityConfig` required-auth 판단을 정책 predicate로 연결. Spring pattern matcher와 정책 해석이 어긋나는 문제를 피하기 위해 문자열 matcher 생성은 제거.
- 2026-06-03: `JwtAuthenticationEntryPoint`가 인증 실패를 resolver로 넘기며 500으로 변환하던 문제를 401 JSON 응답 직접 생성으로 수정.
- 2026-06-03: 단위 테스트 37개 통과, product `SecurityConfigIntegrationTest` 13개 통과.
- 2026-06-03: admin `AdminAuthIntegrationTest` 통과, batch `compileJava` 통과.
- 2026-06-03: product 전체 `integration_test` 219개 통과.
