# Test Patterns for Product API

## Test Classification

| Tag | Type | Base Class | Location |
|-----|------|------------|----------|
| `@Tag("unit")` | Unit test | None (plain JUnit) | `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/service/` |
| `@Tag("integration")` | Integration test | `IntegrationTestSupport` | `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/integration/` |
| (none) | RestDocs test | `AbstractRestDocs` | `bottlenote-product-api/src/test/java/app/docs/{domain}/` |

---

## Test Infrastructure

Read this section first — these are the shared utilities available across all test types.

### IntegrationTestSupport

Base class for all integration tests. Location: `bottlenote-product-api/src/test/java/app/bottlenote/IntegrationTestSupport.java`

**Provided fields:**
- `ObjectMapper mapper` - JSON serialization
- `MockMvc mockMvc` - legacy Spring MVC test API
- `MockMvcTester mockMvcTester` - modern Spring 6+ fluent API (prefer this)
- `TestAuthenticationSupport authSupport` - token generation
- `DataInitializer dataInitializer` - DB cleanup

**Provided helper methods:**
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

---

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

---

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

---

## 1. Unit Test - Fake/Stub Pattern (Preferred)

Mock 대신 InMemory 구현체를 사용하는 패턴. 실제 동작에 가깝고 리팩토링에 강하다.

### Fake Repository

```java
// Location: {domain}/fixture/InMemory{Domain}Repository.java
public class InMemoryRatingRepository implements RatingRepository {
    private final Map<Long, Rating> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Rating save(Rating rating) {
        if (rating.getId() == null) {
            ReflectionTestUtils.setField(rating, "id", idGenerator.getAndIncrement());
        }
        database.put(rating.getId(), rating);
        return rating;
    }

    @Override
    public Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
        return database.values().stream()
            .filter(r -> r.getAlcoholId().equals(alcoholId) && r.getUserId().equals(userId))
            .findFirst();
    }
}
```

### Fake Test

```java
// Location: {domain}/service/Fake{Domain}ServiceTest.java
@Tag("unit")
@DisplayName("{Domain} 서비스 단위 테스트")
class FakeRatingCommandServiceTest {

    private RatingCommandService sut; // system under test
    private InMemoryRatingRepository ratingRepository;
    private FakeApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ratingRepository = new InMemoryRatingRepository();
        eventPublisher = new FakeApplicationEventPublisher();
        sut = new RatingCommandService(ratingRepository, eventPublisher, /* other fakes */);
    }

    @Nested
    @DisplayName("평점 등록 시")
    class RegisterRating {

        @Test
        @DisplayName("유효한 요청이면 평점을 등록할 수 있다")
        void register_whenValidRequest_savesRating() {
            // given
            Long alcoholId = 1L;
            Long userId = 1L;
            RatingPoint point = RatingPoint.FIVE;

            // when
            RatingRegisterResponse response = sut.register(alcoholId, userId, point);

            // then
            assertThat(response).isNotNull();
            assertThat(ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)).isPresent();
        }

        @Test
        @DisplayName("이벤트가 발행된다")
        void register_publishesEvent() {
            sut.register(1L, 1L, RatingPoint.FIVE);

            assertThat(eventPublisher.getPublishedEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(RatingRegistryEvent.class);
        }
    }
}
```

## 2. Unit Test - Mockito (Last Resort Only)

Mockito는 최후의 수단이다. Fake/Stub으로 해결할 수 없는 경우에만 사용한다.
Mock은 구현 세부사항에 결합되어 리팩토링 시 깨지기 쉽고, 실제 동작을 검증하지 못한다.

**Mockito 사용 전 반드시 사용자에게 먼저 확인:**
- Fake 구현의 공수가 과도하게 큰 경우 (외부 시스템 연동, 복잡한 인프라 의존 등)
- 사용자에게 "Fake 구현 공수가 크니 Mock으로 타협할까요?" 라고 대화를 먼저 시작할 것
- 사용자 동의 없이 Mockito를 선택하지 말 것

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RatingCommandServiceTest {

    @InjectMocks
    private RatingCommandService sut;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private AlcoholFacade alcoholFacade;

    @Test
    @DisplayName("존재하지 않는 주류에 평점을 등록하면 예외가 발생한다")
    void register_whenAlcoholNotFound_throwsException() {
        // given
        given(alcoholFacade.existsByAlcoholId(anyLong())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.register(1L, 1L, RatingPoint.FIVE))
            .isInstanceOf(RatingException.class);
    }
}
```

## 3. Integration Test

Full Spring context with TestContainers (real DB).

```java
// Location: {domain}/integration/{Domain}IntegrationTest.java
@Tag("integration")
@DisplayName("{Domain} 통합 테스트")
class RatingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RatingTestFactory ratingTestFactory;

    @Test
    @DisplayName("평점을 등록할 수 있다")
    void registerRating() {
        // given - TestFactory로 데이터 생성
        User user = userTestFactory.persistUser();
        Alcohol alcohol = alcoholTestFactory.persistAlcohol();
        String token = getToken(user);

        RatingRegisterRequest request = new RatingRegisterRequest(alcohol.getId(), 4.5);

        // when - MockMvcTester (modern API)
        MvcTestResult result = mockMvcTester.post()
            .uri("/api/v1/ratings")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(mapper.writeValueAsString(request))
            .exchange();

        // then - helper methods
        assertThat(result).hasStatusOk();
        GlobalResponse response = parseResponse(result);
        assertThat(response.getSuccess()).isTrue();
    }
}
```

### Async Event Wait (Awaitility)

```java
@Test
void register_triggersHistoryEvent() {
    // ... perform action ...

    Awaitility.await()
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
            List<History> histories = historyRepository.findByUserId(userId);
            assertThat(histories).hasSize(1);
        });
}
```

## 4. RestDocs Test

API documentation with Spring REST Docs.

```java
// Location: app/docs/{domain}/Rest{Domain}ControllerDocsTest.java
class RestRatingControllerDocsTest extends AbstractRestDocs {

