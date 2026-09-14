# BottleNote Java와 Kotlin 구현 패턴

Java 21·Spring·Gradle 모듈의 변경에 필요한 참조다. 프로젝트 지침의 레이어 표준과 공통 실행 계약을 따른다. HTTP 작업에는 [web-api.md](../types/web-api.md), Batch에는 [batch.md](../types/batch.md), 테스트 더블·마이그레이션 변경에는 [bottlenote-patterns.md](bottlenote-patterns.md)를 함께 확인한다.

## 모듈과 실제 위치

- `bottlenote-mono/src/main/java/app/bottlenote/`: 도메인 모델, 포트, Repository 구현, Service, Facade 등 공유 기능을 둔다.
- `bottlenote-product-api/src/main/java/app/bottlenote/`: Product Controller 등 API 구현을 둔다.
- `bottlenote-admin-api/src/main/kotlin/app/bottlenote/`: Kotlin Admin의 `presentation/`, `presentation/docs/`와 모듈 전용 기능을 둔다. 전역 설정은 `app/global/`에 있다.
- `bottlenote-batch/src/main/java/app/batch/bottlenote/`: Spring Batch·Quartz 구현을 둔다.
- `bottlenote-test-support/src/main/java/app/bottlenote/`: 공용 테스트 인프라·Fake·Factory를 둔다. Product·Admin·mono가 testImplementation으로 소비한다.
- `bottlenote-observability`: 관측 관련 공통 기능을 둔다.

모듈 구성은 `settings.gradle`, 의존성은 각 `build.gradle*`에서 확인한다. Admin도 mono의 Java 계약을 소비하므로 포트 변경 시 Kotlin 호출부를 함께 검색한다.

## Repository와 Facade

Repository는 순수 도메인 포트와 기술 구현의 2형으로 분리한다.

- 포트: `{domain}/domain/{Domain}Repository.java`에 둔다. `@DomainRepository`는 선택이며 Spring Data의 Page·Pageable·Slice·Sort를 공개하지 않는다.
- JPA 구현: `{domain}/repository/Jpa{Domain}Repository.java`에 `@JpaRepositoryImpl`을 붙이고 JPA 인터페이스와 포트를 연결한다.
- 복잡한 동적 조건·조인·projection만 `Custom{Domain}Repository`, 구현체, QuerySupporter로 분리한다. 단순 CRUD는 메서드 쿼리·JPQL을 우선한다.

Service는 자기 도메인 포트와 타 도메인의 Facade 인터페이스에 의존한다. 타 도메인 Service·Repository를 직접 참조하지 않는다. Facade 인터페이스는 `facade/`, `@FacadeService`를 붙인 `Default{Domain}Facade`는 `service/`에 둔다. Service가 Facade를 겸직하지 않는다. 공개 인터페이스 변경 시 Fake/InMemory와 Java·Kotlin 소비자를 함께 갱신한다.

Controller는 인터페이스에 의존하고 입력·응답을 다룬다. 트랜잭션 경계, VO 생성·검증, 도메인 조합은 Service가 소유한다. 기존 Command/Query 분리를 존중하되 관련 없는 재분리를 추가하지 않는다.

## DTO와 예외

Java DTO는 record, Kotlin DTO는 기존 data class 패턴을 따른다. Bean Validation을 경계에 적용하고 Kotlin은 주변 코드의 `@field:` 등 use-site target을 확인한다. 응답 DTO가 Entity를 직접 받거나 반환하지 않도록 변환 책임을 Service 등에 둔다.

도메인 예외와 ExceptionCode는 해당 `exception/`에 둔다. 공통 예외 베이스와 `GlobalResponse`·공통 페이징 계약의 위치는 현재 코드에서 확인한다. 현재 keyset 구현은 `global/pagination/KeysetPageResponse`, `KeysetPageRequest`, `KeysetPagination`이다.

Lombok은 기존 `@Getter`, `@Builder`, `@RequiredArgsConstructor`를 우선하고 상태의 불필요한 노출을 피한다. 새 null 분석 도구나 패키지 전체 어노테이션을 관습으로 가정하지 않는다.

## QueryDSL constructor projection

`Projections.constructor`는 대상 생성자의 접근성, 인자 순서와 타입을 모두 맞춰야 한다. 현재 `gradle/libs.versions.toml`은 OpenFeign QueryDSL을 사용한다. 설치된 6.11 sources의 `ConstructorUtils`는 `getConstructors()`·`getConstructor(...)`로 public 생성자를 조회한다.

메서드 local record는 implicitly static이므로 숨은 outer-instance 인자가 붙는다는 설명은 틀리다. 기본 local record의 canonical constructor는 public이 아니므로 public 생성자 탐색에서 빠질 수 있다. 읽기 쉬운 public DTO 또는 접근 가능한 public 중첩 record와 canonical constructor를 사용하고 실제 projection 경로에서 확인한다. 이는 “클래스 레벨이면 무조건 안전하다”는 규칙도 아니다. 언어 근거는 [Java 21 Record Classes](https://docs.oracle.com/en/java/javase/21/language/records.html)를 따른다.

## 이벤트와 트랜잭션

Service의 트랜잭션 안에서 도메인 변경 후 `ApplicationEventPublisher.publishEvent(...)`를 호출한다. 이벤트 발행과 리스너 실행 시점은 다르다. 커밋 후 실행할 리스너에 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`을 사용하며, 발행 자체를 트랜잭션 종료 뒤로 옮기지 않는다. 활성 트랜잭션 밖에서 발행하면 기본 설정의 transactional listener는 호출되지 않는다. [Spring의 transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)가 이 동작을 설명한다.

비동기 부수효과는 `@Async`, 별도 DB 쓰기 트랜잭션은 `REQUIRES_NEW`로 분리한다. `@DomainEventListener`는 Component와 처리 방식 표식을 제공하며 그 자체가 비동기 실행을 적용하지 않는다. 동기 처리가 필요한 리스너는 실제 계약에 맞춰 구분한다.

현재 참고 구현은 mono의 `notification/event/listener/ReviewReplyNotificationListener.java`이며, 규칙은 Product의 `src/test/java/app/rule/api/EventListenerRules.java`에 있다. 기존에 phase를 생략한 리스너도 있으므로 모든 기존 코드가 명시 규칙을 만족한다고 단정하지 않는다. 리팩토링 시 이벤트 발행 누락과 조건·횟수 변경을 점검한다.

## 외부 연동과 공유 상태

외부 연동은 `app.external`의 기존 `@ThirdPartyService`·client 경계를 확인하고 테스트에서는 외부 client Fake로 격리한다. 외부 패키지라는 이유만으로 도메인의 Facade·Repository 규칙을 완화하지 않는다.

다중 인스턴스에서 요청 카운트·분산 잠금·중복 억제·스케줄 소유권을 JVM static이나 인메모리 캐시로 구현하지 않는다. 공유 Redis 등 실제 저장소의 원자성·만료·소유권을 확인한다. 단순 캐시와 원자적인 제어 연산을 구분하고 `@Cacheable`로 모든 Redis 연산을 대체하지 않는다.

## 검증 연결

동작 변경에 필요한 테스트는 구현과 함께 작성한다. 구현 중 최소 검사와 전체 검증 권한은 프로젝트 지침을 따른다. 컴파일의 Spotless 부수효과와 Actions 관찰 방법은 [검증 참조](../../../verify/references/verify/java-gradle.md)에 있다.
