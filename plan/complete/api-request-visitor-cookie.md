# Plan: API 요청 방문자 쿠키와 DAU 식별 기준

## Overview

PR #661의 `ApiRequestConsoleLoggingFilter`를 확장해 성공한 Product API 요청을 브라우저/웹뷰 설치 단위로 연결할 수 있는 서버 발행 방문자 쿠키를 추가한다. 이번 PR은 식별값 발행과 콘솔 로그 검증까지만 다루며 DB 저장, 일별 집계 테이블, 대시보드는 포함하지 않는다.

### Assumptions

- 대상은 Product API다. Admin API, Batch, Actuator, 정적 리소스는 방문 활동 수집 대상이 아니다.
- 모바일 앱은 BottleNote 웹 화면을 여는 persistent WebView이며 일반 모드에서 HTTP 쿠키 저장소를 사용한다.
- 방문자 쿠키는 사람 또는 물리 기기를 증명하지 않는다. 같은 쿠키 저장소를 유지하는 브라우저 또는 웹뷰 설치를 나타낸다.
- 쿠키 삭제, 앱 재설치, 웹뷰 데이터 삭제, 시크릿/비영속 웹뷰, 다른 브라우저 또는 다른 기기는 새로운 방문자로 계산한다.
- 쿠키 값에는 userId, IP, User-Agent 등 개인정보나 권한 정보를 넣지 않는다.
- 이번 PR에서 IP나 User-Agent fingerprinting은 사용하지 않는다.
- 개인정보 처리방침 반영 및 동의/거부 제공 필요성은 운영 반영 전 별도 개인정보 검토를 거친다.

### Recommended Decisions

#### 1. 쿠키 이름과 값

- 운영 이름: `__Host-bn-visitor-id`
- 값: 서버의 CSPRNG로 생성한 최소 128-bit 무작위 식별자
- 형식 예: UUID v4 또는 동등한 128-bit random value
- 클라이언트가 임의로 보낸 형식 오류 값은 신뢰하지 않고 새 값으로 교체한다.
- 이 값은 인증이나 권한 판단에 절대 사용하지 않는다.

`__Host-` prefix를 사용하면 지원 브라우저가 `Secure`, `Path=/`, `Domain` 미지정을 강제한다. API의 특정 host에만 귀속되어 다른 subdomain이 같은 이름의 쿠키를 덮어쓰는 범위를 줄인다.

로컬 HTTP 검증은 브라우저별 localhost의 Secure-cookie 처리 차이를 피하기 위해 이름과 Secure 여부를 local profile 설정으로 분리할 수 있다. 운영 속성을 약화한 local 쿠키가 운영 설정으로 승격되지 않도록 설정 테스트가 필요하다.

#### 2. 쿠키 속성

```text
Set-Cookie: __Host-bn-visitor-id=<random>;
  Max-Age=31536000;
  Path=/;
  Secure;
  HttpOnly;
  SameSite=Lax
```

- `Max-Age=31536000`: 발행 시점부터 고정 365일
- `Domain`: 미지정, host-only
- `Path=/`: `__Host-` 조건 충족
- `Secure`: HTTPS 전송만 허용
- `HttpOnly`: JavaScript 접근 불필요, XSS 노출 범위 축소
- `SameSite=Lax`: 현재 `bottle-note.com`과 API가 같은 site이고 브라우저 호출은 Next rewrite의 same-origin 경로를 사용하므로 기본 선택
- `SameSite=None`은 실제 웹뷰가 `file://`, custom scheme 또는 다른 site에서 API를 직접 호출한다는 증거가 있을 때만 사용한다. 이 경우 `Secure`와 credentials/CORS 변경을 함께 검증한다.
- 쿠키 만료는 매 요청마다 갱신하지 않는다. sliding expiration은 사실상 무기한 추적이 되므로 최초 발행 기준 365일 후 새 식별자를 발급한다.

365일은 Chrome의 persistent cookie 상한 400일보다 짧고 연간 재방문 분석이 가능하다. 2년 관행은 현재 Chrome에서 그대로 보장되지 않으며 데이터 최소화 관점에서도 채택하지 않는다.

#### 3. 활동 기록 대상

아래 조건을 모두 만족한 요청만 `api_request` 활동 로그로 기록한다.

- Product API의 `/api/v1/**` 또는 `/api/v2/**`
- 최종 HTTP status가 `200 <= status < 300`
- HTTP method가 `GET`, `POST`, `PUT`, `PATCH`, `DELETE` 중 하나
- `OPTIONS`, `HEAD`, Actuator, 정적 리소스는 제외

4xx/5xx는 활동 로그에서 제외하되 기존 예외/접근 로그는 유지한다. Redirect(3xx)는 최종 성공 활동이 아니므로 제외한다.

#### 4. DAU 정의

한 가지 숫자로 섞지 않고 다음 두 지표를 별도로 정의한다.

