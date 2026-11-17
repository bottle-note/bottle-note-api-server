# BottleNote Java와 Kotlin 구현 패턴

Java 21·Spring·Gradle 모듈의 변경에 필요한 참조다. 프로젝트 지침의 레이어 표준과 공통 실행 계약을 따른다. HTTP 작업에는 [web-api.md](../types/web-api.md), Batch에는 [batch.md](../types/batch.md), 테스트 더블·마이그레이션 변경에는 [bottlenote-patterns.md](bottlenote-patterns.md)를 함께 확인한다.

## 모듈과 실제 위치

- `settings.gradle`의 16개 모듈과 각 `build.gradle*`를 먼저 확인한다. mono는 제거했다.
- 업무 모델·포트·Repository·Service·Facade는 해당 업무 라이브러리에 둔다. foundation은 업무 타입과 테이블을 모르는 기반만 소유한다.
- Product와 Admin은 진입점과 각 앱의 복합 조회를 소유한다. Admin은 Kotlin 진입점과 Java 조회 구현을 함께 사용한다.
- Batch는 기존 좁은 scan과 명시적 import를 유지한다. 모든 업무를 scan해 누락을 숨기지 않는다.
- 업무 Factory·Fake·InMemory는 소유 모듈의 `src/testFixtures/java`, 앱 조합 fixture는 앱의 `src/test`에 둔다.
- test-support의 Java 인프라는 TestContainersConfig·DataInitializer·FakeWebhookRestTemplate 세 개다. 필요한 업무 fixture는 testImplementation의 testFixtures(project(...))로 선택한다.

타 업무 호출은 Facade 등 공개 계약을 따른다. storage의 ResourceVerifierService·ResourceCommandService·ImageUploadService와 앱의 복합 조회는 협력 ADR 및 ModuleBoundaryRules의 허용 범위를 따른다. 읽기 전용 조인은 명확한 조회 구현에서만 허용하며 타 업무 저장소로 쓰지 않는다. Gradle implementation은 의존 모듈의 public 내부 클래스를 숨기지 않는다. 허용 방향과 공개 계약을 함께 검토한다.

## Repository와 Facade

Repository는 순수 도메인 포트와 기술 구현의 2형으로 분리한다.

- 포트: `{domain}/domain/{Domain}Repository.java`에 둔다. `@DomainRepository`는 선택이며 Spring Data의 Page·Pageable·Slice·Sort를 공개하지 않는다.
- JPA 구현: `{domain}/repository/Jpa{Domain}Repository.java`에 `@JpaRepositoryImpl`을 붙이고 JPA 인터페이스와 포트를 연결한다.
- 복잡한 동적 조건·조인·projection만 `Custom{Domain}Repository`, 구현체, QuerySupporter로 분리한다. 단순 CRUD는 메서드 쿼리·JPQL을 우선한다.

Service는 자기 업무의 포트와 명시된 타 업무 공개 계약을 사용한다. 일반적인 타 업무 내부 Service·Repository 참조는 피하고, 위에 적힌 좁은 계약과 조회 예외를 확인한다. Facade interface와 Default 구현체 분리는 기존 패턴이며 모든 클래스의 의무는 아니다. Product의 FollowQueryService는 FollowFacade를 구현한다. 대체가 필요한 포트와 계약은 유지하되 전달용 interface를 일괄 추가하지 않는다.

Controller는 공개 command/query 서비스나 해당 앱의 조회 서비스를 직접 주입할 수 있다. 업무 VO 생성·검증과 트랜잭션 경계는 서비스가 맡는다. 여러 업무의 응답 조합과 조회 구현은 사용하는 앱이 소유한다. 공개 계약을 바꾸면 Fake/InMemory와 Java·Kotlin 소비자도 확인한다.

소유는 모듈과 source set으로 판단하며 패키지명이 모듈명과 같을 필요는 없다. foundation은 업무 타입과 테이블을 참조하지 않는다. user의 global.security와 community의 global.rating·WebConfig는 기존 FQN을 보존한 업무 코드다. global/common이라는 이름만으로 foundation 소유나 업무 참조 금지를 적용하지 않는다.

## DTO와 예외

Java DTO는 record, Kotlin DTO는 기존 data class 패턴을 따른다. Bean Validation을 경계에 적용하고 Kotlin은 주변 코드의 `@field:` 등 use-site target을 확인한다. 응답 DTO가 Entity를 직접 받거나 반환하지 않도록 변환 책임을 Service 등에 둔다.

