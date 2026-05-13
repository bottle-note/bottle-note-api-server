# Test Infrastructure

## Test Classification

| Tag | Type | Base Class | Location |
|-----|------|------------|----------|
| `@Tag("unit")` | Unit test | None (plain JUnit) | `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/service/` |
| `@Tag("integration")` | Integration test | `IntegrationTestSupport` | `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/integration/` |
| `@Tag("admin_integration")` | Admin integration | `IntegrationTestSupport` | `bottlenote-admin-api/src/test/kotlin/app/integration/{domain}/` |
| (none) | RestDocs test | `AbstractRestDocs` | `bottlenote-product-api/src/test/java/app/docs/{domain}/` |

## Shared Test Utilities

### IntegrationTestSupport

Location: `bottlenote-product-api/src/test/java/app/bottlenote/IntegrationTestSupport.java`

**Fields:**
- `ObjectMapper mapper` - JSON serialization
- `MockMvc mockMvc` - legacy Spring MVC test API
- `MockMvcTester mockMvcTester` - modern Spring 6+ fluent API (prefer this)
- `TestAuthenticationSupport authSupport` - token generation
- `DataInitializer dataInitializer` - DB cleanup

**Helper methods:**
- `getToken()` - default user token (3 overloads: no-arg, User, userId)
- `getTokenUserId()` - get userId from default token
- `extractData(MvcTestResult, Class<T>)` - parse GlobalResponse.data into target type
- `extractData(MvcResult, Class<T>)` - legacy MockMvc version
- `parseResponse(MvcTestResult)` - parse raw response to GlobalResponse
- `parseResponse(MvcResult)` - legacy version

**Auto cleanup:** `@AfterEach` calls `dataInitializer.deleteAll()` which TRUNCATEs all tables except system tables (databasechangelog, flyway, schema_version).

### TestContainersConfig

Location: `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`

Containers (all with `@ServiceConnection` for auto-wiring):
- **MySQL 8.0.32** - `withReuse(true)`, DB name from `System.getProperty("testcontainers.db.name", "bottlenote")`
- **Redis 7.0.12** - `withReuse(true)`
- **MinIO** - S3-compatible storage with `AmazonS3` client bean

Fake beans (`@Primary`, replaces real implementations in test context):
- `FakeWebhookRestTemplate` - captures HTTP calls instead of sending
- `FakeProfanityClient` - returns clean text without calling external API

### TestAuthenticationSupport

Location: `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestAuthenticationSupport.java`

- `getFirstUser()` - first user from DB
- `getAccessToken()` - default access token
- `getRandomAccessToken()` - random user token
- `createToken(OauthRequest)` / `createToken(User)` - custom token generation
- `getDefaultUserId()` / `getUserId(String email)` - user ID lookup

### DataInitializer

Location: `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/DataInitializer.java`

- `deleteAll()` - TRUNCATE all user tables (dynamic table discovery via `SHOW TABLES`)
- `refreshCache()` - refresh table list for dynamically created tables
- `@Transactional(REQUIRES_NEW)` for isolation
- Filters: `databasechangelog*`, `flyway_*`, `schema_version`, `BATCH_*`, `QRTZ_*`

## Test Data Patterns

### TestFactory (Integration tests)

Location: `bottlenote-mono/src/test/java/app/bottlenote/{domain}/fixture/{Domain}TestFactory.java`

TestFactory uses `EntityManager` + `@Transactional` (not JPA repositories). Method naming: `persist{Entity}()`.

```java
@Component
public class RatingTestFactory {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AlcoholTestFactory alcoholTestFactory;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Transactional
    public Rating persistRating(Long alcoholId, Long userId) {
        Rating rating = Rating.builder()
            .alcoholId(alcoholId)
            .userId(userId)
            .ratingPoint(RatingPoint.FOUR)
            .build();
        em.persist(rating);
        em.flush();
        return rating;
    }
}
```

Key patterns:
- `AtomicInteger counter` for unique suffixes (names, emails)
- `persistAndFlush()` variants for immediate ID access
- Compose other factories: `AlcoholTestFactory` uses `RegionTestFactory`, `DistilleryTestFactory`

### ObjectFixture (Unit tests)

Location: `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/fixture/{Domain}ObjectFixture.java`

Static factory methods returning pre-configured domain objects. No DB, no Spring.

```java
public class RatingObjectFixture {
    public static Rating createRating(Long alcoholId, Long userId) {
        return Rating.builder()
            .alcoholId(alcoholId)
            .userId(userId)
            .ratingPoint(RatingPoint.FOUR)
            .build();
    }

    public static RatingRegisterRequest createRegisterRequest() {
        return new RatingRegisterRequest(1L, 4.5);
    }
}
```

Use ObjectFixture for unit tests, TestFactory for integration tests.

## Existing Fake/InMemory Implementations

Before creating a new Fake, check if one already exists.

### InMemory Repositories
- `InMemoryRatingRepository`, `InMemoryReviewRepository`, `InMemoryLikesRepository`
- `InMemoryUserRepository`, `InMemoryUserQueryRepository`
- `InMemoryAlcoholQueryRepository`, `InMemoryFollowRepository`
- `InMemoryPicksRepository` (also `FakePicksRepository` in `picks/fake/`)
- Plus others for block, support, banner, etc.

### Fake Services/Facades
- `FakeAlcoholFacade` - in-memory with `add()`, `remove()`, `clear()`
- `FakeUserFacade` - similar pattern
- `FakeHistoryEventPublisher` - captures published history events
- `FakeApplicationEventPublisher` - captures all Spring events (`getPublishedEvents()`, `getPublishedEventsOfType()`, `hasPublishedEventOfType()`, `clear()`)

### Fake External Services
- `FakeWebhookRestTemplate` - captures HTTP calls (`getCallCount()`, `getLastRequestBody()`)
- `FakeProfanityClient` - returns input text as-is
- Fake JWT/BCrypt implementations for security testing
