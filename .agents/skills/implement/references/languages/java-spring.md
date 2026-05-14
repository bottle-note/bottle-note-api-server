# Language: java-spring

Battle-tested patterns for Java 21 + Spring Boot 3.x. Generalized from `bottle-note-api-server` (production-validated).

> Use this with the matching `types/*.md` (typically `web-api.md` or `batch.md`).

## Module Layout (multi-module Gradle)

Recommended structure:
- **`{root}-core`** (a.k.a. `mono`) — domain, business logic, JPA, QueryDSL, Redis. Library JAR. ALL business assets live here.
- **`{root}-api`** — public REST API. Depends on core. `bootJar`.
- **`{root}-admin-api`** — admin API with context-path `/admin/api/v1`. Depends on core. `bootJar`.
- **`{root}-batch`** — Spring Batch + Quartz. Depends on core. `bootJar`.
- **`{root}-observability`** (optional) — OpenTelemetry / Micrometer.

Build: Gradle + version catalogs (`libs.versions.toml`). Formatting: `google-java-format` via Spotless.

## Repository 3-Tier Pattern

### Tier 1 — Domain Repository (required, pure interface)

Location: `{domain}/domain/{Domain}Repository.java`
- Annotation: `@DomainRepository` (optional marker)
- NO Spring / JPA dependency
- The ONLY interface services depend on

```java
@DomainRepository
public interface RatingRepository {
    Rating save(Rating rating);
    Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
```

### Tier 2 — JPA Repository (required, implementation)

Location: `{domain}/repository/Jpa{Domain}Repository.java`
- Annotation: `@JpaRepositoryImpl` (mandatory, includes `@Repository`)
- Extends `JpaRepository<T, ID>` + implements Domain repo
- Integrates QueryDSL Custom repo when needed

```java
@JpaRepositoryImpl
public interface JpaRatingRepository
    extends JpaRepository<Rating, Long>, RatingRepository, CustomRatingRepository {
}
```

### Tier 3 — QueryDSL Custom (optional, complex queries only)

Use ONLY for: dynamic multi-condition filters, multi-table joins+aggregation, complex projections. Do NOT use for: simple CRUD, single-condition lookups (method queries handle it).

Files: `Custom{Domain}Repository.java` (interface), `Custom{Domain}RepositoryImpl.java`, `{Domain}QuerySupporter.java` (`@Component`).

**Trap (verified in production)**: Never use a method-local `record` with `Projections.constructor()`. The compiler hides an outer-class reference parameter, so reflection-based constructor matching fails at runtime (compile passes). Always declare such records at class/interface level.

## Service Pattern

Default: a single `{Domain}Service`. The pre-existing Command/Query split is OPTIONAL — do not force it on new code.

```java
@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final AlcoholFacade alcoholFacade;          // cross-domain via Facade
    private final ApplicationEventPublisher publisher;

    @Transactional
    public RatingRegisterResponse register(Long alcoholId, Long userId, RatingPoint point) {
        Objects.requireNonNull(alcoholId, "alcoholId must not be null");

        if (FALSE.equals(alcoholFacade.existsByAlcoholId(alcoholId))) {
            throw new RatingException(RatingExceptionCode.ALCOHOL_NOT_FOUND);
        }

        Rating rating = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)
            .orElse(Rating.builder().alcoholId(alcoholId).userId(userId).build());
        rating.registerRatingPoint(point);
        ratingRepository.save(rating);

        publisher.publishEvent(new RatingRegistryEvent(alcoholId, userId));
        return new RatingRegisterResponse(rating.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingListFetchResponse> fetchList(RatingListFetchCriteria criteria) {
        // read-only methods → readOnly = true
    }
}
```

## Aggregate Root + Facade

Domains group into Aggregates. External access goes through the Aggregate Root (Facade) ONLY.

```
ranking (Aggregate)
├── RankingService          ← external access via Facade only
├── RankingPointService     ← internal, no external access
├── RankingHistoryService   ← internal
└── RankingFacade           ← the only external door
```

### Access Rules

```
[OK]  UserService → UserRepository      (same Aggregate)
[OK]  UserService → RankingFacade       (other Aggregate, via Facade)
[NO]  UserService → RankingRepository   (cross-Aggregate direct)
[NO]  UserService → RankingPointService (cross-Aggregate internal)
```

### Facade Implementation

