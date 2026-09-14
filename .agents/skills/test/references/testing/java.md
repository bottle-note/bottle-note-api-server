# BottleNote 테스트 참조

Java·Kotlin 테스트를 작성할 때 현재 구현과 함께 읽는다. 실행 권한과 검사 범위는 프로젝트 지침의 `스킬 실행 원칙`을 따르며, 실행·Actions 관찰 명령은 [검증 참조](../../../verify/references/verify/java-gradle.md)에 있다.

## 태그와 테스트 위치

- `unit`: Spring context 없이 Service·도메인 동작을 Fake/InMemory로 검증한다. `unit_test`가 선택한다.
- `integration`: Product의 `IntegrationTestSupport`를 사용하는 API·영속성 통합 테스트이며 `integration_test`가 선택한다.
- `admin_integration`: Admin Kotlin의 통합 테스트이며 `admin_integration_test`가 선택한다.
- `rule`: ArchUnit 규칙이며 `check_rule_test`가 선택한다.
- `batch`: Batch 전용 테스트이며 `batch_test`가 선택한다. Batch는 위 네 공통 태스크가 비활성화되어 있다.

기본 `test`는 `integration`과 `admin_integration`을 제외한다. unit만 실행하거나 모든 통합 테스트를 실행하는 명령으로 설명하지 않는다. 다른 태그·무태그·모듈별 태스크 설정도 확인한다.

테스트 클래스는 `{기능명}ServiceTest`, `{기능명}IntegrationTest` 등 주변 명명 규칙을 따른다. 기존 `Fake{기능명}ServiceTest`도 있으나 새 클래스에 Fake 접두사를 일괄 강제하지 않는다. 메서드는 관찰 가능한 동작을 표현하고 `@DisplayName`은 “~할 때 ~한다” 형식의 한글로 작성한다.

## 공용 헬퍼와 모듈별 기반 클래스

저장소 루트 기준 실제 위치는 다음과 같다.

- Product 기반 클래스: `bottlenote-product-api/src/test/java/app/bottlenote/IntegrationTestSupport.java`
- Admin 기반 클래스: `bottlenote-admin-api/src/test/kotlin/app/IntegrationTestSupport.kt`
- 공용 인프라: `bottlenote-test-support/src/main/java/app/bottlenote/operation/utils/`의 `TestContainersConfig`, `DataInitializer`, `TestAuthenticationSupport`, `FakeWebhookRestTemplate`
- 공용 Factory·InMemory: `bottlenote-test-support/src/main/java/app/bottlenote/{domain}/fixture/`
- Product 전용 publisher Fake: `bottlenote-product-api/src/test/java/app/bottlenote/common/event/fixture/FakeApplicationEventPublisher.java`
- Batch 기반 사례: `bottlenote-batch/src/test/java/app/batch/bottlenote/BatchApplicationContextTest.java`와 `job/popularity/`

Product·Admin·mono는 test-support를 testImplementation으로 사용한다. 특정 모듈에만 쓰는 헬퍼는 그 모듈 test에 둘 수 있다. 새 Fake를 만들기 전에 전체 모듈에서 기존 구현을 검색하며, 기반 클래스를 mono의 test에 있다고 가정하지 않는다.

Product의 `getToken(User)`는 TokenItem을 반환하고 `getToken()`은 access token 문자열을 반환한다. Admin은 `createToken(AdminUser)`와 `getAccessToken(AdminUser)`를 제공한다. 호출 전에 실제 메서드 시그니처를 확인하며 토큰 값을 출력하지 않는다.

## Fake/InMemory 단위 테스트

대상 Service에 도메인 포트의 InMemory와 타 도메인의 Fake Facade를 직접 주입한다. Given-When-Then 구조로 정상 동작, 경계값, 예외, 저장 상태와 이벤트 발생 조건을 검증한다. 반환값만 맞춘 빈 구현이나 구현 호출 순서를 그대로 복제하는 테스트는 피한다.

