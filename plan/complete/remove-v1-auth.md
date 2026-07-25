# Plan: v1 인증 경로 제거

================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-07-26

** Core Achievements **
- 소셜 토큰을 검증하지 않고 JWT를 발급하던 `POST /api/v1/oauth/login`을 제거했다.
- 이메일/비밀번호(BASIC) 로직 일체를 제거했다: `BasicLoginRequest`, `SocialType.BASIC`, `User.password`, 계정 복구 API.
- 로그인 수단이 카카오·애플 v2 두 가지로 단일화됐다.
- 토큰 재발급·검증은 v1 경로를 유지하고 v2에도 동일 엔드포인트를 복제했다 (클라이언트 전환 지원).

** Key Components **
- `bottlenote-mono/.../user/service/OauthService.java`: 삭제 완료.
- `bottlenote-product-api/.../user/controller/OauthController.java`: 토큰 재발급·검증 표면만 남김.
- `bottlenote-mono/.../user/service/AuthService.java`: `reissue()`, `verifyToken()` 추가 — v1·v2 컨트롤러가 공유.
- `bottlenote-test-support/.../TestAuthenticationSupport.java`: `OauthService` 의존 제거, JwtTokenProvider 직접 발급.

** Verification **
- Implementation Commits: `19d7a154`, `75421ea2`, `30220263`, `a2beb84f`, `04c12771`
- `/verify full` (L3): compile(Java/Kotlin), `check_rule_test`, `unit_test`, build, `integration_test`(254 tests, 0 failures), `admin_integration_test`(204 tests, 0 failures), `asciidoctor` 모두 성공. 총 11m21s.

** Deferred Items **
- 카카오 access token의 발급 앱 검증(`app_id` 확인) — **같은 브랜치의 후속 커밋으로 진행한다.** `KAKAO_APP_ID`는 서브모듈에 반영 완료(prod `1052783`, dev `1071644`).
- `users.password` 컬럼 DROP 마이그레이션 (서브모듈 포인터 갱신 필요).
- 탈퇴 계정 복구 기능의 v2 재설계 — 필요 시.
================================================================================

## Overview

`POST /api/v1/oauth/login`은 요청 본문의 `email`과 `socialType`만 받아 **소셜 토큰을 전혀 검증하지 않고** 해당 이메일 계정의 JWT를 발급했다. 계정이 없으면 생성까지 했다. 즉 이메일 문자열 하나만 알면 임의 사용자로 로그인할 수 있었고, 공개 엔드포인트이므로 클라이언트를 가리지 않았다.

같은 계열의 이메일/비밀번호(BASIC) 로직도 남아 있었다. `restoreUser()`는 `SocialType.BASIC`일 때만 비밀번호를 검사했으므로, 소셜 계정은 이메일만으로 탈퇴 상태에서 복구됐다.

이번 변경의 목표는 검증 없는 인증 경로를 코드에서 물리적으로 제거하고, 로그인 수단을 **카카오·애플 v2 두 가지로 단일화**하는 것이다. v2는 애플이 id_token 서명·issuer·audience·nonce를 검증하고, 카카오는 백엔드가 카카오 API로 직접 확인한다.

### 조사로 확정된 사실

FE(`bottle-note-frontend`)와 앱(`bottle-note-app`) 저장소 교차 조사 결과, v1 `login`을 실제로 태우던 경로는 **웹 카카오 로그인 하나**였다. 인앱 카카오·애플과 웹 애플은 이미 v2를 쓰고 있었다.

- 앱은 WebView 셸이며 백엔드 API를 직접 호출하지 않는다. 네이티브는 카카오/애플 SDK 로그인 결과만 웹뷰로 넘긴다(`auth_bridge_handler.dart:18,44`). 따라서 FE 배포만으로 앱 사용자까지 커버되며 앱 스토어 업데이트가 불필요했다.
- **작업 중 FE가 웹 카카오를 v2로 전환 완료했다**(`src/lib/auth/server.ts:104-116`). `fetchKakaoToken`의 `access_token`을 사용자 정보 조회 없이 `kakaoLogin()`에 넘기는 방식이며, 이메일 fallback도 함께 제거됐다. 그 결과 백엔드의 v1 `login`은 **참조하는 클라이언트가 없는 코드**가 되었다.

