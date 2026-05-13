# Plan: 푸시 기능 제거

## Overview

초기 기능으로 들어갔던 푸시 알림 기능은 현재 제품 관점에서 필요성이 사라졌다. 동시에 `GET /api/v1/external/push?msg=` 엔드포인트는 인증 경계 밖에 놓여 있고, 내부 대상 사용자 ID도 `6L`로 하드코딩되어 있어 운영 리스크가 있다.

이번 변경의 목표는 푸시 발송, 디바이스 토큰 저장, Firebase FCM 초기화/전송 코드를 제거해 더 이상 푸시 관련 API와 런타임 의존성이 애플리케이션에 남지 않게 하는 것이다.

### Assumptions

#### 확정 가정

1. `GET /api/v1/external/push?msg=` 엔드포인트는 제거한다.
2. `POST /api/v1/external/push/token` 디바이스 토큰 저장 API도 제거한다.
3. Firebase FCM 전송 로직은 더 이상 사용하지 않는다.
4. product-api의 푸시 REST Docs 문서와 푸시 컨트롤러 RestDocs 테스트는 제거 대상이다.
5. OAuth 토큰 검증 API인 `PUT /api/v1/oauth/token/verify`는 푸시 기능이 아니므로 유지한다.
6. `SingleTokenRequest`는 현재 `app.external.push.data.request` 패키지에 있지만, 토큰 검증 DTO로 남겨야 하므로 user/auth 쪽 DTO로 이동하거나 대체한다.

#### 확인 필요 가정

1. `notifications`, `user_push_configs`, `user_device_tokens` 테이블의 DB schema 삭제는 이번 작업에서 하지 않고, 후속 migration 이슈로 분리한다.
2. `app.external.notification.*`의 인앱 알림 저장 모델은 푸시 발송과 분리된 개념일 수 있으므로, 이번 작업에서는 직접 사용 여부를 확인한 뒤 미사용일 때만 제거한다.
3. `firebase-admin` 의존성은 푸시 제거 후 참조가 0건이면 `bottlenote-mono`에서 제거한다.
4. `application-external.yml`, `application-test.yml`의 `firebase-configuration-file` 설정은 Firebase 참조 제거와 함께 삭제한다.
5. HTTP 수동 테스트 파일의 푸시 항목은 제거한다.

### Success Criteria

| # | 기준 | 검증 |
|---|------|------|
| SC1 | `/api/v1/external/push/**` 매핑이 product-api에서 제거된다. | `rg "/api/v1/external/push"` |
| SC2 | `PushController`, `PushHandler`, `DefaultPushHandler`, `UserDeviceService`, `UserDeviceTokenRepository`가 제거되거나 더 이상 컴파일 대상에 없다. | `rg "PushController|PushHandler|UserDeviceService|UserDeviceTokenRepository"` |
| SC3 | Firebase 초기화/전송 코드가 제거된다. | `rg "Firebase|FirebaseMessaging|firebase-admin"` |
| SC4 | product-api 문서에서 푸시 API include가 제거된다. | `rg "external/push|push/save-user-token" bottlenote-product-api/src/docs` |
| SC5 | 푸시 RestDocs 테스트가 제거되어 더 이상 snippet을 생성하지 않는다. | `rg "PushControllerRestDocsTest|push/save-user-token"` |
| SC6 | `PUT /api/v1/oauth/token/verify`는 유지되고 컴파일된다. | 관련 RestDocs/컨트롤러 테스트 |
| SC7 | SecurityConfig의 `/api/v1/push/**` 규칙처럼 더 이상 존재하지 않는 푸시 경로 정책이 제거된다. | `rg "/api/v1/push|/api/v1/external/push"` |
| SC8 | 관련 모듈 테스트가 통과한다. | `./gradlew :bottlenote-product-api:test :bottlenote-mono:test` |

### Impact Scope

**product-api**

