# Plan: 마케팅 선택 동의 추가

## Overview

기존 사용자 동의 도메인에 마케팅 정보 수신 동의를 선택 항목으로 추가한다. 동의 상태 응답은 각 유형의 필수 여부와 현재 동의 여부를 함께 제공하고, 전체 동의 자격과 로그인 동의 필요 힌트는 필수 항목만 기준으로 판정한다.

### Assumptions

1. 대상은 product-api의 기존 사용자 동의 조회·제출 및 로그인 힌트 흐름이다.
2. 새 동의 유형의 식별자는 `MARKETING`, 설명은 `마케팅 정보 수신 동의`, 필수 여부는 `false`다.
3. `TERMS_OF_SERVICE`, `PRIVACY_COLLECTION_USE`의 필수 여부는 `true`다.
4. 마케팅 동의와 철회는 기존 append-only 동의 이력과 기존 제출 API를 사용한다.
5. 상태 API는 마케팅 항목도 `required=false`와 최신 `agreed` 상태를 포함해 반환한다.
6. 전체 `eligible`과 로그인 `agreementRequired`는 필수 동의 유형만 기준으로 계산한다.
7. 실제 마케팅 메시지 발송과 채널별 수신 설정은 범위에서 제외한다.
8. 동의 유형은 문자열 컬럼에 저장되므로 DB 스키마 마이그레이션은 필요하지 않다.

### Success Criteria

1. 동의 제출 API가 `MARKETING`의 `AGREE`와 `REVOKE`를 기존 방식으로 기록한다.
2. 상태 API가 `MARKETING` 항목에 `required=false`와 최신 동의 상태를 반환한다.
3. 마케팅 미동의 상태에서도 기존 필수 동의 두 유형이 모두 동의면 `eligible=true`다.
4. 마케팅 미동의 상태에서도 기존 필수 동의 두 유형이 모두 동의면 로그인 응답의 `agreementRequired=false`다.
5. 기존 필수 동의 중 하나라도 미동의면 마케팅 상태와 관계없이 `eligible=false`, `agreementRequired=true`다.
6. OpenAPI 문서가 `MARKETING`을 제출 가능한 동의 유형으로 노출한다.
7. 로컬 Java/Kotlin compile과 전체 unit test가 통과한다.
8. main 대상 PR의 CI에서 rule, product/admin integration, 최종 build를 포함한 전체 파이프라인이 통과한다.

### Impact Scope

- `bottlenote-mono`: 동의 유형의 필수 여부와 동의 자격 판정
- `bottlenote-product-api`: 동의 제출 API의 OpenAPI 계약과 상태 응답 회귀
- 테스트: 동의 평가·서비스·로그인 힌트 단위 테스트 및 API 문서 계약
- DB migration, admin-api 기능, batch, 실제 마케팅 발송 로직은 영향 범위에서 제외

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- local-verify: Java/Kotlin compile, unit test only
- ci-verify: main 대상 PR의 `ci_pipeline.yml` 완료 확인
- stop-conditions: 가정 붕괴, verify 3회 실패, scope 밖 행동

## Tasks

### Task 1: 선택 동의 판정 경로
- Acceptance: `MARKETING`은 `required=false`로 평가되고 최신 동의 상태는 항목에 노출되지만, 필수 유형이 모두 동의면 마케팅 상태와 무관하게 `eligible=true`, 로그인 `agreementRequired=false`가 된다.
- Verification: 전체 Java/Kotlin compile, `./gradlew unit_test`
- Files (advisory): `AgreementType`, `AgreementEvaluator`, evaluator/service/auth 단위 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: 마케팅 동의 API 계약
- Acceptance: 기존 제출 API가 `MARKETING`을 허용하고 OpenAPI에 해당 값을 노출하며, 상태 응답은 마케팅 항목의 `required=false`와 최신 `agreed` 값을 반환한다.
- Verification: 로컬 compile·unit test, main 대상 PR CI의 product integration·RestDocs/OpenAPI 결과
- Files (advisory): `AgreementSubmitRequest`, 동의 Controller 통합 테스트, RestDocs/OpenAPI 계약 테스트
- Depends: Task 1
- Size: M
- Status: [ ] not done

## Progress Log

- 2026-08-02: define 승인. 마케팅을 선택 동의로 추가하고 필수 동의만 전체 자격과 로그인 힌트에 반영하기로 확정했다. 로컬 검증은 compile과 unit test로 제한하며, rule·integration·최종 build는 main 대상 PR CI에서 확인한다.
- 2026-08-02: plan 완료. 선택 동의 판정 경로와 API 계약의 수직 슬라이스 2개로 분해했으며 Task 2는 Task 1에 의존한다.
- 2026-08-02: Task 1 완료. 동의 유형에 필수 여부를 추가하고 마케팅을 선택 유형으로 정의했다. Evaluator는 필수 항목만 전체 자격에 반영하며 상태 항목에는 유형별 필수 여부를 노출한다. 관련 compile과 evaluator/service/auth focused unit test가 통과했다.
