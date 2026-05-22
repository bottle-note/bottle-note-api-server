# Plan: AWS SDK v2 Resource Verify 후속 보강

## Overview
PR #611 후속으로 이미지 presign 이후 리뷰 저장까지의 운영 안전성을 보강한다. Presigned URL 응답 전 `ResourceLog` CREATED 저장을 완료하고, 리뷰 생성/수정 시 전달된 `viewUrl`이 현재 사용자에게 발급된 리소스인지 저장 전에 검증한다.

### Assumptions
- 새 브랜치를 만들지 않고 `codex/aws-sdk-v2-migration` 브랜치에서 이어서 작업한다.
- `git.environment-variables`의 기존 unstaged 변경은 건드리지 않는다.
- Mock 프레임워크는 추가하지 않고 기존 Fake/InMemory/Testcontainers 테스트 관습을 사용한다.
- 우선 적용 범위는 리뷰 생성/수정 이미지 URL이며, Help/Profile/Business/Admin 확장이 가능하도록 공통 검증 서비스로 둔다.
- 운영 안전 기준은 임의 URL이나 타 사용자 URL을 저장하지 않는 쪽으로 둔다.

### Success Criteria
- Presigned URL 응답 직후 CREATED `ResourceLog`가 DB에서 조회된다.
- 리뷰 생성/수정은 저장 전에 이미지 URL의 resourceKey, 로그 존재 여부, 소유자, 저장된 viewUrl, 활성화 가능 상태를 검증한다.
- 타 사용자 viewUrl과 임의 viewUrl은 리뷰 생성/수정 저장 전에 4xx로 거부된다.
- Presign 직후 CREATED 대기 없이 리뷰 등록해도 ResourceLog가 안전하게 ACTIVATED로 전환된다.
- CloudFront URL에 trailing slash가 있어도 viewUrl에 중복 slash가 생기지 않는다.
- `/verify full` 수준의 전체 검증 후 `fix: ...` 커밋을 push한다.

### Impact Scope
- `bottlenote-mono`: ResourceLog 생성 저장 동기화, 공통 ResourceVerifierService 추가, 리뷰 서비스 저장 전 검증 적용, 파일 예외 코드 보강, CloudFront URL normalize.
- `bottlenote-product-api`: ResourceCommand/ImageUpload/ResourceVerifier 단위 테스트와 이미지 업로드 통합 테스트 보강, 리뷰 통합 테스트의 임의 이미지 URL 의존 제거.
- 스키마, API 응답 필드, 의존성 버전 변경은 포함하지 않는다.

## Tasks

### Task 1: ResourceLog 생성 동기화와 CloudFront URL 정규화
- Acceptance: `saveImageResourceCreated`가 비동기 경합 없이 응답 전 저장을 완료한다.
- Acceptance: CloudFront base URL trailing slash가 viewUrl 중복 slash를 만들지 않는다.
- Verification: `./gradlew compileJava compileTestJava`, 관련 unit 테스트.
- Files: `ResourceCommandService.java`, `ImageUploadService.java`, 관련 테스트.
- Size: M
- Status: [x] done

### Task 2: 공통 Resource Verify와 리뷰 저장 전 검증
- Acceptance: viewUrl에서 추출한 resourceKey 기준으로 로그 존재, 사용자 소유, 저장된 viewUrl, 활성화 가능 상태를 저장 전에 검증한다.
- Acceptance: 리뷰 생성/수정에서 임의 URL과 타 사용자 URL이 4xx로 거부된다.
- Verification: 관련 unit/integration 테스트.
- Files: `ResourceVerifierService.java`, `FileExceptionCode.java`, `ReviewService.java`, 관련 테스트.
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] Compile / type-check passes
- [x] Unit tests pass
- [x] Integration tests cover no-await presign-to-review and unsafe URLs

## Progress Log
- 2026-05-22: baseline `./gradlew check_rule_test` 성공(50s).
- 2026-05-22: Task 1 완료. `saveImageResourceCreated`의 `@Async`를 제거해 presign 응답 전 CREATED 저장이 완료되도록 했고, CloudFront trailing slash를 normalize했다.
- 2026-05-22: Task 2 완료. 공통 `ResourceVerifierService`를 추가하고 리뷰 생성/수정 저장 전에 resourceKey, 로그 존재, 소유자, viewUrl, 활성화 가능 상태를 검증하도록 적용했다.
- 2026-05-22: `/verify full` 수준 검증 완료. `integration_test` 1차 실패는 기존 감사 테스트의 presign 없는 이미지 fixture가 새 정책과 충돌한 것이 원인이어서 이미지 없는 요청으로 수정했고, 재실행 통과했다.