- `visitor DAU`: KST 하루(00:00:00~23:59:59)에 대상 요청을 1회 이상 성공한 고유 `visitorId` 수
- `member DAU`: 같은 KST 하루에 대상 요청을 1회 이상 성공한 고유 JWT `userId` 수

모든 대상 요청에 visitor cookie를 사용하므로 로그인 전후에도 같은 브라우저/웹뷰는 visitor DAU에서 한 번만 계산된다. member DAU는 로그인한 회원만 계산한다. 두 지표는 단위가 다르므로 합산해 total DAU를 만들지 않는다.

이번 PR의 콘솔 로그에는 집계에 필요한 두 식별자를 함께 남긴다.

```text
api_request 추적ID=<request-trace> 방문자ID=<stable-visitor-digest> 회원ID=<nullable>
기기유형=모바일 운영체제=Android 브라우저=Chrome 브라우저주버전=143 웹뷰=true
메서드=GET 경로=/api/v1/... 쿼리존재=true 상태=200 처리시간ms=...
```

로그 시스템 접근 범위를 고려해 raw visitorId 대신 안정적인 one-way digest를 출력하는 방안을 구현 단계에서 우선 검토한다. JWT, Refresh Token, raw query, Cookie header 전체는 절대 기록하지 않는다.

#### 5. 최초 요청과 SSR 한계

현재 frontend 브라우저 호출은 `/bottle-api/**` same-origin 경로를 Next rewrite로 Product API에 전달한다. 이 경로의 `Set-Cookie`가 실제 브라우저/웹뷰 저장소까지 전달되는지 E2E 확인이 필요하다.

반면 Next.js 서버가 `SERVER_URL`로 Product API를 직접 호출하는 SSR 요청은 브라우저 쿠키를 자동으로 전달하지 않고, 백엔드가 발행한 쿠키도 브라우저에 자동 반영되지 않는다. 쿠키 없는 모든 SSR 요청에 새 visitorId를 만들고 즉시 활동으로 기록하면 visitor DAU가 부풀 수 있다.

따라서 다음 중 하나를 구현 전에 확정해야 한다.

- 권장: 브라우저에 이미 저장되어 다시 전달된 visitor cookie가 있는 요청만 DAU 후보 로그로 기록하고, 최초 성공 응답은 쿠키만 발행한다. 최초 1회 방문만 하고 후속 API 요청이 없는 사용자는 누락될 수 있다.
- 확장안: frontend가 브라우저에 전달되는 최초 page response에서 visitor cookie를 발행하거나 백엔드 `Set-Cookie`를 명시적으로 전달한다. 정확도는 높지만 frontend 변경이 필요해 이번 PR 범위를 넓힌다.

IP, User-Agent, `Origin`, `Sec-Fetch-*`만으로 브라우저와 SSR을 구분하는 방식은 신뢰성이 부족해 채택하지 않는다.

### Success Criteria

- 유효한 방문자 쿠키가 없는 성공 Product API 응답은 정확한 이름, 365일 만료, host-only, `Path=/`, `Secure`, `HttpOnly`, `SameSite=Lax` 속성으로 쿠키를 한 번 발행한다.
- 기존 유효 쿠키가 있으면 값을 유지하고 중복 `Set-Cookie`를 발행하지 않는다.
- 잘못된 형식의 쿠키는 활동 식별자로 사용하지 않고 안전하게 교체한다.
- 같은 쿠키로 보낸 여러 2xx 요청은 동일 visitor 식별자로 로그에 연결된다.
- 인증된 요청은 visitor 식별자와 JWT userId가 함께 연결되고, 비인증 요청은 userId 없이 기록된다.
- 3xx/4xx/5xx, `OPTIONS`, `HEAD`, `/actuator/**`, 비 API 경로는 활동 로그를 남기지 않는다.
- raw query, Authorization, Refresh Token, 전체 Cookie header는 로그에 포함되지 않는다.
- Product API에만 적용되고 Admin API와 Batch에는 방문자 쿠키가 발행되지 않는다.
- 실제 frontend rewrite 및 persistent WebView에서 `Set-Cookie -> 다음 요청 Cookie` 왕복을 확인한다.
- 브라우저 cookie 저장소 삭제 후 새 visitor로 인식되는 동작을 문서화한다.
- 개인정보 처리방침 또는 동의/거부 정책의 운영 반영 여부가 배포 전 확인된다.

### Impact Scope

- `bottlenote-mono`: 현재 필터와 단위 테스트. Product 전용 적용 경계를 위해 위치 또는 등록 방식을 재검토한다.
- `bottlenote-product-api`: Security filter ordering, Product 전용 bean 등록, 통합 테스트 가능성이 있다.
- `bottlenote-observability`: 기존 traceId와 중복 생성하지 않고 현재 trace context를 재사용한다.
- `bottle-note-frontend`: 코드 변경은 기본 범위 밖이지만 Next rewrite의 Set-Cookie 전달 E2E 검증 대상이다.
- 모바일 WebView: persistent cookie store 사용 여부와 데이터 삭제/재설치 동작을 실기기 검증한다.
- DB migration, repository, service, batch aggregation, admin dashboard는 이번 PR 범위 밖이다.

