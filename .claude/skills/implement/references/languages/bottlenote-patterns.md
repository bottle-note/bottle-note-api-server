# BottleNote 변경 시 확인할 계약

프로젝트 지침의 `스킬 실행 원칙`이 우선한다. 포트·Facade, OpenAPI, 이벤트, 마이그레이션 또는 Batch를 바꿀 때 관련 항목만 읽는다. 전체 언어 설명은 [java-spring.md](java-spring.md)에 있다.

## 포트 변경과 테스트 더블

Repository 또는 Facade의 메서드·시그니처가 바뀌면 구현체와 모든 소비자를 함께 검색한다. 공용 더블은 `bottlenote-test-support/src/main/java/app/bottlenote/{domain}/fixture/`에 있고, 모듈 전용 더블은 Product의 `src/test/java/`나 Admin의 `src/test/kotlin/` 등에 남아 있다. 이름만 추측하지 말고 `rg --files`, 인터페이스 구현·참조 검색으로 실제 위치를 찾는다.

Fake/InMemory는 반환값뿐 아니라 필터, 정렬, 갱신·삭제, 중복, 페이징 계약을 재현해야 한다. JPA 구현만 고치거나 테스트를 통과시키기 위해 Fake를 느슨하게 만들지 않는다. 공용 테스트 인프라를 운영 모듈로 되돌리지 않는다.

## OpenAPI

- Product 공개 JSON은 `/api/v1/openapi.product.json`, Admin 공개 JSON은 `/admin/api/v1/openapi.admin.json`이다.
- Product의 `src/main/java/app/global/config/`, Admin의 `src/main/kotlin/app/global/config/`에서 OpenAPI·응답·보안 customizer를 확인한다.
- Product 계약 테스트는 `bottlenote-product-api/src/test/java/app/bottlenote/global/integration/`, Admin은 `bottlenote-admin-api/src/test/kotlin/app/integration/openapi/`에 있다.
- OpenAPI 3.1, 명시적 GlobalResponse data 스키마, 실제 인증 정책과 일치하는 bearer 요구, 무인증 문서 접근, Swagger UI 미제공, `https://bottle-note.github.io` 문서 CORS 정책을 보존한다.
- Controller·Docs 어노테이션 수정과 런타임 JSON 계약 테스트 작성을 함께 진행한다. CI는 Product·Admin 통합 jobs에서 이 테스트를 검증한다. 어노테이션이나 로컬 build만으로 런타임 계약을 검증했다고 보고하지 않는다.

자세한 HTTP·커서 계약은 [web-api.md](../types/web-api.md)를 따른다.

## Flyway와 패키징

엔티티의 스키마가 바뀌면 마이그레이션을 함께 작성한다. 원본은 비공개 서브모듈의 `git.environment-variables/storage/db/migration/`에 있고 Product·Admin의 processResources가 `classpath:db/migration`으로 복사한다. 생성된 build 리소스를 직접 고치지 않는다.

운영 및 API 통합 테스트는 Flyway와 Hibernate validate를 사용한다. 임의 baseline이나 ddl-auto 변경으로 누락을 숨기지 않는다. 서브모듈 수정과 상위 gitlink 반영 필요 여부를 분리하여 보고하며, 커밋·포인터 변경은 명시된 Git 권한을 따른다. 민감한 설정 내용은 출력하지 않는다.

Batch는 Flyway를 실행하지 않는다. `bottlenote-batch/build.gradle`은 지정된 두 SQL 리소스만 main·test에 복사하고 `verifyBatchPackagedResources` 태스크를 정의한다. 해당 태스크가 검증 명령에 실제 연결되어 있는지 확인하며 존재만으로 실행됐다고 주장하지 않는다. 배포·환경 설정 전체를 Batch JAR에 포함하지 않는다.

## 이벤트와 projection

Service 변경 시 기존 `publishEvent`가 제거·우회되거나 중복되지 않았는지 확인한다. 발행은 활성 트랜잭션 안에서, 커밋 후 부수효과는 AFTER_COMMIT listener에서 처리한다. Fake publisher 검증과 실제 commit·rollback 통합 검증은 서로 다른 증거다.

QueryDSL의 local record 문제는 숨은 outer 인자 문제가 아니다. public 생성자 조회와 접근성을 확인하며 [java-spring.md](java-spring.md)의 설명을 따른다.

## Batch 검사 범위

Batch에는 `@Tag("batch")` 테스트와 `batch_test` 태스크가 있다. PR CI는 popularity 패키지를 선택하고 배포 workflow는 전체 Batch 테스트를 실행한다. 전체 Batch 테스트의 실행 범위를 축소하거나 미실행 테스트를 통과로 보고하지 않는다. 실제 의존성과 리소스 선택은 Batch build 파일에서 확인한다.

Batch 동작의 기준과 테스트 위치는 [batch.md](../types/batch.md), 실행·관찰은 [검증 참조](../../../verify/references/verify/java-gradle.md)에 있다. 이 참조를 읽었다는 이유로 로컬 전체 검증이나 배포 workflow를 시작하지 않는다.