```java
// Interface: {domain}/facade/{Domain}Facade.java
public interface AlcoholFacade {
    Boolean existsByAlcoholId(Long alcoholId);
    AlcoholInfo getAlcoholInfo(Long alcoholId);
}

// Implementation: {domain}/service/Default{Domain}Facade.java
@FacadeService                       // includes @Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAlcoholFacade implements AlcoholFacade {
    private final AlcoholRepository alcoholRepository;
    // Only uses its own Aggregate's repository
}
```

## DTO Patterns

### Request (record + Bean Validation)

```java
public record RatingRegisterRequest(
    @NotNull Long alcoholId,
    @NotNull Double rating
) {}
```

### Pageable Request (defaults via @Builder)

```java
public record ReviewPageableRequest(
    ReviewSortType sortType, SortOrder sortOrder, Long cursor, Long pageSize
) {
    @Builder
    public ReviewPageableRequest {
        sortType = sortType != null ? sortType : ReviewSortType.POPULAR;
        cursor = cursor != null ? cursor : 0L;
        pageSize = pageSize != null ? pageSize : 10L;
    }
}
```

### Response (factory method, never `from(Entity)`)

```java
public record RatingListFetchResponse(Long totalCount, List<Info> ratings) {
    public record Info(Long ratingId, Long alcoholId, Double rating) {}
    public static RatingListFetchResponse create(Long total, List<Info> infos) {
        return new RatingListFetchResponse(total, infos);
    }
}
```

**DTO–Entity separation (architectural rule)**: Response DTOs MUST NOT reference Entity directly. Conversion lives in Service via `of(...)` factory or direct constructor. `from(Entity)` on DTO is prohibited — DTO knowing Entity is a coupling violation.

## Exception Pattern

```java
// {domain}/exception/{Domain}Exception.java
public class RatingException extends AbstractCustomException {
    public RatingException(RatingExceptionCode code) { super(code); }
}

// {domain}/exception/{Domain}ExceptionCode.java
@Getter
public enum RatingExceptionCode implements ExceptionCode {
    INVALID_RATING_POINT(HttpStatus.BAD_REQUEST, "invalid rating point"),
    ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "alcohol not found");

    private final HttpStatus httpStatus;
    private final String message;

    RatingExceptionCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
```

Global handler: `@RestControllerAdvice` catches `AbstractCustomException` and maps to `GlobalResponse` shape.

## Event Pattern (async + REQUIRES_NEW)

```java
// Event as record
public record RatingRegistryEvent(Long alcoholId, Long userId) {}

// Listener
@DomainEventListener                  // includes @Component
@RequiredArgsConstructor
public class RatingEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRatingRegistry(RatingRegistryEvent event) {
        // Side effects in a separate transaction
    }
}
```

Pattern: async + `REQUIRES_NEW` separates side-effect transaction from main. Failures in listeners do NOT roll back the main transaction.

**Explicit `phase` required.** Production-grade projects with ArchUnit rules (e.g., a `EventListenerRules` test) enforce explicit `phase = TransactionPhase.AFTER_COMMIT` (or another deliberate value). Omitting `phase` defaults to `AFTER_COMMIT` at runtime, but the rule test FAILS at build time. Always specify `phase` explicitly — do not rely on the default.

## Custom Annotations (semantic markers)

| Annotation | Role | Location | Includes |
|------------|------|----------|----------|
| `@FacadeService` | Aggregate Root external door | `{domain}/facade/` | `@Service` |
| `@DomainRepository` | Pure domain interface | `{domain}/domain/` | (none, marker) |
| `@JpaRepositoryImpl` | JPA implementation | `{domain}/repository/` | `@Repository` |
| `@DomainEventListener` | Domain event listener | `{domain}/event/` | `@Component` |
| `@ThirdPartyService` | External system integration | `app.external` | `@Service` |

## Folder Convention

```
{root}-core/src/main/java/app/{root}/{domain}/
├── constant/       # enums, constants
├── domain/         # Entity, DomainRepository interface
├── dto/
│   ├── request/    # Request records
│   ├── response/   # Response records
│   └── dsl/        # QueryDSL criteria
├── event/          # domain events, listeners
├── exception/      # Exception + ExceptionCode
├── facade/         # Facade interface
├── repository/     # Jpa{Domain}Repository, Custom repo
└── service/        # {Domain}Service, Default{Domain}Facade

{root}-api/src/main/java/app/{root}/{domain}/
└── controller/     # {Domain}Controller (thin, delegates to core)
```