### Research Evidence

- MDN `Set-Cookie`: `__Host-`는 Secure, `Path=/`, Domain 미지정을 요구하고 HttpOnly/SameSite/Max-Age 동작을 정의한다.
- Chrome Developers: Chrome 104부터 persistent cookie expiry는 최대 400일로 제한된다.
- WebKit Tracking Prevention: first-party server cookie는 일반적인 JavaScript 7일 제한과 구분되지만 third-party/CNAME cloaking 및 private browsing에는 별도 제한이 있다.
- Apple `WKHTTPCookieStore`: 각 WebView의 HTTP cookie store를 관리하며 persistent/non-persistent data store 선택에 영향을 받는다.
- Android `CookieManager`: WebView별 cookie policy와 HTTP cookie 저장을 관리한다.
- OWASP Session Management: 식별값은 의미 없는 CSPRNG 값이어야 하며 자체 생성 시 최소 128-bit, Secure/HttpOnly/SameSite와 제한적인 Domain/Path 사용을 권고한다.
- Google Analytics user metrics: active user와 total user를 구분하며, user identity 방식이 다르면 사용자 수가 달라질 수 있다. BottleNote도 visitor DAU와 member DAU를 분리한다.
- 개인정보보호위원회: 누적 행태정보는 개인 식별·추론 위험이 있으며 투명성, 사후 통제권, 안전조치를 고려해야 한다.

### Open Decision

- [결정 완료] 이번 PR은 backend-only로 유지하고, SSR 오염을 막기 위해 기존 쿠키가 재전송된 2xx 요청부터 visitor DAU 후보 로그를 기록한다.

## Tasks

### Task 1: 기존 요청 로깅 필터에 방문자 식별 흐름 통합

- Acceptance: `ApiRequestConsoleLoggingFilter`가 Product API의 2xx 응답에서만 유효한 기존 방문자 쿠키를 활동 로그에 기록하고, 쿠키가 없거나 잘못된 경우 안전한 새 쿠키만 발급한다.
- Acceptance: 필터를 JWT 인증 이후에 실행해 인증 요청에는 `userId`를 함께 기록하며, Admin API에는 등록하지 않는다.
- Acceptance: 기존 테스트가 쿠키 속성·재사용·교체, 2xx/비2xx 및 API 경로 필터링, 민감값 미노출을 검증한다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-product-api:compileJava -x spotlessApply -x spotlessJavaApply -x spotlessJava -x spotlessInternalRegisterDependencies`
- Verification: `./gradlew :bottlenote-mono:unit_test --tests 'app.bottlenote.global.logging.ApiRequestConsoleLoggingFilterTest'`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/global/logging/ApiRequestConsoleLoggingFilter.java`, `bottlenote-product-api/src/main/java/app/global/security/SecurityConfig.java`, `bottlenote-mono/src/test/java/app/bottlenote/global/logging/ApiRequestConsoleLoggingFilterTest.java`
- Size: S
- Status: [x] done

### Checkpoint: after Task 1

- [x] Product API와 mono가 컴파일된다.
- [x] 필터 단위 테스트가 통과한다.
- [x] 구현 변경 파일이 합의한 3개를 넘지 않는다.

## Progress Log

- 2026-07-16: `ApiRequestConsoleLoggingFilter`의 component 자동 등록을 제거하고 Product Security chain의 JWT 필터 뒤에 등록했다.
- 2026-07-16: 최초 2xx API 요청에는 365일 `__Host-bn-visitor-id` 쿠키만 발급하고, 유효한 쿠키가 돌아온 2xx 요청부터 traceId, visitorId digest, userId와 요청 메타데이터를 기록하도록 변경했다.
- 2026-07-16: 필터 테스트 10개 실행, failures 0, errors 0. mono/Product compile 및 집중 unit test가 통과했다.
- 2026-07-16: 로그 key를 한글로 구조화하고 User-Agent 원문 대신 기기유형, 운영체제, 브라우저, 주버전, 웹뷰 여부를 정규화해 기록하도록 확장했다.
- 2026-07-16: 확장 후 필터 테스트 11개 실행, failures 0, errors 0. mono/Product compile이 통과했다.

## Completion

- Status: COMPLETE
- PR: [#661 feat: add API request console logging filter](https://github.com/bottle-note/bottle-note-api-server/pull/661) — 2026-07-16 merge
- 계획의 Task 1과 Checkpoint가 완료됐다.
- 방문자 쿠키 발행과 식별 계약은 이후 `bottlenote-observability`의 `VisitorTelemetry` 구조로 확장됐다.
- 실제 persistent WebView 왕복과 개인정보 정책 검토는 이 backend 구현 완료와 별개의 운영 후속 확인이며, 이 문서에서 완료됐다고 주장하지 않는다.
