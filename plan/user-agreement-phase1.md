# Plan: 사용자 동의 Phase 1

## Overview

인증된 사용자의 약관 동의 이력을 append-only로 기록하고, 요청 시점의 정책 기준에 따라 동의 충족 여부를 하나의 Evaluator가 판정한다. Product API는 동의 상태 조회와 제출 기능을 제공하며, 소셜 로그인 응답에는 같은 판정 결과로 계산한 `agreementRequired` 힌트를 포함한다. 이번 범위에서는 보호 API를 차단하거나 403 응답을 자동 생성하지 않는다.

### Assumptions

1. 대상은 product-api의 사용자 인증 흐름이며 admin-api용 동의 API는 추가하지 않는다.
2. 필수 동의 유형은 `TERMS_OF_SERVICE`, `PRIVACY_COLLECTION_USE` 두 종류다.
3. 유형별 `effective-from`은 해당 시각 이후 기록된 동의만 유효하게 인정하는 정책 기준이다.
4. 필수 유형의 최신 기록이 `AGREE`이고 `recorded_at >= effective-from`일 때만 해당 유형을 충족한다.
5. 최신 기록이 `REVOKE`라면 `effective-from` 전후와 관계없이 해당 유형은 미충족이다.
6. 동의 이력은 UPDATE/DELETE 없이 AGREE/REVOKE 이벤트를 계속 추가한다.
7. 로그인 응답의 `agreementRequired`는 Evaluator의 `eligible`을 반전한 힌트이며 접근 제어 근거가 아니다.
8. AgreementGate, 면제 어노테이션, 보호 API의 403 자동 응답은 이번 범위에서 제외한다.
9. API 경로는 `GET /api/v2/agreements/status`, `POST /api/v2/agreements`이며 두 API 모두 일반 인증이 필요하다.
10. 제출 본문의 `content`는 클라이언트가 실제 표시한 원문이며 서버는 이를 그대로 저장한다. `user_id`와 `recorded_at`은 각각 인증 주체와 서버 시각에서 얻는다.
11. `inputContext`는 항목별 선택인 `INDIVIDUAL`과 전체 선택인 `BULK`만 허용한다.
12. 최초 `effective-from`은 두 필수 유형 모두 `2026-08-01T00:00:00`으로 두고 환경변수로 변경할 수 있게 한다.
13. 동일한 `recorded_at` 이벤트가 있으면 더 큰 `id`를 최신 이벤트로 판정한다.

### Success Criteria

1. 인증된 사용자가 동의 상태를 조회하고 동의 또는 철회를 제출할 수 있다.
2. 동의 제출과 상태 조회는 동일한 상태 응답 스키마를 사용한다.
3. 필수 유형별 최신 이벤트가 모두 유효한 AGREE일 때만 `eligible=true`다.
4. AGREE 이후 REVOKE를 제출하면 기존 AGREE 행은 유지되고 `eligible=false`가 된다.
5. `effective-from`을 상향하면 기준일 이전 AGREE만 가진 사용자는 미충족이 된다.
6. 선택 정책은 전체 `eligible` 판정을 바꾸지 않도록 Evaluator가 `required` 설정을 따른다.
7. Apple/Kakao 소셜 로그인 응답에 `agreementRequired`가 포함되고 상태 API와 동일한 Evaluator 결과를 사용한다.
8. 기존 JWT 발급·refresh cookie·로그인 단일 응답 스키마는 유지된다.
9. 보호 API 차단이나 `AGREEMENT_REQUIRED` 403 응답은 발생하지 않는다.
10. build, unit, integration, rule 검증이 모두 통과한다.
11. 상태 응답은 `eligible`과 유형별 `type`, `required`, `agreed`를 반환한다.
12. 제출 요청은 유형별 `type`, `action`, `content`, `inputContext`를 받고 IP와 User-Agent는 서버가 요청 메타데이터에서 기록한다.

### Impact Scope

- `git.environment-variables`: 병합된 V6 `user_agreements` migration과 서브모듈 포인터
- `bottlenote-mono`: 동의 도메인, 저장소 포트/JPA 구현, 정책 설정, Evaluator, 서비스, DTO, 로그인 응답
- `bottlenote-product-api`: 동의 API, API 문서, 로그인 응답 투영
- `bottlenote-test-support`: InMemory 동의 저장소와 테스트 지원
- 테스트: Evaluator 단위 테스트, API·로그인 통합 테스트, RestDocs/OpenAPI 회귀

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 행동

## Tasks

### Task 1: 동의 이력 영속 경로
- Acceptance: V6 스키마와 일치하는 append-only `UserAgreement`가 정적 팩토리로 생성되고, 도메인 포트와 JPA 구현이 사용자·유형별 최신 이벤트를 `recorded_at`, `id` 내림차순 기준으로 한 건씩 반환한다. 운영 코드에는 UPDATE/DELETE 경로가 없다.
- Verification: `./gradlew :bottlenote-mono:compileJava`, 관련 repository 통합 테스트
- Files (advisory): 동의 enum 3개, `UserAgreement`, `UserAgreementRepository`, `JpaUserAgreementRepository`, repository 통합 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: InMemory 동의 저장소
- Acceptance: test-support의 InMemory 구현이 저장 시 기존 이벤트를 덮지 않고 추가하며, 사용자·유형 격리와 `recorded_at`, `id` 최신순 규칙을 JPA 포트와 동일하게 모사한다.
- Verification: `./gradlew :bottlenote-test-support:compileJava`, Fake 동작 테스트
- Files (advisory): `InMemoryUserAgreementRepository`, 관련 테스트
- Depends: Task 1
- Size: S
- Status: [x] done

