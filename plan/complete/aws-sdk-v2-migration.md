================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-05-22 (커밋 07273b10)

** Core Achievements **
- S3 연동을 AWS SDK v1에서 v2(S3Client/S3Presigner)로 전환, v1 의존성 제거 (2026-06-12 실측: 레포 전체 com.amazonaws import 및 aws-java-sdk 의존 0건)
- Presigned URL API 외부 동작 유지, MinIO Testcontainers 테스트 v2 기반 전환

** Key Components **
- bottlenote-mono의 ImageUploadService 및 S3Client/S3Presigner 빈 설정
- gradle/libs.versions.toml: aws-sdk-bom + software.amazon.awssdk:s3
- 후속 검증은 plan/complete/aws-sdk-v2-resource-verify-followup.md 별도 완료
================================================================================

# Plan: AWS SDK Java v2 전환

## Overview

현재 프로젝트는 S3 Presigned URL 생성과 MinIO 기반 테스트에서 AWS SDK Java v1
`com.amazonaws:aws-java-sdk-s3`를 사용한다. 이 작업의 목표는 S3 연동을 AWS SDK
Java v2로 전환하여 v1 의존성을 제거하고, product/admin 파일 업로드 API의 외부
동작을 유지하는 것이다.

전환 대상은 구현 승인 이후 `bottlenote-mono`의 S3 설정과 `ImageUploadService`,
product/admin 테스트 인프라, Gradle version catalog 및 각 모듈 테스트 의존성이다.
이번 `/define` 단계에서는 구현하지 않고 영향 범위와 성공 기준만 확정한다.

### Assumptions

- S3 Presigned URL API의 HTTP 응답 구조는 유지한다.
- 업로드 방식은 기존과 동일하게 HTTP `PUT` Presigned URL을 사용한다.
- `amazon.aws.*` 설정 키는 배포 환경 영향 최소화를 위해 기본적으로 유지한다.
- CloudFront view URL 생성 방식은 AWS SDK 전환 범위가 아니므로 변경하지 않는다.
- MinIO/Testcontainers 테스트는 유지하고, v2 `S3Client` 및 `S3Presigner` 기반으로 바꾼다.
- `git.environment-variables` 서브모듈 포인터 변경은 사용자/환경 변경으로 보고 건드리지 않는다.
- 작업 3번 `batch component scan 축소`와 무관한 batch component scan 변경은 하지 않는다.
- AWS SDK v2 의존성은 AWS 공식 문서의 권장 방식대로 BOM + `software.amazon.awssdk:s3` 구성을 우선 검토한다.

### Success Criteria

- `gradle/libs.versions.toml`에서 v1 `com.amazonaws:aws-java-sdk-s3` 의존성이 제거되고 v2 S3 의존성으로 대체된다.
- main/test 소스에서 `com.amazonaws.*` S3 관련 import가 제거된다.
- `AwsS3Config`가 v2 `S3Client`와 Presigned URL 생성에 필요한 v2 `S3Presigner`를 제공한다.
- `ImageUploadService.generatePreSignUrl()`이 v2 `S3Presigner.presignPutObject()`로 동일한 만료 시간과 content type 의미를 유지한다.
- MinIO 기반 테스트가 v2 클라이언트로 버킷 생성, 객체 존재 확인, 객체 조회를 수행한다.
- product/admin Presigned URL API의 응답 필드 `bucketName`, `expiryTime`, `uploadSize`, `imageUploadInfo[].viewUrl`, `imageUploadInfo[].uploadUrl`이 유지된다.
- `/verify full` 또는 승인된 동일 수준 검증에서 compile, rule, unit, integration, admin integration, asciidoctor가 통과한다.

### Impact Scope

- Build:
  - `gradle/libs.versions.toml`
  - `bottlenote-mono/build.gradle`
  - `bottlenote-product-api/build.gradle`
  - `bottlenote-admin-api/build.gradle.kts`
