# Test Patterns

## 1. Unit Test - Fake/Stub Pattern (Preferred)

Use InMemory implementations instead of mocks. Closer to real behavior and resilient to refactoring.

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
@DisplayName("{Domain} service unit test")
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
    @DisplayName("when registering a rating")
    class RegisterRating {

        @Test
        @DisplayName("valid request registers the rating")
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
        @DisplayName("event is published")
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

Mockito couples tests to implementation details and breaks on refactoring.
**Always ask the user before choosing Mockito over Fake/Stub.**

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
    @DisplayName("non-existent alcohol throws exception")
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
Read `test-infra.md` for IntegrationTestSupport details.

```java
// Location: {domain}/integration/{Domain}IntegrationTest.java
@Tag("integration")
@DisplayName("{Domain} integration test")
class RatingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RatingTestFactory ratingTestFactory;

    @Test
    @DisplayName("register a rating")
    void registerRating() {
        // given - TestFactory for data setup
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

## 4. RestDocs Test

API documentation with Spring REST Docs. Only implement when user explicitly requests.

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
    @DisplayName("rating registration API docs")
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
                    fieldWithPath("alcoholId").type(NUMBER).description("alcohol ID"),
                    fieldWithPath("rating").type(NUMBER).description("rating value")
                ),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("success"),
                    fieldWithPath("code").type(NUMBER).description("status code"),
                    fieldWithPath("data").type(OBJECT).description("response data"),
                    fieldWithPath("data.ratingId").type(NUMBER).description("rating ID"),
                    fieldWithPath("errors").type(ARRAY).description("error list"),
                    fieldWithPath("meta").type(OBJECT).description("meta info"),
                    fieldWithPath("meta.serverVersion").type(STRING).description("server version"),
                    fieldWithPath("meta.serverEncoding").type(STRING).description("encoding"),
                    fieldWithPath("meta.serverResponseTime").type(STRING).description("response time"),
                    fieldWithPath("meta.serverPathVersion").type(STRING).description("API version")
                )
            ));
    }
}
```

Note: RestDocs tests use `AbstractRestDocs` which sets up standalone MockMvc with `MockMvcBuilders.standaloneSetup()`, pretty-print configuration, and `GlobalExceptionHandler`. These tests use `@MockBean` for services - this is the one place where mocking is acceptable because we're testing documentation, not business logic.
