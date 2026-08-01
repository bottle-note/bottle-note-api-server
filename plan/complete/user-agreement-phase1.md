# Plan: 사용자 동의 Phase 1

Status: Completed
Completion Date: 2026-08-01

## Overview

인증된 사용자의 약관 동의 이력을 append-only로 기록하고, 유형별 가장 큰 이력 ID의 action으로 동의 충족 여부를 하나의 Evaluator가 판정한다. Product API는 동의 상태 조회와 제출 기능을 제공하며, 소셜 로그인 응답에는 같은 판정 결과로 계산한 `agreementRequired` 힌트를 포함한다. 이번 범위에서는 보호 API를 차단하거나 403 응답을 자동 생성하지 않는다.

### Assumptions

1. 대상은 product-api의 사용자 인증 흐름이며 admin-api용 동의 API는 추가하지 않는다.
2. 필수 동의 유형은 `TERMS_OF_SERVICE`, `PRIVACY_COLLECTION_USE` 두 종류다.
3. 유형별 최신 이력은 DB가 부여한 더 큰 `id`로 판정한다.
4. 최신 기록이 `AGREE`일 때만 해당 유형을 충족한다.
5. 이력이 없거나 최신 기록이 `REVOKE`이면 해당 유형은 미충족이다.
6. 동의 이력은 UPDATE/DELETE 없이 AGREE/REVOKE 이벤트를 계속 추가한다.
7. 로그인 응답의 `agreementRequired`는 Evaluator의 `eligible`을 반전한 힌트이며 접근 제어 근거가 아니다.
8. AgreementGate, 면제 어노테이션, 보호 API의 403 자동 응답은 이번 범위에서 제외한다.
9. API 경로는 `GET /api/v2/agreements/status`, `POST /api/v2/agreements`이며 두 API 모두 일반 인증이 필요하다.
10. 제출 본문의 `content`는 클라이언트가 실제 표시한 원문이며 서버는 이를 그대로 저장한다. `user_id`와 `recorded_at`은 각각 인증 주체와 서버 시각에서 얻는다.
11. `inputContext`는 항목별 선택인 `INDIVIDUAL`과 전체 선택인 `BULK`만 허용한다.
12. 두 동의 유형은 모두 필수이며 별도의 YAML 정책이나 기간 계산을 두지 않는다.

### Success Criteria

1. 인증된 사용자가 동의 상태를 조회하고 동의 또는 철회를 제출할 수 있다.
2. 동의 제출과 상태 조회는 동일한 상태 응답 스키마를 사용한다.
3. 필수 유형별 가장 큰 이력 ID의 action이 모두 `AGREE`일 때만 `eligible=true`다.
4. AGREE 이후 REVOKE를 제출하면 기존 AGREE 행은 유지되고 `eligible=false`가 된다.
5. 기록 시각과 관계없이 DB에서 가장 나중에 추가된 이벤트의 action으로 현재 상태를 판정한다.
6. 두 동의 유형을 모두 충족해야 `eligible=true`다.
7. Apple/Kakao 소셜 로그인 응답에 `agreementRequired`가 포함되고 상태 API와 동일한 Evaluator 결과를 사용한다.
8. 기존 JWT 발급·refresh cookie·로그인 단일 응답 스키마는 유지된다.
9. 보호 API 차단이나 `AGREEMENT_REQUIRED` 403 응답은 발생하지 않는다.
10. build, unit, integration, rule 검증이 모두 통과한다.
11. 상태 응답은 `eligible`과 유형별 `type`, `required`, `agreed`를 반환한다.
12. 제출 요청은 유형별 `type`, `action`, `content`, `inputContext`를 받고 IP와 User-Agent는 서버가 요청 메타데이터에서 기록한다.

### Impact Scope

- `git.environment-variables`: 병합된 V6 `user_agreements` migration과 서브모듈 포인터
- `bottlenote-mono`: 동의 도메인, 저장소 포트/JPA 구현, Evaluator, 서비스, DTO, 로그인 응답
- `bottlenote-product-api`: 동의 API, API 문서, 로그인 응답 투영
- `bottlenote-test-support`: InMemory 동의 저장소와 테스트 지원
- 테스트: Evaluator 단위 테스트, API·로그인 통합 테스트, RestDocs/OpenAPI 회귀

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 행동

## Tasks