- `bottlenote-product-api/src/main/java/app/external/push/presentation/PushController.java`
- `bottlenote-product-api/src/main/java/app/global/security/SecurityConfig.java`
- `bottlenote-product-api/src/main/java/app/bottlenote/user/controller/OauthController.java`
- `bottlenote-product-api/src/docs/asciidoc/product-api.adoc`
- `bottlenote-product-api/src/docs/asciidoc/api/external/push/device-token.adoc`
- `bottlenote-product-api/src/test/java/app/external/docs/push/ui/PushControllerRestDocsTest.java`
- `bottlenote-product-api/src/test/java/app/docs/user/RestOauthControllerTest.java`
- `bottlenote-product-api/src/main/resources/application-external.yml`
- `bottlenote-product-api/src/test/resources/application-test.yml`

**mono**

- `bottlenote-mono/src/main/java/app/external/push/**`
- `bottlenote-mono/src/main/java/app/external/notification/domain/UserDeviceToken.java`
- `bottlenote-mono/src/main/java/app/external/notification/domain/UserPushConfig.java`
- `bottlenote-mono/src/main/java/app/external/notification/domain/constant/Platform.java`
- `bottlenote-mono/build.gradle`

**admin-api / batch**

- `bottlenote-admin-api/src/main/resources/application-external.yml`
- `bottlenote-admin-api/src/test/resources/application-test.yml`
- `bottlenote-batch/src/main/resources/application-external.yml`
- `bottlenote-batch/src/test/resources/application.yml`
- 위 파일의 Firebase 설정은 mono의 Firebase 컴포넌트 제거 후 필요 여부를 확인한다.

**문서/수동 테스트**

- `http/product/99_공통/푸시알림.http`
- `http/product/01_회원관리/계정관리/디바이스토큰.http`

**DB schema**

- `notifications`, `user_push_configs`, `user_device_tokens` 테이블은 이번 작업에서 drop하지 않는다.
- 운영 데이터 삭제와 Liquibase migration은 후속 이슈에서 별도 승인 후 진행한다.

### Non-Goals

- admin ROOT_ADMIN 기본 계정 정책 변경
- admin RBAC 정책 추가
- batch resource include 범위 축소
- batch JWT secret/salt 기본값 제거
- 운영 DB 테이블 drop 또는 데이터 삭제

### Approval Gate

이 문서는 `/define` 단계의 산출물이다. 위 가정과 성공 기준이 맞으면 다음 단계에서 `/plan`으로 작업을 태스크 단위로 쪼갠다.

## Dependency Analysis

푸시 기능 제거는 product-api API 표면과 mono 런타임 컴포넌트가 얽혀 있다. 구현 순서는 먼저 푸시 패키지 밖에서 계속 필요한 참조를 분리하고, 그 다음 product-api의 외부 노출 경로를 닫은 뒤, 마지막으로 mono의 Firebase/디바이스 토큰 저장 계층과 의존성 설정을 제거한다.

의존성 순서:

1. `PUT /api/v1/oauth/token/verify`가 사용하는 `SingleTokenRequest`를 푸시 패키지에서 분리한다.
2. product-api의 `/api/v1/external/push/**` 컨트롤러와 보안 경로를 제거한다.
3. product-api의 푸시 REST Docs와 수동 HTTP 문서를 제거한다.
4. mono의 Firebase 전송 계층을 제거한다.
5. mono의 디바이스 토큰 저장 계층을 제거한다.
6. Firebase 의존성, 설정값, 남은 참조를 제거하고 검증한다.

## Tasks

### Task 1: OAuth 토큰 검증 DTO 분리

- 수용 기준: `PUT /api/v1/oauth/token/verify`가 더 이상 `app.external.push.*` 패키지의 DTO를 import하지 않는다.
- 수용 기준: 토큰 검증 요청 DTO가 user/auth 문맥의 패키지로 이동하거나 기존 user DTO로 대체된다.
- 검증: `rg "app.external.push.data.request.SingleTokenRequest" bottlenote-product-api bottlenote-mono` 결과가 0건이다.
- 파일: `bottlenote-product-api/src/main/java/app/bottlenote/user/controller/OauthController.java`, `bottlenote-product-api/src/test/java/app/docs/user/RestOauthControllerTest.java`, user request DTO 파일 1건
- 크기: S
- 상태: [x] 완료

