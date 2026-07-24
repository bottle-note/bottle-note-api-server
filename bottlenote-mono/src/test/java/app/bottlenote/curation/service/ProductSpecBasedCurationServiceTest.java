package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.curation.fixture.CurationFixtureFactory;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("ProductSpecBasedCurationService 단위 테스트")
class ProductSpecBasedCurationServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  InMemoryCurationSpecRepository specRepository;
  InMemoryCurationRepository curationRepository;
  InMemoryCurationExtensionRepository extensionRepository;
  CurationFixtureFactory curationFixtureFactory;
  ProductSpecBasedCurationService productService;

  @BeforeEach
  void setUp() {
    specRepository = new InMemoryCurationSpecRepository();
    curationRepository = new InMemoryCurationRepository();
    extensionRepository = new InMemoryCurationExtensionRepository();
    curationFixtureFactory =
        new CurationFixtureFactory(specRepository, curationRepository, extensionRepository);
    CurationResponseMaterializer materializer =
        new CurationResponseMaterializer(
            OBJECT_MAPPER,
            new GraphQLCurationQueryBuilder(),
            new FixedGraphQLCurationExecutor(),
            new CurationPayloadValidator(OBJECT_MAPPER));
    productService =
        new ProductSpecBasedCurationService(
            curationRepository,
            specRepository,
            extensionRepository,
            materializer,
            new CurationFeedProjector(OBJECT_MAPPER));
  }

  @Test
  @DisplayName("Product v2 목록은 활성이고 노출 기간에 포함된 큐레이션만 displayOrder, id 순서로 반환한다")
  void listActiveCurations_whenMixedStatus_returnsOnlyActiveItemsInDisplayOrder()
      throws IOException {
    CurationSpec spec = createSpec();
    Long laterId = createCuration(spec.getId(), "뒤", 20, true);
    createCuration(spec.getId(), "비활성", 1, false);
    createCuration(
        spec.getId(), "미노출", 1, true, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
    createCuration(
        spec.getId(), "노출종료", 1, true, LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));
    Long firstId = createCuration(spec.getId(), "앞", 1, true);

    var result = productService.listActiveCurations();

    assertThat(result).hasSize(2);
    assertThat(result).extracting("id").containsExactly(firstId, laterId);
    assertThat(result.get(0).specCode()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(result.get(0).imageUrls()).containsExactly("https://cdn.example.com/cover.jpg");
  }

  @Test
  @DisplayName("Product v2 상세는 spec meta와 responseSpec 기준으로 stats가 보강된 payload를 반환한다")
  void getDetail_whenCurationExists_returnsMaterializedPayload() throws IOException {
    CurationSpec spec = createSpec();
    Long curationId = createCuration(spec.getId(), "상세", 1, true);

    ProductSpecBasedCurationDetailResponse result = productService.getDetail(curationId);

    JsonNode payload = OBJECT_MAPPER.valueToTree(result.payload());
    assertThat(result.spec().code()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(result.spec().container()).isEqualTo("array");
    assertThat(result.spec().responseSpec()).containsKey("properties");
    assertThat(payload.get(0).path("stats").path("totalPickCount").asInt()).isEqualTo(8);
    assertThat(payload.get(0).path("stats").has("alcoholId")).isFalse();
    assertThat(payload.get(1).path("stats").isNull()).isTrue();
  }

  @Test
  @DisplayName("비활성 큐레이션 상세 조회는 Product v2에서 찾을 수 없다")
  void getDetail_whenCurationInactive_throwsNotFound() throws IOException {
    CurationSpec spec = createSpec();
    Long curationId = createCuration(spec.getId(), "비활성", 1, false);

    assertThatThrownBy(() -> productService.getDetail(curationId))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", CurationExceptionCode.CURATION_NOT_FOUND);
  }

  @Test
  @DisplayName("Product feed는 상세 응답에서 spec만 제외하고 payload 구조를 유지한 채 x-feed 필드만 반환한다")
  void searchFeed_whenXFeedExists_returnsDetailShapeWithFilteredPayload() throws IOException {
    CurationSpec spec = createSpec();
    createCuration(spec.getId(), "피드", 1, true);

    var result = productService.searchFeed(null, List.of(spec.getCode()), 0L, 20);

    assertThat(result.items()).hasSize(1);
    assertThat(result.pageable().getPageSize()).isEqualTo(10L);
    assertThat(result.items().get(0).name()).isEqualTo("피드");
    assertThat(result.items().get(0)).hasNoNullFieldsOrPropertiesExcept("description", "createAt");
    JsonNode payload = OBJECT_MAPPER.valueToTree(result.items().get(0).payload());
    assertThat(payload).hasSize(2);
    assertThat(payload.get(0).has("source")).isFalse();
    assertThat(payload.get(0).has("stats")).isFalse();
    assertThat(payload.get(0).path("alcohol").path("korName").asText()).isEqualTo("테스트");
    assertThat(payload.get(0).path("comment").isNull()).isTrue();
    assertThat(payload.get(1).path("alcohol").path("korName").asText()).isEqualTo("수동");
    assertThat(payload.get(1).has("source")).isFalse();
  }

  @Test
  @DisplayName("Product feed는 시음회 라인업처럼 x-feed가 없는 배열 필드를 payload에서 완전히 제외한다")
  void searchFeed_whenArrayFieldHasNoXFeedChildren_excludesArrayField() throws IOException {
    CurationSpec spec = createTastingEventSpec();
    Long curationId = createTastingEventCuration(spec.getId());

    var result = productService.searchFeed(null, List.of(spec.getCode()), 0L, 20);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).id()).isEqualTo(curationId);
    JsonNode payload = OBJECT_MAPPER.valueToTree(result.items().get(0).payload());
    assertThat(payload.has("alcohols")).isFalse();
    assertThat(payload.path("eventDate").asText()).isEqualTo("2026-06-21");
    assertThat(payload.path("eventTime").asText()).isEqualTo("19:00");
    assertThat(payload.path("isRecruiting").asBoolean()).isTrue();
    assertThat(payload.path("applicationLink").asText()).isEqualTo("https://example.com/apply");
  }

  @Test
  @DisplayName("Product 상세는 시음회 라인업 배열을 payload에 유지한다")
  void getDetail_whenTastingEventContainsAlcohols_keepsAlcoholsPayload() throws IOException {
    CurationSpec spec = createTastingEventSpec();
    Long curationId = createTastingEventCuration(spec.getId());

    var result = productService.getDetail(curationId);

    JsonNode payload = OBJECT_MAPPER.valueToTree(result.payload());
    assertThat(payload.has("alcohols")).isTrue();
    assertThat(payload.path("alcohols")).hasSize(1);
    assertThat(payload.path("alcohols").get(0).path("alcohol").path("korName").asText())
        .isEqualTo("글렌드로낙 오리지널 12년");
  }

  @Test
  @DisplayName("Product feed는 비활성/미노출 큐레이션을 제외하고 cursor size를 최대 10개로 제한한다")
  void searchFeed_whenSizeExceedsLimit_capsToTenAndKeepsVisibility() throws IOException {
    CurationSpec spec = createSpec();
    for (int i = 0; i < 12; i++) {
      createCuration(spec.getId(), "노출" + i, i, true);
    }
    createCuration(spec.getId(), "비활성", 1, false);
    createCuration(
        spec.getId(), "미노출", 1, true, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

    var result = productService.searchFeed(null, List.of(spec.getCode()), 0L, 30);

    assertThat(result.items()).hasSize(10);
    assertThat(result.pageable().getPageSize()).isEqualTo(10L);
    assertThat(result.pageable().getCurrentCursor()).isZero();
    assertThat(result.pageable().getCursor()).isEqualTo(10L);
    assertThat(result.pageable().getHasNext()).isTrue();
  }

  @Test
  @DisplayName(
      "Product feed 검색은 keyword를 큐레이션 제목/설명과 스펙 제목/설명에 LIKE 적용하고 code는 별도 exact match로 필터링한다")
  void searchFeed_whenKeywordAndCodeProvided_filtersBeforeCursorPagination() throws IOException {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec =
        curationFixtureFactory.saveSpec(
            "WHISKY_PAIRING",
            "위스키 페어링",
            "안주 조합 스펙 설명",
            schema("whisky_pairing.json", "Request"),
            schema("whisky_pairing.json", "Response"),
            "alcohol",
            1);
    createCuration(recommendedSpec.getId(), "큐레이션 제목 매치", "일반 설명", 1, true);
    createCuration(recommendedSpec.getId(), "일반 제목", "큐레이션 설명 매치", 2, true);
    createCuration(pairingSpec.getId(), "일반 제목", "일반 설명", 3, true);
    createCuration(pairingSpec.getId(), "큐레이션 제목 매치 페어링", "일반 설명", 4, true);

    List<String> allCodes = List.of("RECOMMENDED_WHISKY", "WHISKY_PAIRING");
    var keywordResult = productService.searchFeed("큐레이션", allCodes, 0L, 10);
    var specKeywordResult = productService.searchFeed("안주", allCodes, 0L, 10);
    var codeResult = productService.searchFeed(null, List.of("WHISKY_PAIRING"), 0L, 10);
    var multiCodeResult = productService.searchFeed(null, allCodes, 0L, 10);
    var combinedResult = productService.searchFeed("큐레이션", List.of("WHISKY_PAIRING"), 0L, 10);
    var negativeResult = productService.searchFeed("큐레이션", List.of("UNKNOWN_CODE"), 0L, 10);
    var emptyCodeResult = productService.searchFeed(null, List.of(), 0L, 10);

    assertThat(keywordResult.items())
        .extracting("name")
        .containsExactly("큐레이션 제목 매치", "일반 제목", "큐레이션 제목 매치 페어링");
    assertThat(specKeywordResult.items())
        .extracting("name")
        .containsExactly("일반 제목", "큐레이션 제목 매치 페어링");
    assertThat(codeResult.items()).extracting("name").containsExactly("일반 제목", "큐레이션 제목 매치 페어링");
    assertThat(multiCodeResult.items())
        .extracting("name")
        .containsExactly("큐레이션 제목 매치", "일반 제목", "일반 제목", "큐레이션 제목 매치 페어링");
    assertThat(combinedResult.items()).extracting("name").containsExactly("큐레이션 제목 매치 페어링");
    assertThat(negativeResult.items()).isEmpty();
    assertThat(emptyCodeResult.items()).isEmpty();
  }

  @Test
  @DisplayName("노출 기간 밖 큐레이션 상세 조회는 Product v2에서 찾을 수 없다")
  void getDetail_whenCurationOutsideExposureWindow_throwsNotFound() throws IOException {
    CurationSpec spec = createSpec();
    Long curationId =
        createCuration(
            spec.getId(), "미노출", 1, true, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

    assertThatThrownBy(() -> productService.getDetail(curationId))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", CurationExceptionCode.CURATION_NOT_FOUND);
  }

  private CurationSpec createSpec() throws IOException {
    Map<String, Object> responseSpec = schema("recommended_whisky.json", "Response");
    Map<String, Object> properties = castMap(responseSpec.get("properties"));
    castMap(properties.get("alcohol"))
        .put(
            "x-feed",
            map(
                "enabled",
                true,
                "role",
                "item-list",
                "order",
                10,
                "description",
                "피드 카드에서 위스키 목록을 구성하기 위해 포함한다."));
    castMap(properties.get("comment"))
        .put(
            "x-feed",
            map(
                "enabled",
                true,
                "role",
                "description",
                "order",
                20,
                "description",
                "피드 카드 설명 문구를 구성하기 위해 포함한다."));
    return curationFixtureFactory.saveSpec(
        "RECOMMENDED_WHISKY",
        "추천 위스키",
        "추천 설명",
        schema("recommended_whisky.json", "Request"),
        responseSpec,
        "alcohol",
        2);
  }

  private Long createCuration(Long specId, String name, int displayOrder, boolean active) {
    return createCuration(
        specId,
        name,
        displayOrder,
        active,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(1));
  }

  private Long createCuration(
      Long specId,
      String name,
      int displayOrder,
      boolean active,
      LocalDate exposureStartDate,
      LocalDate exposureEndDate) {
    return createCuration(
        specId, name, "설명", displayOrder, active, exposureStartDate, exposureEndDate);
  }

  private Long createCuration(
      Long specId, String name, String description, int displayOrder, boolean active) {
    return createCuration(
        specId,
        name,
        description,
        displayOrder,
        active,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(1));
  }

  private Long createCuration(
      Long specId,
      String name,
      String description,
      int displayOrder,
      boolean active,
      LocalDate exposureStartDate,
      LocalDate exposureEndDate) {
    return curationFixtureFactory
        .saveCuration(
            new CurationCreateRequest(
                specId,
                name,
                description,
                List.of("https://cdn.example.com/cover.jpg"),
                exposureStartDate,
                exposureEndDate,
                displayOrder,
                active,
                List.of(
                    item(
                        "BOTTLE_NOTE",
                        map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리"))),
                    item(
                        "MANUAL",
                        map("alcoholId", null, "korName", "수동", "selectedTags", List.of("오크"))))))
        .getId();
  }

  private CurationSpec createTastingEventSpec() throws IOException {
    return curationFixtureFactory.saveSpec(
        "WHISKY_TASTING_EVENT",
        "위스키 시음회",
        "시음회 설명",
        schema("whisky_tasting_event.json", "Request"),
        schema("whisky_tasting_event.json", "Response"),
        "alcohol",
        1);
  }

  private Long createTastingEventCuration(Long specId) {
    return curationFixtureFactory
        .saveCuration(
            new CurationCreateRequest(
                specId,
                "시음회",
                "시음회 설명",
                List.of("https://cdn.example.com/tasting.jpg"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                1,
                true,
                map(
                    "eventDate",
                    "2026-06-21",
                    "eventTime",
                    "19:00",
                    "barAddress",
                    "서울시 강남구 테스트로 1",
                    "detailAddress",
                    "2층",
                    "isRecruiting",
                    true,
                    "entryFee",
                    50000,
                    "capacity",
                    12,
                    "applicationLink",
                    "https://example.com/apply",
                    "guideText",
                    "시음회 안내",
                    "alcohols",
                    List.of(
                        item(
                            "BOTTLE_NOTE",
                            map(
                                "alcoholId",
                                1,
                                "korName",
                                "글렌드로낙 오리지널 12년",
                                "selectedTags",
                                List.of("셰리")))))))
        .getId();
  }

  private static Map<String, Object> schema(String resourceName, String suffix) throws IOException {
    JsonNode root =
        OBJECT_MAPPER.readTree(
            new ClassPathResource("openapi/curation/" + resourceName).getInputStream());
    JsonNode schemas = root.path("components").path("schemas");
    JsonNode schema =
        schemas.properties().stream()
            .filter(entry -> entry.getKey().endsWith(suffix))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow();
    return OBJECT_MAPPER.convertValue(schema, MAP_TYPE);
  }

  private static Map<String, Object> item(String source, Map<String, Object> alcohol) {
    return map("source", source, "alcohol", alcohol, "comment", null);
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  private static final class FixedGraphQLCurationExecutor implements GraphQLCurationExecutor {

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, GraphQLCurationQueryBuilder.Result query) {
      return map(
          "data",
          map(
              query.entryField(),
              List.of(
                  map(
                      "alcoholId",
                      1,
                      "rating",
                      4.2,
                      "totalRatingsCount",
                      10,
                      "reviewCount",
                      3,
                      "totalPickCount",
                      8))));
    }
  }
}
