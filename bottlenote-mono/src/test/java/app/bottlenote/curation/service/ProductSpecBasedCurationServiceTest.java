package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSpecCreateRequest;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
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
  CurationV2Service curationV2Service;
  ProductSpecBasedCurationService productService;

  @BeforeEach
  void setUp() {
    specRepository = new InMemoryCurationSpecRepository();
    curationRepository = new InMemoryCurationRepository();
    extensionRepository = new InMemoryCurationExtensionRepository();
    curationV2Service =
        new CurationV2Service(specRepository, curationRepository, extensionRepository);
    CurationResponseMaterializer materializer =
        new CurationResponseMaterializer(
            OBJECT_MAPPER,
            new SpecGraphqlQueryBuilder(),
            new FixedGraphqlExecutor(),
            new CurationPayloadValidator(OBJECT_MAPPER));
    productService =
        new ProductSpecBasedCurationService(
            curationRepository, specRepository, extensionRepository, materializer);
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
    return curationV2Service.createSpec(
        new CurationSpecCreateRequest(
            "RECOMMENDED_WHISKY",
            "추천 위스키",
            "추천 설명",
            schema("recommended_whisky.json", "Request"),
            schema("recommended_whisky.json", "Response"),
            "alcohol",
            2));
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
    return curationV2Service
        .createCuration(
            new CurationCreateRequest(
                specId,
                name,
                "설명",
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

  private static final class FixedGraphqlExecutor implements CurationGraphqlExecutor {

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, SpecGraphqlQueryBuilder.Result query) {
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
