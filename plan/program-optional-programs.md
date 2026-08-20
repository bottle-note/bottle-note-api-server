# PROGRAM 프로그램 목록 선택 입력 ADR

## Context

workspace #405는 Admin 큐레이션 `PROGRAM` 등록·수정에서 프로그램 일정이 0건인 행사도 저장할 수 있어야 한다. 현재 `ProgramRequest`와 `ProgramResponse`는 루트 `required`에 `programs`를 포함하고 `programs.minItems: 1`도 선언하므로, 키 누락과 빈 배열을 모두 거절한다.

기존 선택 배열인 `programTags`, `programs[].whiskies`는 `minItems: 0`을 명시하지 않고 `minItems` 자체를 생략한다. 이전 #367 구현도 Request/Response 계약을 함께 맞췄다.

## Decision

- `ProgramRequest`와 `ProgramResponse`의 루트 `required`에서 `programs`를 제거한다.
- Request/Response의 `programs.minItems`를 삭제한다. `minItems: 0`은 추가하지 않는다.
- `programs.maxItems: 20`과 각 프로그램 항목의 기존 필수값 `name`, `type`, `description`은 유지한다.
- `programs` 키 누락과 빈 배열 `[]`을 모두 허용한다.
- 프로그램 항목이 존재하면 기존 항목 검증을 그대로 적용한다.
- 리소스 버전은 `3.0.2`에서 `3.0.3`으로 올린다.

## Consequences

- Admin은 일정이 없는 `PROGRAM`을 누락 또는 빈 배열 형태로 저장할 수 있다.
- Product 상세·feed 검증도 같은 선택 계약을 사용하므로 저장 이후 materialize 경로가 불일치하지 않는다.
- 기존 payload 변환, DB migration, 기동 시 payload 정리, Product/Admin Java 운영 로직 변경은 하지 않는다.
- FE에서 빈 영역을 숨기는 렌더링 정책은 이 저장소 변경 범위가 아니다.

## Acceptance Criteria

- Request/Response 모두 루트 `required`에 `programs`가 없다.
- Request/Response 모두 `programs`에 `minItems`가 없다.
- `programs` 누락 payload와 `programs: []` payload가 Request/Response validator를 통과한다.
- 유효한 프로그램 항목을 포함한 기존 payload가 계속 통과한다.
- 필수 항목이 빠진 프로그램 항목은 계속 거절된다.
- `PROGRAM` 리소스 버전이 `3.0.3`이다.

## Files

- Modify: `bottlenote-mono/src/main/resources/openapi/curation/program.json`
- Modify: `bottlenote-mono/src/test/java/app/bottlenote/curation/service/CurationProgramSpecContractTest.java`
- Create: `plan/program-optional-programs.md`

## Verification

로컬 머신 제약상 Gradle 테스트는 직접 실행하지 않고 GitHub Actions CI로 검증한다. 구현 단계에서는 JSON 파싱, 변경 계약의 정적 확인, `git diff --check`를 수행한다.

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- implementation: Luna subagent
- final-review/commit/push/pr: agent-kimglen
- stop-conditions: 가정 붕괴, 범위 밖 변경 필요, 정적 검증 실패 미해결

## Related

- bottle-note/workspace#405
- bottle-note/workspace#367
- bottle-note/bottle-note-api-server#694
