# Plan: BottleNote 1차 보안 설정 정리

## Overview

BottleNote Product API와 Admin API의 명확한 보안 위험을 작은 범위로 먼저 제거한다.
1차 변경은 refresh token 로그 제거, 운영 기본 인증값 제거, 환경별 CORS allowlist 적용으로 제한한다.
Admin Dashboard의 검색엔진 차단은 별도 저장소 PR로 분리하며, Bucket4j + Redis rate limit은 같은 API PR의 후속 단계에서 조사와 설계 승인을 거친 뒤 구현한다.

### Assumptions

- 대상 API 모듈은 `bottlenote-product-api`와 `bottlenote-admin-api`다.
- Product/Admin API는 stateless JWT 인증과 `Authorization: Bearer` 방식을 유지한다.
- CSRF 비활성화와 `allowCredentials=false`는 변경하지 않는다.
- 운영 Product API는 `https://bottle-note.com` Origin만 허용한다.
- 운영 Admin API는 `https://admin.bottle-note.com` Origin만 허용한다.
- 개발 Product API는 `https://development.bottle-note.com`, `http://localhost:3000`, `http://localhost:5173`을 허용한다.
- 개발 Admin API는 `https://admin.development.bottle-note.com`, `http://localhost:5173`을 허용한다.
- CORS Origin은 비밀값이 아니므로 프로파일별 YAML에서 관리한다. `prod.sops.env`와 development secrets는 CORS 때문에 변경하지 않는다.
- JWT secret과 Admin root password는 운영 환경변수가 없으면 기본값으로 대체되지 않고 애플리케이션 기동이 실패해야 한다.
- 테스트와 로컬 실행에 필요한 인증값은 테스트 또는 로컬 전용 설정에서 명시적으로 제공한다.
- Admin Dashboard의 `robots.txt`와 `X-Robots-Tag: noindex, nofollow`는 Admin Dashboard 저장소의 별도 PR로 처리한다. 사용자 서비스인 Product Frontend에는 적용하지 않는다.
- Bucket4j + Redis rate limit은 1차 변경 완료 이후 별도 Task로 조사·설계·구현하되 최종적으로 같은 API PR에 포함한다.
- 현재 작업 트리에 존재하는 사용자 변경 파일과 `git.environment-variables` 서브모듈 변경은 수정하거나 커밋 범위에 포함하지 않는다.

### Success Criteria

- Product API가 refresh token 원문을 어떤 로그 레벨에서도 기록하지 않는다.
- Product/Admin 운영 설정에서 JWT secret의 하드코딩 기본값이 제거된다.
- Admin 운영 설정에서 root password의 하드코딩 기본값이 제거된다.
- 필수 운영 인증 환경변수가 없을 때 해당 애플리케이션의 설정 바인딩 또는 기동이 실패한다.
- Product API 운영 preflight 요청은 `Origin: https://bottle-note.com`에 해당 Origin을 반환한다.
- Product API 운영 preflight 요청은 `Origin: https://evil.example`에 `Access-Control-Allow-Origin`을 반환하지 않는다.
- Admin API 운영 preflight 요청은 `Origin: https://admin.bottle-note.com`에 해당 Origin을 반환한다.
- Admin API 운영 preflight 요청은 Product Origin 및 임의 Origin에 `Access-Control-Allow-Origin`을 반환하지 않는다.
- 개발 API는 지정된 개발 도메인과 localhost Origin만 허용한다.
- 운영 응답 어디에도 `Access-Control-Allow-Origin: *`가 남지 않는다.
- 허용 method는 `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`를 유지한다.
- 허용 header는 실제 클라이언트가 사용하는 `Authorization`, `Content-Type`과 확인된 필수 header로 제한한다.
- `allowCredentials=false`, JWT/stateless 설정, CSRF 비활성화 동작은 유지한다.
- Product/Admin 보안 설정 테스트가 허용·비허용 Origin과 wildcard 부재를 검증한다.
- Product/Admin 컴파일과 관련 보안 테스트가 통과한다.

### Impact Scope

