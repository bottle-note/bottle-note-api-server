# Plan: 동의 만료 액션

## Overview

`AgreementAction`에 서버가 기록할 수 있는 만료 상태 `EXPIRED`를 추가한다. 클라이언트 제출 API는 `EXPIRED`를 허용하지 않고 400 오류를 반환하며, 최신 이력이 `EXPIRED`이면 로그인 힌트와 상태 조회에서 `REVOKE`와 동일하게 미동의로 판정한다.

### Assumptions

- `EXPIRED`를 생성하는 배치나 운영 절차는 이번 범위에 포함하지 않는다.
- 상태 조회 응답은 기존 `agreed` boolean 계약을 유지하고 최신 action 필드를 새로 노출하지 않는다.
- action 컬럼은 문자열 enum을 저장할 수 있어 스키마 마이그레이션이 필요하지 않다.
- 로컬에서는 전체 컴파일과 단위 테스트만 실행하고, 통합·문서·규칙·최종 빌드는 main 대상 PR CI로 검증한다.

### Success Criteria

- `AgreementAction.EXPIRED`가 존재한다.
- 제출 요청에 `EXPIRED`가 포함되면 `UNSUPPORTED_AGREEMENT_ACTION` 코드와 함께 400을 반환하고 저장하지 않는다.
- 최신 필수 동의 이력이 `EXPIRED`이면 `eligible=false`, 해당 항목은 `agreed=false`다.
- OpenAPI와 RestDocs가 요청 가능한 action을 `AGREE`, `REVOKE`로 문서화하고 `EXPIRED`가 서버 상태임을 설명한다.

### Impact Scope

- `bottlenote-mono`: enum, 예외 코드, 동의 판정 회귀 테스트
- `bottlenote-product-api`: 요청 경계 검증, API 통합 테스트, OpenAPI 및 RestDocs 계약
- DB 스키마, 상태 응답 필드, 만료 생성 주기는 제외

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- local-verify: 전체 Java/Kotlin 컴파일, 전체 단위 테스트
- ci-verify: main 대상 기존 PR의 통합 테스트, 문서 테스트, 규칙 테스트, 최종 빌드
- stop-conditions: 가정 붕괴, verify 3회 반복 실패, scope 밖 행동 필요

## Tasks

### Task 1: 만료 액션 계약 추가
- Acceptance:
  - `EXPIRED`는 저장 가능한 도메인 action이지만 사용자 제출 API에서는 400으로 거부된다.
  - 최신 `EXPIRED` 이력은 `REVOKE`와 동일하게 미동의 및 힌트 필요 상태로 판정된다.
  - 요청 가능 action과 서버 전용 `EXPIRED`의 구분이 API 문서와 계약 테스트에 반영된다.
- Verification: 전체 Java/Kotlin 컴파일, 전체 단위 테스트, PR CI
- Files (advisory): agreement enum, request DTO, exception code, evaluator/controller/docs/OpenAPI tests
- Depends: 없음
- Size: M
- Status: [x] done

## Progress Log

- 2026-08-02: Opus 5 오케스트레이션 검토 결과, enum 추가만으로는 POST가 `EXPIRED`를 저장하므로 요청 경계 차단이 필요함을 확인했다. 기존 evaluator는 `AGREE`만 유효로 보아 운영 로직 변경 없이 회귀 테스트로 요구사항을 고정한다.
- 2026-08-02: `EXPIRED` enum과 요청 거부 오류를 추가하고, evaluator·로그인 힌트·API 저장 방지·OpenAPI·RestDocs 계약 테스트를 반영했다. 전체 Java/Kotlin 컴파일과 단위 테스트 555개가 통과했다.