도메인 예외와 ExceptionCode는 해당 `exception/`에 둔다. 업무를 모르는 공통 예외 베이스와 `GlobalResponse`·공통 페이징 계약은 foundation이 소유한다. 패키지명만으로 소유를 판단하지 않는다. 현재 keyset 구현은 `global/pagination/KeysetPageResponse`, `KeysetPageRequest`, `KeysetPagination`이다.

Lombok은 기존 `@Getter`, `@Builder`, `@RequiredArgsConstructor`를 우선하고 상태의 불필요한 노출을 피한다. 새 null 분석 도구나 패키지 전체 어노테이션을 관습으로 가정하지 않는다.

## QueryDSL constructor projection

`Projections.constructor`는 대상 생성자의 접근성, 인자 순서와 타입을 모두 맞춰야 한다. 현재 `gradle/libs.versions.toml`은 OpenFeign QueryDSL을 사용한다. 설치된 6.11 sources의 `ConstructorUtils`는 `getConstructors()`·`getConstructor(...)`로 public 생성자를 조회한다.

메서드 local record는 implicitly static이므로 숨은 outer-instance 인자가 붙는다는 설명은 틀리다. 기본 local record의 canonical constructor는 public이 아니므로 public 생성자 탐색에서 빠질 수 있다. 읽기 쉬운 public DTO 또는 접근 가능한 public 중첩 record와 canonical constructor를 사용하고 실제 projection 경로에서 확인한다. 이는 “클래스 레벨이면 무조건 안전하다”는 규칙도 아니다. 언어 근거는 [Java 21 Record Classes](https://docs.oracle.com/en/java/javase/21/language/records.html)를 따른다.

## 이벤트와 트랜잭션

Service의 트랜잭션 안에서 도메인 변경 후 `ApplicationEventPublisher.publishEvent(...)`를 호출한다. 이벤트 발행과 리스너 실행 시점은 다르다. 커밋 후 실행할 리스너에 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`을 사용하며, 발행 자체를 트랜잭션 종료 뒤로 옮기지 않는다. 활성 트랜잭션 밖에서 발행하면 기본 설정의 transactional listener는 호출되지 않는다. [Spring의 transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)가 이 동작을 설명한다.

비동기 부수효과는 `@Async`, 별도 DB 쓰기 트랜잭션은 `REQUIRES_NEW`로 분리한다. `@DomainEventListener`는 Component와 처리 방식 표식을 제공하며 그 자체가 비동기 실행을 적용하지 않는다. 동기 처리가 필요한 리스너는 실제 계약에 맞춰 구분한다. HistoryRegistryListener는 원래 트랜잭션에서 동기로 받아 HistoryEventPublisher를 통해 이력 metadata를 만들고, HistoryListener가 AFTER_COMMIT·Async·REQUIRES_NEW로 저장한다. 모든 이벤트를 비동기로 바꾸지 않는다.

현재 참고 구현은 bottlenote-notification의 `notification/event/listener/ReviewReplyNotificationListener.java`이며, 규칙은 Product의 `src/test/java/app/rule/api/EventListenerRules.java`에 있다. 기존에 phase를 생략한 리스너도 있으므로 모든 기존 코드가 명시 규칙을 만족한다고 단정하지 않는다. 리팩토링 시 이벤트 발행 누락과 조건·횟수 변경을 점검한다.

## 외부 연동과 공유 상태

외부 연동은 `app.external`의 기존 `@ThirdPartyService`·client 경계를 확인하고 테스트에서는 외부 client Fake로 격리한다. 외부 패키지라는 이유만으로 도메인의 Facade·Repository 규칙을 완화하지 않는다.

다중 인스턴스에서 요청 카운트·분산 잠금·중복 억제·스케줄 소유권을 JVM static이나 인메모리 캐시로 구현하지 않는다. 공유 Redis 등 실제 저장소의 원자성·만료·소유권을 확인한다. 단순 캐시와 원자적인 제어 연산을 구분하고 `@Cacheable`로 모든 Redis 연산을 대체하지 않는다.

## 검증 연결

동작 변경에 필요한 테스트는 구현과 함께 작성한다. 구현 중 최소 검사와 전체 검증 권한은 프로젝트 지침을 따른다. 컴파일의 Spotless 부수효과와 Actions 관찰 방법은 [검증 참조](../../../verify/references/verify/java-gradle.md)에 있다.