## Lombok Policy

- Use: `@Getter`, `@Builder`, `@RequiredArgsConstructor`
- Fields: prefer `final`
- Avoid: `@Data` (mutable + auto equals/hashCode), `@AllArgsConstructor` (unnecessary exposure)

## Caching

- Prefer: `@Cacheable` (Spring Cache abstraction) — backing store (Caffeine / Redis) swappable
- Avoid: manual `RedisTemplate` calls (bypass cache abstraction)
- TTL: configure per cache region

## Pagination

| Surface | Style | Reason |
|---------|-------|--------|
| Public API | **Cursor** (`CursorPageable` + `PageResponse`) | Stable under concurrent writes, real-time friendly |
| Admin API | **Offset** (`page`, `size`, Spring `Pageable`) | Admin UI needs page-jump |

### Cursor example

```java
public PageResponse<T> fetchList(Criteria criteria) {
    List<T> items = queryFactory.selectFrom(...)
        .where(cursorCondition(criteria.cursor()))
        .limit(criteria.pageSize() + 1)            // +1 to detect hasNext
        .fetch();

    CursorPageable pageable = CursorPageable.of(items, criteria.cursor(), criteria.pageSize());
    return PageResponse.of(
        items.subList(0, Math.min(items.size(), criteria.pageSize())),
        pageable
    );
}

// In controller
MetaInfos metaInfos = MetaService.createMetaInfo();
metaInfos.add("pageable", response.cursorPageable());
metaInfos.add("searchParameters", request);
return GlobalResponse.ok(response, metaInfos);
```

## Null Safety

- NullAway + `@NullMarked` (declared in `package-info.java` per package)
- Nullable record fields: explicit `@Nullable`
- **Before changing null annotations**, verify existing `package-info.java` presence in the target package

## Response Wrapper (consistency)

- `GlobalResponse.ok(data)` — simple data
- `GlobalResponse.ok(data, metaInfos)` — paginated / with metadata
- `GlobalResponse.fromPage(page)` — admin offset pages
- Errors: standardized by `@RestControllerAdvice`

## Performance Defaults

- **N+1 avoidance**: fetch joins, `@BatchSize`, careful eager/lazy decisions
- **Projection over fetch**: use DTO projection (`Projections.constructor()` at class level, see Tier 3 trap) for read-heavy queries
- Read-only methods: `@Transactional(readOnly = true)` (Hibernate skips dirty checking)

## External Integration Layer (`app.external`)

External system integrations (notification, push, FCM, S3, OpenFeign clients, payment gateways) often live in a separate top-level package (e.g., `app.external.{integration-name}`) rather than `app.{root}.{domain}`. They follow a **different convention** than mono's standard 3-Tier domain pattern.

### Differences from regular domains

| Aspect | Regular domain (`app.{root}.{domain}`) | External integration (`app.external.{name}`) |
|--------|----------------------------------------|----------------------------------------------|
| Repository | 3-Tier (`@DomainRepository` + `@JpaRepositoryImpl` + Custom QueryDSL) | Simple `extends JpaRepository<T, ID>` allowed — external API call is the primary responsibility, rich domain model often unnecessary |
| Service | `@Service` | `@ThirdPartyService` (marks external-system communication) |
| Test isolation | Fake/InMemory of own Repository | Fake of the external client (e.g., `FakeProfanityClient`, `FakeWebhookRestTemplate`) |
| ArchUnit scope | `app.{root}` package | Separate scope needed — extend rules to include `app.external` |

### Access boundary

```
[OK]  ReviewService → NotificationFacade        (or NotificationService in app.external.notification)
[NO]  ReviewService → NotificationRepository    (cross-layer direct, even worse than cross-aggregate)
```

External integrations expose a service / facade interface that the core domains consume. The internal Repository (which is often simpler) is invisible to callers.

### Examples in practice

`app.external.notification` (in-app + push), `app.external.fcm` (Firebase), `app.external.s3` (AWS S3), `app.external.openfeign.*` (external HTTP clients).

## Common Anti-patterns

- Business logic in `@RestController` (must delegate to `@Service`)
- Cross-Aggregate `@Repository` injection (use Facade)
- Local `record` in `Projections.constructor()` (runtime reflection failure)
- DTO referencing Entity (architectural violation)
- Manual `RedisTemplate` for cacheable data (use `@Cacheable`)
- `@Async` event listener without `REQUIRES_NEW` (main txn rollback infects listener)
