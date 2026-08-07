# Plan: AgreementGate 정책 (이슈 #365)

## Overview

product-api에서 인증된 사용자가 필수 약관에 동의하지 않은 채 보호 API를 호출하면 `AGREEMENT_REQUIRED`(403)로 차단한다. 동의 조회·제출, 탈퇴, 토큰 재발급은 면제하며, admin-api와 batch에는 적용하지 않는다.

### Assumptions

1. 자격 판정은 기존 `AgreementFacade.isEligible` / `AgreementEvaluator` 결과를 그대로 사용한다.
2. `SecurityContextUtil.getUserIdByContext()`로 userId를 얻었을 때만 게이트를 평가한다. 비인증·익명(-4L)은 통과한다.
3. 인증/권한은 기존 JWT·`@SecurityPolicy`가 담당하며, AgreementGate는 그 이후 웹 계층 인터셉터로 동작한다.
4. 로그아웃 API와 정보주체 권리 API는 현재 코드베이스에 없다. 추가되면 `@AgreementExempt`를 붙인다.
5. migration, JWT/OAuth, GlobalResponse 구조는 변경하지 않는다.
6. 로컬 검증은 compile, focused unit, `check_rule_test`를 통과 기준으로 한다.

### Success Criteria

1. `AgreementExceptionCode.AGREEMENT_REQUIRED`가 HTTP 403과 함께 존재한다.
2. `@AgreementExempt`를 메서드/클래스에 선언할 수 있다.
3. `AgreementGateInterceptor`가 인증 userId에 대해 `isEligible == false`이면 `AgreementException`을 던진다.
4. 면제·비인증·충족 사용자는 통과한다.
5. product-api 전용 `WebMvcConfigurer`로 `/api/**`에만 등록된다.
6. 면제 대상: `AgreementController` 전체, `DELETE /api/v1/users`, `POST /api/v2/auth/reissue`.
7. 단위 테스트로 면제/미충족/충족/비인증을 검증한다.

### Impact Scope

- `bottlenote-mono`: 예외 코드, `@AgreementExempt`
- `bottlenote-product-api`: 인터셉터, WebMvc 등록, 컨트롤러 면제 선언
- `bottlenote-test-support`: `FakeAgreementFacade`
- admin-api, batch, DB schema, JWT/OAuth 제외

### Policy

| 조건 | 결과 |
|------|------|
| HandlerMethod 아님 | 통과 |
| `@AgreementExempt` (메서드 또는 클래스) | 통과 |
| SecurityContext에 유효 userId 없음 | 통과 |
| userId 있고 `isEligible=true` | 통과 |
| userId 있고 `isEligible=false` | 403 `AGREEMENT_REQUIRED` |

### Exempt Endpoints

| 대상 | 방식 | 이유 |
|------|------|------|
| `AgreementController` (`/api/v2/agreements/**`) | 클래스 `@AgreementExempt` | 동의 상태 조회·제출 자체가 게이트 해소 경로 |
| `DELETE /api/v1/users` | 메서드 `@AgreementExempt` | 미동의 사용자도 탈퇴 가능해야 함 |
| `POST /api/v2/auth/reissue` | 메서드 `@AgreementExempt` | 세션 유지용 재발급 차단 방지 |
| 로그아웃 | 없음 | API 미존재 |
| 정보주체 권리 API | 없음 | API 미존재 |

## Execution Mode

- mode: step-by-step (orchestration worker 1회 구현)
- scope: implement, test, verify(local unit+rule), commit
- stop-conditions: 가정 붕괴, verify 3회 실패, production 배포, 금지 범위 변경

## Tasks

### Task 1: 예외 코드와 면제 어노테이션
- Acceptance: `AGREEMENT_REQUIRED`(403), `@AgreementExempt` 추가
- Status: [x] done

### Task 2: 인터셉터와 product-api 등록
- Acceptance: 인증 userId만 평가, 미충족 시 예외, `/api/**` 등록, admin/batch 미영향
- Status: [x] done

### Task 3: 면제 적용
- Acceptance: AgreementController 전체, 탈퇴, 재발급 면제
- Status: [x] done

### Task 4: 테스트와 검증
- Acceptance: Interceptor 단위 테스트, FakeAgreementFacade, compile + focused unit + check_rule_test
- Status: [x] done

## Progress Log

- 2026-08-08: AgreementGate 구현 착수. product-api 전용 인터셉터 등록과 면제 정책을 문서화했다.
- 2026-08-08: `AGREEMENT_REQUIRED`, `@AgreementExempt`, `AgreementGateInterceptor`, product-api WebMvc 등록, 면제 적용 완료. 단위 테스트 6개와 rule 테스트 통과.