- Main code:
  - `bottlenote-mono/src/main/java/app/bottlenote/global/config/AwsS3Config.java`
  - `bottlenote-mono/src/main/java/app/bottlenote/common/file/service/ImageUploadService.java`
  - `bottlenote-mono/src/main/java/app/bottlenote/global/exception/handler/GlobalExceptionHandler.java`
  - `bottlenote-mono/src/main/java/app/bottlenote/global/service/converter/AdminRoleConverter.java`
  - `bottlenote-mono/src/main/java/app/bottlenote/global/service/converter/JsonArrayConverter.java`
- Tests and fixtures:
  - `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`
  - `bottlenote-mono/src/test/java/app/bottlenote/common/file/ImageUploadUnitTest.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/ImageUploadServiceTest.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/MinioContainerLoadingTest.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/fixture/FakeAmazonS3.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/fixture/AbstractFakeAmazonS3.java`
  - `bottlenote-admin-api/src/test/kotlin/app/integration/file/AdminImageUploadIntegrationTest.kt`
- Persistence:
  - DB schema 변경 없음.
- API/docs:
  - Presigned URL API 계약 변경 없음.
  - REST Docs 산출물은 동작 유지 검증 대상이지만 문서 구조 변경은 목표가 아니다.
- External behavior:
  - 실제 S3 업로드용 Presigned URL 생성 방식은 유지한다.
  - MinIO 테스트에서는 v2 S3-compatible endpoint 설정과 path-style 접근을 검증한다.

### Approval Gate

이 정의는 구현 전 승인용이다. 승인 후 다음 단계에서 `/plan`으로 작업을 분해하고,
그 뒤 별도 구현 단계에서만 코드를 수정한다.

## Tasks

### Task 1: v2 의존성 기반 도입
- Acceptance: AWS SDK v2 BOM/S3 의존성이 version catalog와 `bottlenote-mono`에 추가된다.
- Acceptance: `AwsS3Config`가 기존 `amazon.aws.*` 설정 키로 v2 `S3Client`와 `S3Presigner` 빈을 생성한다.
- Acceptance: v1 유틸/예외 타입에 의존한 main code가 v2 전환을 막지 않도록 정리된다.
- Verification: `./gradlew compileJava`
- Files: `gradle/libs.versions.toml`, `bottlenote-mono/build.gradle`, `bottlenote-mono/src/main/java/app/bottlenote/global/config/AwsS3Config.java`, `bottlenote-mono/src/main/java/app/bottlenote/global/exception/handler/GlobalExceptionHandler.java`, `bottlenote-mono/src/main/java/app/bottlenote/global/service/converter/AdminRoleConverter.java`, `bottlenote-mono/src/main/java/app/bottlenote/global/service/converter/JsonArrayConverter.java`
- Size: M
- Status: [x] done

### Task 2: Presigned URL 서비스 전환
- Acceptance: `ImageUploadService.generatePreSignUrl()`이 v2 `S3Presigner.presignPutObject()`를 사용한다.
- Acceptance: 만료 시간 5분, HTTP `PUT`, content type 지정 의미가 유지된다.
- Acceptance: product service 단위 테스트가 v2 전용 fake/stub으로 동일 URL 생성 계약을 검증한다.
- Verification: `./gradlew :bottlenote-product-api:test --tests '*ImageUploadServiceTest'`
- Files: `bottlenote-mono/src/main/java/app/bottlenote/common/file/service/ImageUploadService.java`, `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/ImageUploadServiceTest.java`, `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/fixture/FakeAmazonS3.java`, `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/fixture/AbstractFakeAmazonS3.java`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [ ] `./gradlew compileJava compileTestJava`
- [ ] `./gradlew unit_test`
- [ ] `rg -n "com\\.amazonaws" bottlenote-mono/src/main/java bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload`

### Task 3: MinIO 공통 테스트 인프라 전환
- Acceptance: `TestContainersConfig`가 MinIO용 v2 `S3Client`/`S3Presigner`를 제공한다.
- Acceptance: MinIO endpoint override와 path-style 설정이 v2 방식으로 적용된다.
- Acceptance: mono/product MinIO 테스트가 v2 클라이언트로 버킷 존재와 업로드 가능 여부를 검증한다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*ImageUploadUnitTest'` and `./gradlew :bottlenote-product-api:integration_test --tests '*MinioContainerLoadingTest'`
- Files: `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`, `bottlenote-mono/src/test/java/app/bottlenote/common/file/ImageUploadUnitTest.java`, `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/MinioContainerLoadingTest.java`
- Size: S
- Status: [x] done