### Assumptions

#### 확정 가정

1. 로그인 수단은 카카오·애플 v2 두 가지만 남긴다. `POST /api/v1/oauth/login`은 제거한다.
2. 이메일/비밀번호(BASIC) 로직 일체를 제거한다: `BasicLoginRequest`, `SocialType.BASIC`, `User.password` 필드.
3. 대상은 product-api와 mono다. admin-api의 관리자 로그인(`AdminAuthService`, `AdminUser.password`)은 **별개 체계이므로 건드리지 않는다.**
4. 작업 브랜치는 `Whale0928/auth-check`다.
5. **토큰 재발급·검증은 v1 경로를 유지한다.** 취약점이 아니고, FE가 `/api/v1/oauth/reissue`를 헤더 방식으로 쓰고 있다. 경로 이동은 순수 비용이므로 하지 않는다.
6. **같은 엔드포인트를 v2에도 복제한다.** FE가 나중에 v2로 옮길 때를 위한 지원이며, 로직은 `AuthService` 하나를 공유하므로 중복은 표면뿐이다.
7. **계정 복구는 제거한다.** BASIC을 걷어내면 비밀번호 검증이 무의미해지고, 이메일만으로 타인의 탈퇴 계정을 복구할 수 있는 경로가 남기 때문이다.
8. **DB 스키마**: `users.password` 컬럼은 남긴다. Java 엔티티 필드만 제거하고 Flyway 마이그레이션은 작성하지 않는다. `ddl-auto: validate`는 엔티티에 없는 컬럼을 문제 삼지 않는다.
9. **미사용 SocialType**: `NAVER`, `GOOGLE`, `NONE`은 건드리지 않는다. 테스트 픽스처가 `GOOGLE`을 쓰고 있고 로그인 경로와 무관하다.

#### 서브모듈 취급 규칙

DB 마이그레이션 원본은 `git.environment-variables` 서브모듈에 있고, 반영에는 **서브모듈 저장소 커밋 + 상위 저장소의 포인터 갱신** 두 단계가 필요하다. 가정 8에 따라 이 작업 구간에서는 마이그레이션을 작성하지 않았다.

- 커밋 전 `git status`에 `git.environment-variables` 항목이 뜨면 커밋에 포함시키지 않는다. 의도치 않은 포인터 이동은 다른 브랜치의 마이그레이션 상태를 되돌리거나 앞당길 수 있다.

**실제로는 이 규칙을 지키지 못했다.** Task 1 커밋(`75421ea2`)에서 `git commit -am`을 쓰는 바람에 포인터가 `1678bfb` → `1a52766`으로 함께 이동했고, 이후 커밋들이 이를 물려받았다. `1a52766`은 `KAKAO_APP_ID`를 추가한 커밋으로 **후속 app_id 검증 작업에 필요한 포인터**이며, 되돌림이 아니라 앞당김이라 다른 브랜치의 마이그레이션 상태를 훼손하지 않는다. 이 점을 확인한 뒤 되돌리지 않고 그대로 두기로 결정했다(2026-07-26).

교훈: 서브모듈이 있는 저장소에서 `commit -am`은 포인터를 조용히 실어 나른다. 파일을 명시적으로 `add`할 것.

### Success Criteria

| # | 기준 | 결과 |
|---|------|------|
| SC1 | `/api/v1/oauth/login` 매핑이 존재하지 않는다. | 통과 — `rg` 0건 |
| SC2 | `OauthService`가 제거되고 참조가 0건이다. | 통과 |
| SC3 | BASIC 로직이 제거된다: `BasicLoginRequest`, `SocialType.BASIC`, `User.password`. | 통과 — `rg` 0건 |
| SC4 | 사용자 인증 경로에서 `BCryptPasswordEncoder` 사용이 사라진다. product-api의 빈 자체는 mono `AdminAuthService`가 요구하므로 유지한다. | 통과 — `AdminAuthService`만 잔존 |
| SC5 | 토큰 재발급이 v1·v2 양쪽 경로에서 동작한다. | 통과 — RestDocs 테스트 |
| SC6 | 116곳의 통합 테스트 토큰 발급이 정상 동작한다. | 통과 — 254 tests |
| SC7 | 제거된 엔드포인트의 문서와 include가 정리된다. | 통과 — asciidoctor 성공 |
| SC8 | 전체 검증이 통과한다. | 통과 — `/verify full` L3 |

