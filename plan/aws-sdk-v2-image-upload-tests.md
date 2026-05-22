# Plan: AWS SDK v2 이미지 업로드 테스트 보강

## Overview
AWS SDK v2 전환 브랜치에서 이미지 업로드 검증을 레이어별로 분리해 보강한다. S3/MinIO 직접 동작은 unit 성격의 Testcontainers 테스트에서 확인하고, 리뷰 API와 ResourceLog 연결은 product-api integration 테스트에서 확인한다.

### Assumptions
- 새 브랜치를 만들지 않고 현재 `codex/aws-sdk-v2-migration` 브랜치에서 작업한다.
- 실제 S3 대신 기존 테스트 관습대로 MinIO Testcontainers를 사용한다.
- Mock 프레임워크는 추가하지 않고 기존 Fake/InMemory/Testcontainers 기반으로 검증한다.
- 리뷰 이미지 정책은 현재 코드의 ResourceLog 기반 상태 전환과 리뷰 이미지 최대 5장 제한을 기준으로 검증한다.

### Success Criteria
- PreSigned URL 발급 후 실제 PUT 업로드와 객체 조회가 AWS SDK v2 클라이언트로 검증된다.
- 리뷰 등록 통합 흐름에서 업로드 이미지가 상세 조회에 노출되고 ResourceLog가 `ACTIVATED`로 전환된다.
- contentType 불일치 업로드, presign 없이 만든 URL, 다른 사용자 URL, 리뷰 이미지 5장/6장 경계 조건이 테스트로 고정된다.
- unit 테스트와 integration 테스트가 각 레이어의 책임에 맞게 분리된다.
- `/verify full` 수준의 전체 검증을 통과한 뒤 커밋 및 푸시한다.

### Impact Scope
- `bottlenote-mono`: `ImageUploadUnitTest`에 MinIO/S3 SDK v2 레벨 테스트 추가.
- `bottlenote-product-api`: `ImageUploadIntegrationTest`에 리뷰 이미지 연결 및 ResourceLog 통합 테스트 추가.
- 스키마, 운영 설정, API 응답 계약 변경은 포함하지 않는다.