### Task 4: Admin 업로드 통합 테스트 전환
- Acceptance: admin 통합 테스트가 v2 `S3Client`로 객체 존재와 객체 본문을 검증한다.
- Acceptance: admin Presigned URL API 응답 구조와 인증 동작이 유지된다.
- Acceptance: admin 테스트 의존성이 v2 S3 의존성 기준으로 정리된다.
- Verification: `./gradlew admin_integration_test --tests '*AdminImageUploadIntegrationTest'`
- Files: `bottlenote-admin-api/src/test/kotlin/app/integration/file/AdminImageUploadIntegrationTest.kt`, `bottlenote-admin-api/build.gradle.kts`
- Size: S
- Status: [x] done

### Task 5: v1 의존성 제거 검증
- Acceptance: `com.amazonaws:aws-java-sdk-s3` 및 `libs.aws.s3`의 v1 좌표가 제거된다.
- Acceptance: main/test 소스에 S3 관련 `com.amazonaws.*` import가 남지 않는다.
- Acceptance: product/admin/mono 빌드 의존성이 v2 S3 의존성만 사용한다.
- Verification: `rg -n "com\\.amazonaws|aws-java-sdk-s3|libs\\.aws\\.s3" gradle/libs.versions.toml bottlenote-*`
- Files: `gradle/libs.versions.toml`, `bottlenote-mono/build.gradle`, `bottlenote-product-api/build.gradle`, `bottlenote-admin-api/build.gradle.kts`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 3-5
- [ ] `./gradlew compileJava compileTestJava`
- [ ] `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`
- [ ] `./gradlew integration_test`
- [ ] `./gradlew admin_integration_test`

### Task 6: 전체 검증
- Acceptance: `/verify full` 기준 compile, rule, unit, build, integration, admin integration, asciidoctor가 통과한다.
- Acceptance: Presigned URL 관련 product/admin REST Docs 생성이 실패하지 않는다.
- Acceptance: `git.environment-variables`와 batch scan 작업 산출물을 변경하지 않는다.
- Verification: `./gradlew compileJava compileTestJava`, `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`, `./gradlew check_rule_test`, `./gradlew unit_test`, `./gradlew build -x test -x asciidoctor --build-cache --parallel`, `./gradlew integration_test`, `./gradlew admin_integration_test`, `./gradlew asciidoctor`
- Files: no new implementation files expected; verification only
- Size: S
- Status: [ ] pending master full verify

## Progress Log

- 2026-05-22: Task 1 완료. AWS SDK v2 BOM/S3 의존성으로 전환하고, v1 전이 의존이던 `commons-codec`을 명시 의존성으로 추가했다. `AwsS3Config`는 v2 `S3Client`와 `S3Presigner`를 제공한다.
- 2026-05-22: Task 2 완료. `ImageUploadService`를 v2 `S3Presigner.presignPutObject()` 기반으로 전환하고, product 단위 테스트에서 v1 `AmazonS3` fake를 제거했다.
- 2026-05-22: Task 3 완료. `TestContainersConfig`, mono MinIO 단위 테스트, product MinIO 통합 테스트를 v2 `S3Client`/`S3Presigner` 기준으로 전환했다.
- 2026-05-22: Task 4 완료. admin 업로드 통합 테스트를 v2 `S3Client` 검증으로 전환했다. 테스트 컨텍스트에서 MinIO presigner가 선택되도록 테스트 전용 primary bean 이름을 분리했다.
- 2026-05-22: Task 5 완료. `com.amazonaws`, `aws-java-sdk-s3`, `libs.aws.s3` 잔존 검색 결과 없음.
- 2026-05-22: 구현 단계 검증 완료. `compileJava compileTestJava`, product `ImageUploadServiceTest`, mono `ImageUploadUnitTest`, product `MinioContainerLoadingTest`, `admin_integration_test`, `unit_test` 통과. `/verify full`은 마스터 지시 전까지 대기한다.