- **Product API**: `SecurityConfig.java`, OAuth controller의 민감정보 로그, CORS 설정 바인딩, 프로파일별 application 설정, 관련 테스트
- **Admin API**: `SecurityConfig.kt`, CORS 설정 바인딩, JWT/root admin 프로파일 설정, 관련 테스트
- **배포 설정**: 기존 환경변수 이름과 실제 운영 Secret 주입 여부를 읽기 전용으로 확인한다. CORS Origin 자체는 SOPS Secret에 추가하지 않는다.
- **Admin Dashboard**: 이번 API 저장소 변경에서 제외한다. 별도 저장소에서 `robots.txt`와 noindex header를 적용한다.
- **Persistence**: DB schema 변경 없음
- **Redis/cache**: 1차 변경 없음. Bucket4j + Redis 후속 Task에서 별도 영향 분석
- **Events/async**: 변경 없음
- **API contract**: endpoint와 응답 body 변경 없음. 브라우저가 허용되지 않은 Origin에서 호출할 때의 CORS 동작만 변경
- **Tests**: Product Java 및 Admin Kotlin security integration/configuration test 보강 필요
- **배포 검증**: 개발 환경 선배포 후 허용·비허용 Origin preflight 확인, 이후 운영 환경에서 동일 검증

### Deferred in the Same PR

Bucket4j + Redis rate limit은 다음 내용을 먼저 조사하고 사용자 승인을 받은 뒤 Tasks에 추가한다.

- Spring Boot 3.4 / Java 21 / 현재 Redis client와 호환되는 Bucket4j integration 방식
- 다중 Product API Pod에서 Redis 기반으로 공유되는 원자적 rate-limit 보장 방식
- 로그인, 토큰 재발급, 검색 및 공개 조회 endpoint별 key와 제한값
- Redis 장애 시 fail-open 또는 fail-closed 정책
- `429 Too Many Requests`, `Retry-After`, 기존 `GlobalResponse` 오류 계약
- 프록시 환경에서 신뢰할 client IP 결정 방식과 `X-Forwarded-For` 위조 방지

## Dependency Analysis

- Refresh token 로그 제거는 다른 변경에 의존하지 않는 독립 작업이다.
- 기본 인증값 제거는 운영 환경변수 주입을 전제로 하며, 현재 Kubernetes Secret key 존재 여부를 값 노출 없이 먼저 확인해야 한다.
- CORS는 모듈별 설정값이 먼저 존재해야 `SecurityConfig`가 allowlist를 주입받을 수 있다.
- Product와 Admin CORS는 서로 다른 Origin 정책과 언어(Java/Kotlin)를 사용하므로 독립 Task로 분리한다.
- CORS 변경은 DB, Redis, 도메인 계층, 이벤트 계약에 의존하지 않는다.
- Bucket4j + Redis는 현재 Redis 연결 방식과 다중 Pod 공유 동작, 적용 endpoint 및 장애 정책 결정에 의존한다.
- Rate-limit 구현 Task는 조사 결과와 사용자 결정 전에는 확정하지 않는다. 조사 Task 완료 후 이 문서에 후속 Tasks를 추가한다.

## Tasks

### Task 1: Refresh token 원문 로그 제거
- Acceptance: Product OAuth 토큰 재발급 과정에서 refresh token 원문을 어떤 로그 레벨로도 기록하지 않는다.
- Acceptance: 토큰 재발급의 요청·응답 동작은 변경하지 않는다.
- Verification: `rg -n "log\\..*refresh.*token|refresh token in request" bottlenote-product-api/src/main` 결과에 토큰 원문 로그가 없다.
- Files: `bottlenote-product-api/src/main/java/app/bottlenote/user/controller/OauthController.java`
- Size: S
- Status: [x] done

### Task 2: Product JWT secret 기본값 제거
- Acceptance: Product main 설정은 `JWT_SECRET_KEY`가 없을 때 하드코딩 값으로 대체되지 않는다.
- Acceptance: Product test 설정은 테스트 전용 JWT 값을 명시적으로 제공한다.
- Verification: `./gradlew :bottlenote-product-api:compileJava :bottlenote-product-api:compileTestJava`
- Files: `bottlenote-product-api/src/main/resources/application.yml`, 필요 시 Product test resource
- Size: S
- Status: [x] done

