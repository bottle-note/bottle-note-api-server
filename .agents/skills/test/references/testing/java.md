# Testing: java

Production-validated patterns for JUnit 5 + TestContainers + MockMvcTester. Generalized from `bottle-note-api-server`.

## Test Classification (JUnit Tags)

| Tag | Type | Base | Location | Run via |
|-----|------|------|----------|---------|
| `@Tag("unit")` | Unit test | (none, plain JUnit) | `{module}/src/test/java/.../service/` | `./gradlew unit_test` |
| `@Tag("integration")` | Integration test | `IntegrationTestSupport` | `{module}/src/test/java/.../integration/` | `./gradlew integration_test` (Docker) |
| `@Tag("admin_integration")` | Admin integration | `IntegrationTestSupport` | `{admin-module}/src/test/.../integration/` | `./gradlew admin_integration_test` |
| `@Tag("rule")` | Architecture rules | (none, ArchUnit) | `{module}/src/test/.../rule/` | `./gradlew check_rule_test` |
| (none) | RestDocs test | `AbstractRestDocs` | `{module}/src/test/.../docs/` | Document build |

Tag-filtered execution prevents slow integration tests from running with every unit test cycle.

## Naming Convention

- Test class: `Fake{Feature}ServiceTest` (unit), `{Feature}IntegrationTest` (integration), `Rest{Domain}ControllerDocsTest` (RestDocs)
- Method: `{action}_{scenario}_{expectedResult}()` or Korean `@DisplayName`
- `@DisplayName`: Korean `~할 때 ~한다` describing observable behavior

## Shared Test Utilities

### IntegrationTestSupport

Location: `{core-module}/src/test/java/.../IntegrationTestSupport.java`

```java
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {
    @Autowired protected ObjectMapper mapper;
    @Autowired protected MockMvcTester mockMvcTester;          // prefer over legacy MockMvc
    @Autowired protected TestAuthenticationSupport authSupport;
    @Autowired protected DataInitializer dataInitializer;

    @AfterEach
    void cleanup() { dataInitializer.deleteAll(); }

    protected String getToken() { return authSupport.getAccessToken(); }
    protected String getToken(User user) { return authSupport.createToken(user); }
    protected <T> T extractData(MvcTestResult result, Class<T> type) { ... }
}
```

### TestContainersConfig

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {
    @Bean @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName(System.getProperty("testcontainers.db.name", "test"))
            .withReuse(true);                                  // critical: reuse across tests
    }

    @Bean @ServiceConnection
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7.0.12")
            .withExposedPorts(6379)
            .withReuse(true);
    }

    @Bean @Primary FakeWebhookRestTemplate fakeWebhook() { ... }   // captures, never sends
    @Bean @Primary FakeProfanityClient fakeProfanity() { ... }      // bypasses external API
}
```

**`@ServiceConnection`** auto-wires container properties (DB URL, Redis host:port) into Spring context. **`withReuse(true)`** keeps containers warm across test runs (one container, many tests).

### DataInitializer

```java
@Component
public class DataInitializer {
    @PersistenceContext private EntityManager em;
    private List<String> tableNames;

    @PostConstruct
    public void refreshCache() {
        tableNames = em.createNativeQuery("SHOW TABLES").getResultList().stream()
            .map(Object::toString)
            .filter(t -> !t.startsWith("databasechangelog"))
            .filter(t -> !t.startsWith("flyway_"))
            .filter(t -> !t.equals("schema_version"))
            .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAll() {
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        tableNames.forEach(t -> em.createNativeQuery("TRUNCATE TABLE " + t).executeUpdate());
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }
}
```

Dynamic table discovery (no hardcoded list). `REQUIRES_NEW` ensures cleanup runs outside test transaction.

## Pattern 1 — Unit Test (Fake/Stub, PREFERRED)

```java
// Location: {api-module}/src/test/java/.../service/FakeRatingServiceTest.java
@Tag("unit")
@DisplayName("RatingService 단위 테스트")
class FakeRatingServiceTest {

    private RatingService sut;                                     // system under test
    private InMemoryRatingRepository ratingRepository;
    private FakeApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ratingRepository = new InMemoryRatingRepository();
        publisher = new FakeApplicationEventPublisher();
        sut = new RatingService(ratingRepository, publisher);
    }

    @Nested @DisplayName("평점을 등록할 때")
    class RegisterRating {

        @Test @DisplayName("유효한 요청이면 저장한다")
        void register_whenValidRequest_savesRating() {
            // given
            Long alcoholId = 1L, userId = 1L;
            RatingPoint point = RatingPoint.FIVE;

            // when
            RatingRegisterResponse response = sut.register(alcoholId, userId, point);

            // then
            assertThat(response).isNotNull();
            assertThat(ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)).isPresent();
        }

        @Test @DisplayName("이벤트를 발행한다")
        void register_publishesEvent() {
            sut.register(1L, 1L, RatingPoint.FIVE);

            assertThat(publisher.getPublishedEvents())
                .hasSize(1)
                .first().isInstanceOf(RatingRegistryEvent.class);
        }
    }
}
```

### InMemory Repository

```java
// Location: {api-module}/src/test/java/.../{domain}/fixture/InMemory{Domain}Repository.java
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

## Pattern 2 — Unit Test (Mockito, LAST RESORT — ask user first)