포트 변경 시 JPA·Redis 구현뿐 아니라 모든 Fake/InMemory의 메서드와 의미를 갱신한다. 필터·정렬·삭제·중복·페이징 동작도 실제 계약에 맞아야 한다. Mock framework를 새 기본 패턴으로 도입하지 않는다. Fake로 격리하기 어려운 외부 경계는 현재 지침의 예외 기준과 사용자 요청을 확인한다.

이벤트 publisher Fake는 발행된 payload·횟수·조건을 검증한다. AFTER_COMMIT 실행이나 rollback 시 미실행을 입증하지는 않는다. 이 동작에는 실제 트랜잭션 경계를 사용하는 통합 테스트가 필요하다.

## 통합 테스트와 정리

Product·Admin의 기반 클래스를 상속하고 MockMvcTester, 실제 auth helper, 해당 도메인의 TestFactory를 재사용한다. 공용 TestContainersConfig는 MySQL·Redis·MinIO 등을 구성하고 외부 webhook·비속어 client를 테스트 대역으로 격리한다. MySQL·Redis의 reuse 설정을 모든 컨테이너의 재사용 보장으로 확대하지 않는다.

Product·Admin의 테스트 스키마는 Flyway가 만들고 Hibernate가 validate한다. 마이그레이션은 서브모듈의 `storage/db/migration/`에서 processResources로 복사된다. init SQL이나 ddl-auto create로 마이그레이션 결함을 우회하지 않는다.

현재 `DataInitializer.deleteAll()`은 테이블을 동적으로 찾고 별도 트랜잭션에서 DELETE로 정리한다. AUTO_INCREMENT를 초기화하지 않으므로 고정 ID를 기대하지 않는다. 제외 prefix는 `flyway_`, `databasechangelog`, `schema_version`이다. BATCH·QRTZ 테이블까지 자동 제외한다고 가정하지 않는다. 동적 테이블 추가 시 기존 `refreshCache()` 동작을 확인한다.

비동기 검증에는 기존 Awaitility 패턴으로 완료 조건을 기다리고 임의 sleep을 추가하지 않는다. 테스트가 rollback되는 트랜잭션 안에서만 이벤트를 발행하면 AFTER_COMMIT을 관찰할 수 없으므로 commit·rollback 시나리오를 분리한다.

## OpenAPI 계약

- Product: `bottlenote-product-api/src/test/java/app/bottlenote/global/integration/`의 `OpenApiSpecTestSupport`, `OpenApiSpecQualityTest`, `OpenApiSecurityRequirementTest`와 기능별 계약 테스트를 참고한다.
- Admin: `bottlenote-admin-api/src/test/kotlin/app/integration/openapi/`의 같은 역할의 Kotlin 기반·품질·보안 테스트를 참고한다.
- 실제 context에서 `/api/v1/openapi.product.json`, `/admin/api/v1/openapi.admin.json`을 가져와 OpenAPI 3.1, 경로·파라미터·명시적 응답 data 스키마, 실제 bearer 요구사항, 무인증 문서 접근 정책을 검사한다.
- DTO 필드나 endpoint를 바꾸면 관련 문서 어노테이션과 런타임 JSON 검증을 함께 갱신한다. 어노테이션 문자열만 검사하여 customizer·보안 설정을 건너뛰지 않는다.

## 결과 해석

작성한 테스트 수와 실행되어 통과한 테스트 수를 구분한다. CI의 정확한 SHA·실행 범위가 현재 변경을 포함하는지 확인하며 미커밋 테스트 코드는 과거 CI 성공으로 검증되지 않는다. skipped, disabled, NO-SOURCE, 필터 불일치, 환경 오류를 통과로 집계하지 않는다. 테스트 실행 시간을 직접 측정하지 않았다면 속도나 개선율을 제시하지 않는다.