### Task 1: 동의 이력 영속 경로
- Acceptance: V6 스키마와 일치하는 append-only `UserAgreement`가 정적 팩토리로 생성되고, 도메인 포트와 JPA 구현이 사용자·유형별 가장 큰 `id`의 이벤트를 한 건씩 반환한다. 운영 코드에는 UPDATE/DELETE 경로가 없다.
- Verification: `./gradlew :bottlenote-mono:compileJava`, 관련 repository 통합 테스트
- Files (advisory): 동의 enum 3개, `UserAgreement`, `UserAgreementRepository`, `JpaUserAgreementRepository`, repository 통합 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: InMemory 동의 저장소
- Acceptance: test-support의 InMemory 구현이 저장 시 기존 이벤트를 덮지 않고 추가하며, 사용자·유형 격리와 가장 큰 `id`의 이벤트를 최신으로 조회하는 규칙을 JPA 포트와 동일하게 모사한다.
- Verification: `./gradlew :bottlenote-test-support:compileJava`, Fake 동작 테스트
- Files (advisory): `InMemoryUserAgreementRepository`, 관련 테스트
- Depends: Task 1
- Size: S
- Status: [x] done

### Task 3: Agreement Evaluator
- Acceptance: TERMS_OF_SERVICE와 PRIVACY_COLLECTION_USE를 모두 필수 유형으로 응답하고, 유형별 가장 큰 `id`의 이벤트가 없거나 REVOKE면 미동의, AGREE면 동의로 판정한다. 두 유형이 모두 동의일 때만 `eligible=true`다.
- Verification: `./gradlew unit_test --tests '*AgreementEvaluatorTest'`
- Files (advisory): 평가 결과 DTO, `AgreementEvaluator`, Evaluator 단위 테스트
- Depends: Tasks 1-2
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-3
- [x] mono/test-support 컴파일 통과
- [x] Evaluator 단위 테스트 통과
- [x] ArchUnit 룰 통과

### Task 4: 동의 조회·제출 서비스
- Acceptance: 인증 사용자 기준 조회와 제출이 동일한 상태 응답을 반환하고, 제출은 유형별 새 이벤트에 표시 원문·서버 시각·입력 맥락·IP·User-Agent를 기록한다. AGREE 뒤 REVOKE를 제출하면 기존 행은 유지되고 최신 판정만 미충족으로 바뀐다.
- Verification: `./gradlew unit_test --tests '*AgreementServiceTest'`
- Files (advisory): 제출 요청 DTO, 상태 응답 DTO, `AgreementService`, 사용자 예외 코드, 서비스 단위 테스트
- Depends: Task 3
- Size: M
- Status: [x] done

### Task 5: 인증된 동의 API
- Acceptance: `GET /api/v2/agreements/status`와 `POST /api/v2/agreements`가 일반 인증을 요구하고 동일 응답 스키마를 반환한다. 제출 API는 신뢰된 XFF 기반 IP와 User-Agent를 서비스에 전달하며 다른 보호 API의 동작은 바꾸지 않는다.
- Verification: 동의 Controller 통합 테스트, RestDocs/OpenAPI 문서 테스트
- Files (advisory): `AgreementController`, `AgreementApiDocs`, Controller 통합 테스트, RestDocs 테스트
- Depends: Task 4
- Size: M
- Status: [x] done

### Task 6: 로그인 판정 힌트
- Acceptance: AuthService가 기존 JWT 발급 이후 같은 `AgreementEvaluator`로 판정한 `agreementRequired = !eligible`을 AuthResponse에 담는다. Apple/Kakao 모두 같은 경로를 사용하고 기존 JWT·refresh cookie·최초 로그인 판정은 유지된다.
- Verification: `./gradlew unit_test --tests '*AuthServiceTest'`
- Files (advisory): `AuthService`, `AuthResponse`, `AuthServiceTest`
- Depends: Task 3
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 4-6
- [x] mono/product-api 컴파일 통과
- [x] 동의 API·로그인 단위 및 통합 테스트 통과
- [x] 보호 API에 Gate/403 변경이 없음을 diff로 확인

### Task 7: 로그인 응답·문서 호환성
- Acceptance: Apple/Kakao 로그인 응답에 `agreementRequired` 불리언이 추가되고 재발급 응답은 기존 nullable 필드 계약을 유지한다. RestDocs와 OpenAPI가 단일 로그인 응답 스키마를 문서화하고 기존 bare-response 규칙을 통과한다.
- Verification: `RestAuthV2ControllerTest`, `OpenApiAuthV2ControllerTest`, `OpenApiDocsIntegrationTest`, `asciidoctor`
- Files (advisory): `OauthResponse`, `AuthV2Controller`, 로그인 RestDocs/OpenAPI 테스트, 공통 OpenAPI 회귀 테스트
- Depends: Task 6
- Size: M
- Status: [x] done