### Task 3: 정책 기반 Agreement Evaluator
- Acceptance: 유형별 `required`와 `effective-from` 설정을 읽고, 최신 이벤트 없음·REVOKE·기준일 이전 AGREE를 미충족으로 판정한다. 선택 정책은 전체 `eligible`에 영향을 주지 않고 두 필수 유형이 모두 유효한 AGREE일 때만 `eligible=true`다.
- Verification: `./gradlew unit_test --tests '*AgreementEvaluatorTest'`
- Files (advisory): `AgreementPolicyProperties`, 평가 결과 DTO, `AgreementEvaluator`, `application.yml`, Evaluator 단위 테스트
- Depends: Tasks 1-2
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 1-3
- [ ] mono/test-support 컴파일 통과
- [ ] Evaluator 단위 테스트 통과
- [ ] ArchUnit 룰 통과

### Task 4: 동의 조회·제출 서비스
- Acceptance: 인증 사용자 기준 조회와 제출이 동일한 상태 응답을 반환하고, 제출은 유형별 새 이벤트에 표시 원문·서버 시각·입력 맥락·IP·User-Agent를 기록한다. AGREE 뒤 REVOKE를 제출하면 기존 행은 유지되고 최신 판정만 미충족으로 바뀐다.
- Verification: `./gradlew unit_test --tests '*AgreementServiceTest'`
- Files (advisory): 제출 요청 DTO, 상태 응답 DTO, `AgreementService`, 사용자 예외 코드, 서비스 단위 테스트
- Depends: Task 3
- Size: M
- Status: [ ] not done

### Task 5: 인증된 동의 API
- Acceptance: `GET /api/v2/agreements/status`와 `POST /api/v2/agreements`가 일반 인증을 요구하고 동일 응답 스키마를 반환한다. 제출 API는 신뢰된 XFF 기반 IP와 User-Agent를 서비스에 전달하며 다른 보호 API의 동작은 바꾸지 않는다.
- Verification: 동의 Controller 통합 테스트, RestDocs/OpenAPI 문서 테스트
- Files (advisory): `AgreementController`, `AgreementApiDocs`, Controller 통합 테스트, RestDocs 테스트
- Depends: Task 4
- Size: M
- Status: [ ] not done

### Task 6: 로그인 판정 힌트
- Acceptance: AuthService가 기존 JWT 발급 이후 같은 `AgreementEvaluator`로 판정한 `agreementRequired = !eligible`을 AuthResponse에 담는다. Apple/Kakao 모두 같은 경로를 사용하고 기존 JWT·refresh cookie·최초 로그인 판정은 유지된다.
- Verification: `./gradlew unit_test --tests '*AuthServiceTest'`
- Files (advisory): `AuthService`, `AuthResponse`, `AuthServiceTest`
- Depends: Task 3
- Size: S
- Status: [ ] not done

### Checkpoint: after Tasks 4-6
- [ ] mono/product-api 컴파일 통과
- [ ] 동의 API·로그인 단위 및 통합 테스트 통과
- [ ] 보호 API에 Gate/403 변경이 없음을 diff로 확인

### Task 7: 로그인 응답·문서 호환성
- Acceptance: Apple/Kakao 로그인 응답에 `agreementRequired` 불리언이 추가되고 재발급 응답은 기존 nullable 필드 계약을 유지한다. RestDocs와 OpenAPI가 단일 로그인 응답 스키마를 문서화하고 기존 bare-response 규칙을 통과한다.
- Verification: `RestAuthV2ControllerTest`, `OpenApiAuthV2ControllerTest`, `OpenApiDocsIntegrationTest`, `asciidoctor`
- Files (advisory): `OauthResponse`, `AuthV2Controller`, 로그인 RestDocs/OpenAPI 테스트, 공통 OpenAPI 회귀 테스트
- Depends: Task 6
- Size: M
- Status: [ ] not done

## Progress Log

- 2026-08-01: environment-variables PR #8 병합 및 부모 저장소 서브모듈 포인터 커밋 완료.
- 2026-08-01: Task 1 완료. V6와 일치하는 append-only `UserAgreement`, enum 3개, 도메인 repository port와 `JpaUserAgreementRepository`를 추가했다. `recorded_at DESC, id DESC` 최신 조회와 사용자·유형 격리를 포함한 repository 통합 테스트 5개가 통과했고, 전체 Java/Test 컴파일 및 `check_rule_test`가 통과했다. self-review는 Critical 0건, Important 1건(`JpaRepository` 관례와 append-only 경계 명시)을 `@Immutable`, `updatable=false`, 제한된 도메인 포트로 해소했다.
- 2026-08-01: Task 2 완료. test-support에 append-only `InMemoryUserAgreementRepository`를 추가하고 단조 증가 ID와 `recordedAt DESC, id DESC` 최신 조회를 구현했다. append-only·재저장 거부·최신 시각·동률 ID·사용자/유형 격리 단위 테스트 5개와 test-support 컴파일, 전체 `check_rule_test`가 통과했다. self-review는 Critical 0건, Important 1건(동일 객체 재저장 시 기존 이력 ID 훼손 가능성)을 재저장 거부로 해소했다.