    @MockBean
    private RatingCommandService commandService;

    @MockBean
    private RatingQueryService queryService;

    @Override
    protected Object initController() {
        return new RatingController(commandService, queryService);
    }

    @Test
    @DisplayName("평점 등록 API 문서화")
    void registerRating() throws Exception {
        // given
        given(commandService.register(anyLong(), anyLong(), any()))
            .willReturn(new RatingRegisterResponse(1L));

        mockSecurityContext(1L); // static mock for SecurityContextUtil

        // when & then
        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isOk())
            .andDo(document("rating-register",
                requestFields(
                    fieldWithPath("alcoholId").type(NUMBER).description("주류 ID"),
                    fieldWithPath("rating").type(NUMBER).description("평점")
                ),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("상태 코드"),
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.ratingId").type(NUMBER).description("평점 ID"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta").type(OBJECT).description("메타 정보"),
                    fieldWithPath("meta.serverVersion").type(STRING).description("서버 버전"),
                    fieldWithPath("meta.serverEncoding").type(STRING).description("인코딩"),
                    fieldWithPath("meta.serverResponseTime").type(STRING).description("응답 시간"),
                    fieldWithPath("meta.serverPathVersion").type(STRING).description("API 버전")
                )
            ));
    }
}
```

Note: RestDocs tests use `AbstractRestDocs` which sets up standalone MockMvc with `MockMvcBuilders.standaloneSetup()`, pretty-print configuration, and `GlobalExceptionHandler`. These tests use `@MockBean` for services — this is the one place where mocking is acceptable because we're testing documentation, not business logic.

---

---

## What to Implement

기능 구현 시 어떤 테스트를 만들어야 하는지 기준.

### Required

| 구현 대상 | 테스트 타입 | 태그 | 위치 |
|-----------|-----------|------|------|
| Service (비즈니스 로직) | Unit (Fake/Stub) | `@Tag("unit")` | `{domain}/service/Fake{Domain}ServiceTest.java` |
| Controller (API 엔드포인트) | Integration | `@Tag("integration")` | `{domain}/integration/{Domain}IntegrationTest.java` |

- Unit 테스트는 **반드시** Fake/Stub 패턴으로 작성
- Integration 테스트는 **반드시** `IntegrationTestSupport` 상속

### Optional (사용자 요청 시)

| 구현 대상 | 테스트 타입 | 태그 | 위치 |
|-----------|-----------|------|------|
| API 문서화 | RestDocs | `@Tag("restdocs")` | `app/docs/{domain}/Rest{Domain}ControllerDocsTest.java` |

RestDocs는 사용자가 API 문서화를 요청한 경우에만 작성한다.

### Tag Reference

새 테스트 작성 시 반드시 아래 태그를 사용:

```java
@Tag("unit")              // 단위 테스트 → ./gradlew unit_test
@Tag("integration")       // product 통합 테스트 → ./gradlew integration_test
@Tag("admin_integration") // admin 통합 테스트 → ./gradlew admin_integration_test
@Tag("restdocs")          // API 문서화 테스트 → ./gradlew restDocsTest
```

### Verify Skill 연계

테스트 작성 후 `/verify` 스킬로 검증:
- **구현 중**: `/verify quick` (컴파일 + 아키텍처 규칙 통과 확인)
- **테스트 작성 완료**: `/verify standard` (컴파일 + unit test + build)
- **push 직전**: `/verify full` (전체 CI - integration 포함)

## Test Naming Convention

- Class: `Fake{Feature}ServiceTest`, `{Feature}IntegrationTest`, `Rest{Domain}ControllerDocsTest`
- Method: `{action}_{scenario}_{expectedResult}` or Korean `@DisplayName`
- DisplayName format: `~할 때 ~한다`, `~하면 ~할 수 있다`