### Task 8: 동의 상태 판정 단순화
- Acceptance: YAML 정책과 `AgreementPolicyProperties`를 제거하고, 유형별 가장 큰 이력 ID의 action만으로 현재 동의 상태를 판정한다. 이력 없음과 최신 REVOKE는 미동의, 최신 AGREE는 동의다.
- Verification: 전체 Java/Kotlin compile, Agreement Evaluator/Service/InMemory/Auth focused unit, 전체 rule. integration/admin integration은 Task 8 작업 범위에서 실행하지 않는다.
- Files (advisory): 정책 설정·테스트, Evaluator, repository port/JPA/InMemory, 관련 테스트, application.yml
- Depends: Task 7
- Size: M
- Status: [x] done

## Progress Log

- 2026-08-01: environment-variables PR #8 병합 및 부모 저장소 서브모듈 포인터 커밋 완료.
- 2026-08-01: Task 1 완료. V6와 일치하는 append-only `UserAgreement`, enum 3개, 도메인 repository port와 `JpaUserAgreementRepository`를 추가했다. `recorded_at DESC, id DESC` 최신 조회와 사용자·유형 격리를 포함한 repository 통합 테스트 5개가 통과했고, 전체 Java/Test 컴파일 및 `check_rule_test`가 통과했다. self-review는 Critical 0건, Important 1건(`JpaRepository` 관례와 append-only 경계 명시)을 `@Immutable`, `updatable=false`, 제한된 도메인 포트로 해소했다.
- 2026-08-01: Task 2 완료. test-support에 append-only `InMemoryUserAgreementRepository`를 추가하고 단조 증가 ID와 `recordedAt DESC, id DESC` 최신 조회를 구현했다. append-only·재저장 거부·최신 시각·동률 ID·사용자/유형 격리 단위 테스트 5개와 test-support 컴파일, 전체 `check_rule_test`가 통과했다. self-review는 Critical 0건, Important 1건(동일 객체 재저장 시 기존 이력 ID 훼손 가능성)을 재저장 거부로 해소했다.
- 2026-08-01: Task 3 완료. 두 필수 유형의 `required`와 `effective-from` 환경 설정, 최신 이벤트 기반 `AgreementEvaluator`, status API와 login hint가 공유할 불변 평가 결과를 추가했다. 이력 없음·유효 동의·철회·기준 시각 이전·재동의·동률 ID·선택 정책 7개 분기와 설정 기본값·override 바인딩 2개를 검증했고, 전체 unit/compile 및 `check_rule_test`가 통과했다. self-review는 Critical 0건, Important 2건(`@Service` 규칙 부적합, 응답 DTO 접미사 부적합)을 각각 정책 `@Component`와 domain record로 해소했다.
- 2026-08-01: Task 4 완료. 인증 사용자 기준 상태 조회와 append-only 제출 서비스를 추가하고, 항목별 AGREE/REVOKE·원문·INDIVIDUAL/BULK 맥락·IP·User-Agent 및 주입된 `Clock`의 서버 시각을 저장한 뒤 같은 Evaluator 결과를 반환하도록 했다. 서비스 단위 테스트 7개와 전체 unit 538개, Java/Kotlin 컴파일, `check_rule_test`, 테스트 제외 전체 build가 통과했다. self-review는 Critical 0건, Important 2건(메서드별 트랜잭션 명시, 허용 DTO 패키지 위반)을 각각 읽기 트랜잭션 선언과 domain submission record로 해소했다.
- 2026-08-01: Task 5 완료. 일반 인증이 필요한 `GET /api/v2/agreements/status`와 `POST /api/v2/agreements`를 추가하고, 요청 `agreements[]`를 `AgreementSubmission`으로 변환해 신뢰된 XFF 기반 IP와 User-Agent를 서비스에 전달했다. Controller 통합 테스트 7개, OpenAPI 회귀 테스트 14개, RestDocs 테스트 2개와 product/root ArchUnit 검증이 통과했으며 두 API의 `eligible`, `items[{type, required, agreed}]` exact field와 제출 요청 exact field를 고정했다. self-review는 Critical 0건, Important 4건(인증 주체 주입 불일치, 문서 테스트 Mock 사용, OpenAPI nested Item 충돌, 알 수 없는 type 예외 코드)을 기존 `SecurityContextUtil`, InMemory 기반 실제 서비스, 고유 schema 이름, `INVALID_AGREEMENT_TYPE` 매핑으로 모두 해소했다.
- 2026-08-01: Task 6 완료. Apple/Kakao가 공유하는 `AuthService#getAuthResult`에 `AgreementEvaluator` 판정을 연결해 `agreementRequired = !eligible`을 내부 `AuthResponse`에 담았고, 미충족이어도 기존 JWT 발급·refresh token 저장·로그인 성공 흐름을 유지했다. InMemory 동의 저장소를 사용한 AuthService 단위 테스트 10개(신규 2개 포함), 전체 unit 540개, Auth RestDocs 회귀 6개, Java/Kotlin 컴파일, `check_rule_test`, 테스트 제외 전체 build가 통과했다. 현재 코드에 agent 로그인 경로는 없음을 확인했고 Gate/403과 `OauthResponse` 투영은 변경하지 않았으며, self-review Important 1건(Apple/Kakao refresh cookie 값 회귀 공백)을 두 RestDocs 테스트의 cookie 값 assertion으로 해소했다.
- 2026-08-01: Task 7 완료. `OauthResponse`에 nullable `agreementRequired`를 추가하고 Apple/Kakao 로그인에서 `AuthResponse` 힌트를 투영하되, reissue factory는 `isFirstLogin`·`nickname`·`agreementRequired`를 `null`로 유지했다. RestAuthV2ControllerTest 6개, 기존 OpenApiAuthV2ControllerTest 1개, 실제 Spring context OpenApiDocsIntegrationTest 7개, 전체 unit 540개와 Java/Kotlin 컴파일, `check_rule_test`, 테스트 제외 전체 build, `asciidoctor`가 통과했다. Apple/Kakao는 `accessToken`, `isFirstLogin`, `nickname`, `agreementRequired` exact field와 필수 동의 필요 의미를 문서화했고, bare response 3개·refresh cookie·reissue nullable 형태를 유지했으며 Gate/403·SecurityConfig 변경은 없다. self-review는 Critical 0건, Important 1건(신규 Mockito OpenAPI 테스트가 테스트 더블 정책에 맞지 않음)을 해당 추가본 제거와 실제 context 통합 테스트로 해소했다.
- 2026-08-01: 최종 self-review 아키텍처 수정 완료. user `AuthService`가 agreement `AgreementEvaluator`를 직접 참조하던 레이어 표준 6·7 위반을 `AgreementFacade#isEligible`과 `@FacadeService DefaultAgreementFacade`로 분리했고, 구현체만 기존 Evaluator에 위임하도록 했다. AuthService 단위 테스트는 Mockito 추가 없이 InMemory 동의 저장소→실제 Evaluator→실제 Default Facade 조합을 사용했고 focused 10개, 전체 unit 540개, rule 63개, Java/Test 컴파일, 테스트 제외 전체 build가 통과했다. 기존 `agreementRequired = !eligible`, 동의 API, Gate/403 제외 범위는 변경하지 않았으며 self-review의 Critical 0건, Important 1건(타 도메인 Service 직접 참조)을 해소했다.
- 2026-08-01: 최종 L3 검증 완료. 전체 unit 540개, integration 283개, admin integration 210개, rule 63개, RestDocs 138개가 모두 실패·스킵 없이 통과했고 Java/Kotlin 컴파일, 패키지 build, `asciidoctor`도 통과했다. `origin/main`은 현재 브랜치의 조상이며 서브모듈 포인터 `c605215`는 병합된 environment-variables `origin/main`에 포함됨을 확인했다.
- 2026-08-01: Task 8 완료. `AgreementPolicyProperties`와 바인딩 테스트, `application.yml`의 `agreement.policy`를 제거하고, repository 파생 조회와 InMemory 구현을 `id DESC` 한 건 조회로 통일했다. Evaluator는 두 유형을 모두 필수로 응답하며 최신 action이 `AGREE`인 경우에만 `agreed=true`로 판정하고, `recordedAt`은 감사 데이터로만 보존한다. 리뷰 지적에 따라 V6 최신 조회 인덱스를 `(user_id, agreement_type, id)`로 맞추고 Task 1~3 Acceptance의 과거 정책 표현을 최종 규칙으로 정리해 리뷰를 해소했다. 최종 빠른 검증에서 전체 Java/Kotlin compile, 전체 unit 535개, rule 63개가 실패·스킵 없이 통과했고 `git diff --check`도 통과했으며, 지시된 범위에 따라 integration/admin integration은 실행하지 않았다.