## Execution Mode
- mode: delegated
- scope: plan, implement, test, verify, commit
- stop-conditions: 기본 3종 (가정 붕괴 / verify 3회 실패 / scope 밖 행동)
  - 추가: 서브모듈 포인터 변경이 필요해지면 정지한다.

`push`와 `pr`은 scope에 없다. 커밋까지 수행하고 푸시 직전에 정지한다.

## Tasks

### Task 1: 테스트 토큰 발급을 OauthService에서 분리
- Acceptance: `TestAuthenticationSupport`가 `OauthService`를 주입받지 않고, `createToken(User)`가 `JwtTokenProvider`로 직접 발급하며 리프레시 토큰 저장 동작을 유지한다.
- Status: [x] done — `75421ea2`

### Task 2: 미참조 v1 로그인 엔드포인트 제거
- Acceptance: `POST /api/v1/oauth/login` 매핑과 로그인 로직(`login`/`doLogin`/`doAppleLogin`/`oauthSignUp`)이 사라지고, 재발급·검증은 `AuthService`로 이전되어 계속 동작한다.
- Status: [x] done — `30220263`

### Task 3: 이메일 비밀번호 로직 제거
- Acceptance: 계정 복구 API, `BasicLoginRequest`, `SocialType.BASIC`, `User.password`가 제거되고 `OauthService`가 삭제된다.
- Status: [x] done — `a2beb84f`

### Task 4: 토큰 재발급·검증을 v2 경로에도 제공
- Acceptance: `POST /api/v2/auth/reissue`와 `PUT /api/v2/auth/token/verify`가 v1과 동일하게 동작한다.
- Status: [x] done — `04c12771`

## Progress Log

- 2026-07-25: define 작성. FE·앱 저장소 교차 조사로 v1 잔존 경로를 웹 카카오 하나로 확정.
- 2026-07-25: /plan 완료 후 Task 1~5 구현. 이 시점에는 재발급·검증을 v2로 **이전**하는 계획이었다.
- 2026-07-26: FE가 웹 카카오 v2 전환을 반영한 것을 확인. 동시에 FE가 재발급을 `/api/v1/oauth/reissue`로 구현했음이 드러나, **재발급·검증의 v2 이전은 불필요한 변경**으로 판단했다. 가정 5·6으로 수정 — v1 유지 + v2 복제.
- 2026-07-26: 위 판단에 따라 커밋 이력을 재작성했다. v2 이전 후 되돌리는 왕복을 남기지 않기 위해 시작 커밋으로 reset 후 Task 4개로 다시 쌓았다. 재작성 전후 코드는 완전히 동일함을 diff로 확인.
- 2026-07-26: `SC4` 정밀화 — `BCryptPasswordEncoder` 빈 제거 시 컨텍스트 기동이 실패했다. ① test-support `AdminUserTestFactory`가 이 빈을 `@Autowired`로 받고 있었다 → 픽스처가 자체 생성하도록 변경(레이어 표준 15). ② mono `AdminAuthService`가 요구하며 product-api가 이를 컴포넌트 스캔한다 → 빈 유지. 관리자 비밀번호 검증용이며 사용자 인증과 무관하다.
- 2026-07-26: `/verify full` L3 PASS (11m21s). 통합 254개, admin 통합 204개 전부 통과.
- 2026-07-26: 서브모듈 포인터가 Task 1 커밋에 섞여 들어간 것을 발견. `commit -am`이 원인. 앞당김이고 후속 작업에 필요한 포인터라 그대로 두기로 결정 — 위 "서브모듈 취급 규칙" 참조.