### Task 2: product-api 푸시 API 제거

- 수용 기준: `PushController`가 제거되어 `/api/v1/external/push/**` 매핑이 더 이상 존재하지 않는다.
- 수용 기준: `SecurityConfig`의 존재하지 않는 푸시 경로 matcher가 제거된다.
- 검증: `rg "/api/v1/external/push|/api/v1/push" bottlenote-product-api/src/main/java` 결과가 0건이다.
- 파일: `bottlenote-product-api/src/main/java/app/external/push/presentation/PushController.java`, `bottlenote-product-api/src/main/java/app/global/security/SecurityConfig.java`
- 크기: S
- 상태: [x] 완료

### Task 3: product-api 푸시 문서 제거

- 수용 기준: product-api 문서에서 푸시 API include와 adoc 파일이 제거된다.
- 수용 기준: 푸시 RestDocs 테스트와 수동 HTTP 푸시 요청 파일이 제거된다.
- 검증: `rg "external/push|push/save-user-token|PushControllerRestDocsTest" bottlenote-product-api http` 결과가 0건이다.
- 파일: `bottlenote-product-api/src/docs/asciidoc/product-api.adoc`, `bottlenote-product-api/src/docs/asciidoc/api/external/push/device-token.adoc`, `bottlenote-product-api/src/test/java/app/external/docs/push/ui/PushControllerRestDocsTest.java`, `http/product/99_공통/푸시알림.http`, `http/product/01_회원관리/계정관리/디바이스토큰.http`
- 크기: M
- 상태: [x] 완료

### Checkpoint: Task 1-3 완료 후

- [x] product-api 컴파일 통과
- [x] product-api 테스트의 푸시 문서 참조 0건
- [x] OAuth 토큰 검증 API 컴파일 유지

### Task 4: mono Firebase 전송 계층 제거

- 수용 기준: Firebase FCM 초기화와 전송 담당 클래스가 제거된다.
- 수용 기준: `FirebaseMessaging`, `FirebaseApp`, `FirebaseOptions` 참조가 main source에서 사라진다.
- 검증: `rg "FirebaseMessaging|FirebaseApp|FirebaseOptions" bottlenote-mono/src/main/java` 결과가 0건이다.
- 파일: `bottlenote-mono/src/main/java/app/external/push/application/DefaultPushHandler.java`, `bottlenote-mono/src/main/java/app/external/push/application/PushHandler.java`, `bottlenote-mono/src/main/java/app/external/push/config/FirebaseInitializerConfig.java`, `bottlenote-mono/src/main/java/app/external/push/config/FirebaseProperties.java`, `bottlenote-mono/src/main/java/app/external/push/domain/PushStatus.java`
- 크기: M
- 상태: [x] 완료

### Task 5: mono 디바이스 토큰 저장 계층 제거

- 수용 기준: 디바이스 토큰 저장 서비스, 레포지토리, DTO, 엔티티 참조가 제거된다.
- 수용 기준: `user_device_tokens`와 `user_push_configs` JPA 매핑 클래스는 코드에서 제거되지만 DB table drop은 수행하지 않는다.
- 검증: `rg "UserDeviceToken|UserPushConfig|UserDeviceService|UserDeviceTokenRepository|TokenSaveRequest|TokenSaveResponse|TokenMessage|Platform" bottlenote-mono/src/main/java bottlenote-product-api/src/main/java` 결과가 0건이다.
- 파일: `bottlenote-mono/src/main/java/app/external/push/application/UserDeviceService.java`, `bottlenote-mono/src/main/java/app/external/push/repository/UserDeviceTokenRepository.java`, `bottlenote-mono/src/main/java/app/external/push/data/request/TokenSaveRequest.java`, `bottlenote-mono/src/main/java/app/external/push/data/response/TokenSaveResponse.java`, `bottlenote-mono/src/main/java/app/external/push/data/payload/TokenMessage.java`, `bottlenote-mono/src/main/java/app/external/notification/domain/UserDeviceToken.java`, `bottlenote-mono/src/main/java/app/external/notification/domain/UserPushConfig.java`, `bottlenote-mono/src/main/java/app/external/notification/domain/constant/Platform.java`
- 크기: M
- 상태: [x] 완료