Mockito couples tests to implementation details and breaks on refactoring. Use ONLY when Fake/InMemory is genuinely impractical (e.g., complex external integrations).

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {
    @InjectMocks private RatingService sut;
    @Mock private RatingRepository ratingRepository;
    @Mock private AlcoholFacade alcoholFacade;

    @Test @DisplayName("존재하지 않는 주류면 예외")
    void register_whenAlcoholNotFound_throwsException() {
        given(alcoholFacade.existsByAlcoholId(anyLong())).willReturn(false);
        assertThatThrownBy(() -> sut.register(1L, 1L, RatingPoint.FIVE))
            .isInstanceOf(RatingException.class);
    }
}
```

## Pattern 3 — Integration Test (TestContainers + MockMvcTester)

```java
@Tag("integration")
@DisplayName("Rating 통합 테스트")
class RatingIntegrationTest extends IntegrationTestSupport {

    @Autowired private RatingTestFactory ratingTestFactory;
    @Autowired private UserTestFactory userTestFactory;
    @Autowired private AlcoholTestFactory alcoholTestFactory;

    @Test @DisplayName("평점을 등록한다")
    void registerRating() throws Exception {
        // given - real DB via TestFactory
        User user = userTestFactory.persistUser();
        Alcohol alcohol = alcoholTestFactory.persistAlcohol();
        String token = getToken(user);
        RatingRegisterRequest request = new RatingRegisterRequest(alcohol.getId(), 4.5);

        // when - modern MockMvcTester fluent API
        MvcTestResult result = mockMvcTester.post()
            .uri("/api/v1/ratings")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(mapper.writeValueAsString(request))
            .exchange();

        // then
        assertThat(result).hasStatusOk();
        GlobalResponse response = parseResponse(result);
        assertThat(response.getSuccess()).isTrue();
    }
}
```

### TestFactory (integration data setup)

```java
// Location: {core-module}/src/test/java/.../{domain}/fixture/{Domain}TestFactory.java
@Component
public class RatingTestFactory {
    @PersistenceContext private EntityManager em;
    @Autowired private AlcoholTestFactory alcoholTestFactory;       // factory composition
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

    @Transactional
    public Rating persistAndFlush(Long alcoholId, Long userId) { ... }   // for immediate ID
}
```

Factory pattern: `EntityManager` + `@Transactional`. Method naming: `persist{Entity}()`. Compose other factories (e.g., `AlcoholTestFactory` uses `RegionTestFactory`).

### Async Event Verification (Awaitility)

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

## Pattern 4 — RestDocs Test (user request only)

```java
class RestRatingControllerDocsTest extends AbstractRestDocs {

    @MockBean private RatingService service;                         // mocking acceptable HERE

    @Override
    protected Object initController() {
        return new RatingController(service);
    }

    @Test @DisplayName("rating registration API docs")
    void registerRating() throws Exception {
        given(service.register(anyLong(), anyLong(), any()))
            .willReturn(new RatingRegisterResponse(1L));

        mockSecurityContext(1L);                                     // static mock for SecurityContextUtil

        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isOk())
            .andDo(document("rating-register",
                requestFields(
                    fieldWithPath("alcoholId").type(NUMBER).description("alcohol ID"),
                    fieldWithPath("rating").type(NUMBER).description("rating value")
                ),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("success"),
                    fieldWithPath("code").type(NUMBER).description("status code"),
                    fieldWithPath("data.ratingId").type(NUMBER).description("rating ID")
                )
            ));
    }
}
```

**Why `@MockBean` is OK here**: docs tests verify the API contract (request/response shape), not business logic. Service behavior is mocked because the test target is documentation generation.

## Test Data Catalog (Fake/InMemory)

Before creating a new Fake, check the existing catalog:

### InMemory Repositories
- `InMemoryRatingRepository`, `InMemoryReviewRepository`, `InMemoryLikesRepository`
- `InMemoryUserRepository`, `InMemoryUserQueryRepository`
- `InMemoryAlcoholQueryRepository`, `InMemoryFollowRepository`
- Plus block, support, banner, etc.

### Fake Services / Facades
- `FakeAlcoholFacade`, `FakeUserFacade` — `add()`, `remove()`, `clear()` API
- `FakeApplicationEventPublisher` — captures all Spring events; `getPublishedEvents()`, `getPublishedEventsOfType()`, `hasPublishedEventOfType()`, `clear()`
- `FakeHistoryEventPublisher` — domain-specific event captures

### Fake External Services
- `FakeWebhookRestTemplate` — captures HTTP calls (`getCallCount()`, `getLastRequestBody()`)
- `FakeProfanityClient` — returns input text as-is (no external API call)
- Fake JWT / BCrypt — for security testing

## ArchUnit Architecture Rules (@Tag("rule"))

```java
@Tag("rule")
@AnalyzeClasses(packages = "app.bottlenote")
class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule controllers_should_be_in_controller_package =
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule facades_only_use_own_aggregate_repos =
        classes().that().areAnnotatedWith(FacadeService.class)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..{same-aggregate}..", "java..", "org.springframework..");
}
```

## Cleanup Strategy

- **Per-test**: `@AfterEach dataInitializer.deleteAll()` in `IntegrationTestSupport`
- **Filters**: skip `databasechangelog*`, `flyway_*`, `schema_version`, `BATCH_*`, `QRTZ_*`
- **Foreign keys**: temporarily disable `FOREIGN_KEY_CHECKS` for TRUNCATE order independence

## Anti-patterns

- Skipping tests (`@Disabled`, commented out, conditional skip) instead of fixing
- Mockito in unit tests when Fake/InMemory is feasible
- Integration test without `@Tag("integration")` (runs in every unit cycle, slows feedback)
- Hardcoded test data (use TestFactory with `AtomicInteger counter` for uniqueness)
- TestContainers without `withReuse(true)` (cold-start per run, 10x slower)
- `@AfterEach` cleanup that misses tables added later (use dynamic discovery, not hardcoded list)