### Task 3: Admin 인증 기본값 제거
- Acceptance: Admin main 설정은 `JWT_SECRET_KEY`와 `ROOT_ADMIN_PASSWORD`가 없을 때 하드코딩 값으로 대체되지 않는다.
- Acceptance: Admin test 설정은 테스트 전용 JWT와 root admin password를 명시적으로 제공한다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`
- Files: `bottlenote-admin-api/src/main/resources/application.yml`, `bottlenote-admin-api/src/test/resources/application-test.yml`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-3
- [ ] Product/Admin 컴파일과 test compile이 통과한다.
- [ ] main resource에 JWT secret 및 root admin password 기본값이 없다.
- [ ] 기존 사용자 변경 파일과 `git.environment-variables` 서브모듈 diff가 보존된다.

### Task 4: Product API 환경별 CORS allowlist
- Acceptance: Product SecurityConfig가 설정 바인딩된 Origin 목록을 사용하고 wildcard Origin/header를 사용하지 않는다.
- Acceptance: prod는 Product 운영 Origin만, dev는 개발 Origin과 지정 localhost만 허용한다.
- Acceptance: 통합 테스트가 허용 Origin 반영과 비허용 Origin 거부를 검증한다.
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests 'app.global.security.SecurityConfigIntegrationTest'`
- Files: Product CORS properties, `SecurityConfig.java`, `application.yml`, `SecurityConfigIntegrationTest.java`
- Size: M
- Status: [x] done

### Task 5: Admin API 환경별 CORS allowlist
- Acceptance: Admin SecurityConfig가 설정 바인딩된 Origin 목록을 사용하고 wildcard Origin/header를 사용하지 않는다.
- Acceptance: prod는 Admin 운영 Origin만, dev는 Admin 개발 Origin과 `localhost:5173`만 허용한다.
- Acceptance: 통합 테스트가 Admin Origin 허용과 Product/임의 Origin 거부를 검증한다.
- Verification: `./gradlew :bottlenote-admin-api:admin_integration_test --tests 'app.global.security.SecurityConfigIntegrationTest'`
- Files: Admin CORS properties, `SecurityConfig.kt`, `application.yml`, Admin security integration test
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 4-5
- [ ] Product/Admin 관련 CORS 테스트가 통과한다.
- [ ] `allowedOrigins = *`와 `allowedHeaders = *`가 main 코드에 남아 있지 않다.
- [ ] JWT/stateless, CSRF 비활성화, `allowCredentials=false` 설정이 유지된다.
- [ ] 개발 배포 후 허용·비허용 preflight를 실제 HTTP 요청으로 검증한다.
- [ ] 운영 배포 후 허용·비허용 preflight를 실제 HTTP 요청으로 검증한다.

### Task 6: Bucket4j + Redis rate-limit 기술 조사
- Acceptance: 공식 문서를 근거로 Spring Boot 3.4, Java 21 및 현재 Redis 구성과 호환되는 연동 방식을 비교한다.
- Acceptance: 적용 endpoint/key/제한값, 다중 Pod 원자성, Redis 장애 정책, client IP 신뢰 경계 및 429 응답 계약의 선택지를 제시한다.
- Acceptance: 사용자 결정 전에는 Bucket4j 의존성이나 rate-limit 운영 코드를 추가하지 않는다.
- Verification: 조사 결과에 각 결정 항목, 권장안, 근거 링크 및 검증 전략이 모두 기록되어 있는지 문서 리뷰한다.
- Files: 이 계획 문서의 Rate Limit Decision 섹션 또는 별도 ADR 1개
- Size: S
- Status: [ ] not done

### Decision Checkpoint: after Task 6
- [ ] Bucket4j integration 방식이 승인되었다.
- [ ] 적용 endpoint와 제한값이 승인되었다.
- [ ] Redis 장애 정책과 429 응답 계약이 승인되었다.
- [ ] 승인 결과를 기준으로 같은 문서에 rate-limit 구현 Tasks가 추가되었다.

## Progress Log

### 2026-07-18 Task 1 완료

- OAuth 토큰 재발급 시 refresh token 원문을 남기던 INFO 로그를 제거했다.
- 요청 header 처리와 토큰 재발급 동작은 변경하지 않았다.

### 2026-07-18 Task 2 완료

- Product main 설정의 `JWT_SECRET_KEY` 하드코딩 fallback을 제거했다.
- Product test 설정의 명시적인 테스트 전용 JWT 값은 유지했다.

### 2026-07-18 Task 3 완료

- Admin main 설정의 JWT secret과 root admin password fallback을 제거했다.
- Admin test 설정에는 환경변수와 분리된 테스트 전용 root admin 값을 명시했다.

### 2026-07-18 Task 4 완료

- Product CORS Origin을 `app.cors.allowed-origins` 프로파일 설정으로 분리했다.
- 운영은 Product Origin만, 개발은 개발 Origin과 localhost 두 포트만 허용했다.
- 허용 header를 `Authorization`, `Content-Type`으로 제한하고 허용·거부 preflight 테스트를 추가했다.