### Task 6: Firebase 의존성 설정 제거

- 수용 기준: `firebase-admin` Gradle 의존성이 제거된다.
- 수용 기준: product-api, admin-api, batch 설정 파일에서 `firebase-configuration-file` 설정이 제거된다.
- 수용 기준: main/test source와 문서에서 Firebase/푸시 잔여 참조가 성공 기준 범위 내에서 사라진다.
- 검증: `rg "firebase-admin|firebase-configuration-file|Firebase|FirebaseMessaging|/api/v1/external/push|/api/v1/push" gradle bottlenote-mono bottlenote-product-api bottlenote-admin-api bottlenote-batch` 결과에 허용된 과거 plan 문서 외 잔여 참조가 없다.
- 파일: `bottlenote-mono/build.gradle`, `gradle/libs.versions.toml`, `bottlenote-product-api/src/main/resources/application-external.yml`, `bottlenote-product-api/src/test/resources/application-test.yml`, `bottlenote-admin-api/src/main/resources/application-external.yml`, `bottlenote-admin-api/src/test/resources/application-test.yml`, `bottlenote-batch/src/main/resources/application-external.yml`, `bottlenote-batch/src/test/resources/application.yml`
- 크기: M
- 상태: [x] 완료

### Checkpoint: Task 4-6 완료 후

- [x] `./gradlew :bottlenote-mono:test`
- [x] `./gradlew :bottlenote-product-api:test`
- [x] `rg "PushController|PushHandler|UserDeviceService|UserDeviceTokenRepository|FirebaseMessaging|firebase-admin|/api/v1/external/push|/api/v1/push" bottlenote-mono bottlenote-product-api bottlenote-admin-api bottlenote-batch gradle http`

## Progress Log

- 2026-05-13: Task 1 완료. OAuth 토큰 검증 요청 DTO를 `app.bottlenote.user.dto.request.TokenVerifyRequest`로 분리하고 product-api 컴파일을 확인했다.
- 2026-05-13: Task 2 완료. `PushController`와 product-api의 푸시 보안 matcher를 제거하고 main source 푸시 경로 참조 0건을 확인했다.
- 2026-05-13: Task 3 완료. 푸시 REST Docs, product-api include, 수동 HTTP 요청 파일을 제거하고 product-api test compile을 확인했다.
- 2026-05-13: Task 4 완료. mono의 Firebase 초기화/FCM 전송 계층을 제거하고 `FirebaseMessaging`, `FirebaseApp`, `FirebaseOptions` 참조 0건을 확인했다.
- 2026-05-13: Task 5 완료. 디바이스 토큰 저장 서비스/레포지토리/DTO/JPA 매핑을 제거하고 관련 main source 참조 0건을 확인했다.
- 2026-05-13: Task 6 완료. `firebase-admin` 의존성과 Firebase 설정값을 제거했다. `./gradlew :bottlenote-mono:test :bottlenote-product-api:test :bottlenote-admin-api:compileKotlin :bottlenote-batch:compileJava`가 성공했다.
- 2026-05-13: self-review 완료. `TokenVerifyRequest`에 `@NotBlank(message = "TOKEN_REQUIRED")`를 추가하고 `./gradlew compileJava compileTestJava unit_test check_rule_test` 성공을 확인했다.
- 2026-05-14: `/verify full` 완료. `compileJava compileTestJava`, admin Kotlin compile, `check_rule_test`, `unit_test`, `build -x test -x asciidoctor --build-cache --parallel`, `integration_test`, `admin_integration_test`가 모두 성공했다.
