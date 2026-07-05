package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSearchRequest;
import app.bottlenote.curation.dto.request.CurationUpdateRequest;
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.CurationFeedItemResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.curation.fixture.CurationFixtureFactory;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import app.bottlenote.global.data.response.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminSpecBasedCurationService 단위 테스트")
class AdminSpecBasedCurationServiceTest {

  InMemoryCurationSpecRepository curationSpecRepository;
  InMemoryCurationRepository curationRepository;
  InMemoryCurationExtensionRepository curationExtensionRepository;
  CurationFixtureFactory curationFixtureFactory;
  AdminSpecBasedCurationService adminSpecBasedCurationService;

  @BeforeEach
  void setUp() {
    curationSpecRepository = new InMemoryCurationSpecRepository();
    curationRepository = new InMemoryCurationRepository();
    curationExtensionRepository = new InMemoryCurationExtensionRepository();
    ObjectMapper objectMapper = new ObjectMapper();
    curationFixtureFactory =
        new CurationFixtureFactory(
            curationSpecRepository, curationRepository, curationExtensionRepository);
    adminSpecBasedCurationService =
        new AdminSpecBasedCurationService(
            curationSpecRepository,
            curationRepository,
            curationExtensionRepository,
            new CurationPayloadValidator(objectMapper),
            new CurationResponseMaterializer(
                objectMapper,
                new GraphQLCurationQueryBuilder(),
                new NoopGraphQLCurationExecutor(),
                new CurationPayloadValidator(objectMapper)),
            new CurationFeedProjector(objectMapper));
  }

  @Test
  @DisplayName("큐레이션을 생성하면 imageUrls 첫 번째 이미지를 coverImageUrl에 저장하고 payload를 함께 저장한다")
  void create_이미지와_payload_저장() {
    CurationSpec spec = createSpec();

    var result = adminSpecBasedCurationService.create(createRequest(spec.getId()));

    AdminSpecBasedCurationDetailResponse detail =
        adminSpecBasedCurationService.getDetail(result.targetId());
    assertThat(result.code()).isEqualTo("CURATION_CREATED");
    assertThat(detail.coverImageUrl()).isEqualTo("https://cdn.example.com/cover.jpg");
    assertThat(detail.imageUrls())
        .containsExactly("https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg");
    assertThat(detail.spec().code()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(new ObjectMapper().valueToTree(detail.payload()).path("source").asText())
        .isEqualTo("BOTTLE_NOTE");
  }

  @Test
  @DisplayName("큐레이션 스펙 상세를 조회할 수 있다")
  void getSpecDetail_스펙_상세_조회() {
    CurationSpec spec = createSpec();

    var result = adminSpecBasedCurationService.getSpecDetail(spec.getId());

    assertThat(result.id()).isEqualTo(spec.getId());
    assertThat(result.code()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(result.requestSpec()).containsKey("required");
  }

  @Test
  @DisplayName("payload가 requestSpec required 필드를 만족하지 않으면 예외가 발생한다")
  void create_payload_검증_실패() {
    CurationSpec spec = createSpec();

    assertThatThrownBy(
            () ->
                adminSpecBasedCurationService.create(
                    new CurationCreateRequest(
                        spec.getId(),
                        "잘못된 큐레이션",
                        null,
                        List.of("https://cdn.example.com/cover.jpg"),
                        null,
                        null,
                        0,
                        true,
                        Map.of("source", "BOTTLE_NOTE"))))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_PAYLOAD_INVALID);
  }

  @Test
  @DisplayName("큐레이션을 수정하면 본문과 extension payload를 함께 갱신한다")
  void update_본문과_payload_수정() {
    CurationSpec spec = createSpec();
    Long curationId = adminSpecBasedCurationService.create(createRequest(spec.getId())).targetId();

    var result =
        adminSpecBasedCurationService.update(
            curationId,
            new CurationUpdateRequest(
                spec.getId(),
                "수정된 큐레이션",
                "수정된 설명",
                List.of("https://cdn.example.com/updated.jpg"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                5,
                false,
                Map.of("source", "MANUAL", "alcohol", Map.of("korName", "수동 입력"))));

    AdminSpecBasedCurationDetailResponse detail =
        adminSpecBasedCurationService.getDetail(curationId);
    assertThat(result.code()).isEqualTo("CURATION_UPDATED");
    assertThat(detail.name()).isEqualTo("수정된 큐레이션");
    assertThat(detail.isActive()).isFalse();
    assertThat(detail.imageUrls()).containsExactly("https://cdn.example.com/updated.jpg");
  }

  @Test
  @DisplayName("큐레이션 목록을 조회할 수 있다")
  void search_목록_조회() {
    CurationSpec spec = createSpec();
    adminSpecBasedCurationService.create(createRequest(spec.getId()));

    GlobalResponse result =
        adminSpecBasedCurationService.search(new CurationSearchRequest("", null, 0, 20));

    assertThat(result.getData()).asList().hasSize(1);
  }

  @Test
  @DisplayName("Admin 목록 검색은 code를 spec code exact match로 필터링한다")
  void search_whenCodeProvided_filtersBySpecCode() {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec = createPairingSpec();
    adminSpecBasedCurationService.create(
        createRequest(recommendedSpec.getId(), "추천 큐레이션", 1, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 큐레이션", 2, true));

    GlobalResponse result =
        adminSpecBasedCurationService.search(
            new CurationSearchRequest(null, "WHISKY_PAIRING", null, 0, 20));

    assertThat(result.getData()).asList().hasSize(1);
    assertThat(result.getData()).asList().extracting("specCode").containsExactly("WHISKY_PAIRING");
  }

  @Test
  @DisplayName("Admin 목록 검색은 code와 keyword, isActive 조건을 함께 적용한다")
  void search_whenCodeKeywordAndActiveProvided_combinesFilters() {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec = createPairingSpec();
    adminSpecBasedCurationService.create(createRequest(recommendedSpec.getId(), "추천 매치", 1, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 매치", 2, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 비활성", 3, false));

    GlobalResponse result =
        adminSpecBasedCurationService.search(
            new CurationSearchRequest("매치", "WHISKY_PAIRING", true, 0, 20));

    assertThat(result.getData()).asList().hasSize(1);
    assertThat(result.getData()).asList().extracting("name").containsExactly("페어링 매치");
  }

  @Test
  @DisplayName("Admin 목록 검색은 알 수 없는 code를 빈 결과로 반환한다")
  void search_whenUnknownCodeProvided_returnsEmptyPage() {
    CurationSpec spec = createSpec();
    adminSpecBasedCurationService.create(createRequest(spec.getId()));

    GlobalResponse result =
        adminSpecBasedCurationService.search(
            new CurationSearchRequest(null, "UNKNOWN_CODE", null, 0, 20));

    assertThat(result.getData()).asList().isEmpty();
  }

  @Test
  @DisplayName("Admin 목록 검색은 blank code를 필터 미적용으로 처리한다")
  void search_whenBlankCodeProvided_ignoresCodeFilter() {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec = createPairingSpec();
    adminSpecBasedCurationService.create(
        createRequest(recommendedSpec.getId(), "추천 큐레이션", 1, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 큐레이션", 2, true));

    GlobalResponse result =
        adminSpecBasedCurationService.search(new CurationSearchRequest(null, "   ", null, 0, 20));

    assertThat(result.getData()).asList().hasSize(2);
  }

  @Test
  @DisplayName("Admin feed 검색도 code 필터를 적용한다")
  void searchFeed_whenCodeProvided_filtersBySpecCode() {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec = createPairingSpec();
    adminSpecBasedCurationService.create(
        createRequest(recommendedSpec.getId(), "추천 큐레이션", 1, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 큐레이션", 2, true));

    GlobalResponse result =
        adminSpecBasedCurationService.searchFeed(
            new CurationSearchRequest(null, "WHISKY_PAIRING", null, 0, 10));

    assertThat(result.getData()).asList().hasSize(1);
    assertThat(result.getData()).asList().extracting("specCode").containsExactly("WHISKY_PAIRING");
  }

  @Test
  @DisplayName("Admin feed 검색은 blank code를 필터 미적용으로 처리한다")
  void searchFeed_whenBlankCodeProvided_ignoresCodeFilter() {
    CurationSpec recommendedSpec = createSpec();
    CurationSpec pairingSpec = createPairingSpec();
    adminSpecBasedCurationService.create(
        createRequest(recommendedSpec.getId(), "추천 큐레이션", 1, true));
    adminSpecBasedCurationService.create(createRequest(pairingSpec.getId(), "페어링 큐레이션", 2, true));

    GlobalResponse result =
        adminSpecBasedCurationService.searchFeed(
            new CurationSearchRequest(null, "   ", null, 0, 10));

    assertThat(result.getData()).asList().hasSize(2);
  }

  @Test
  @DisplayName("Admin feed는 페이지 size를 최대 10개로 제한하고 비활성 포함 여부를 필터링한다")
  void searchFeed_페이지_최대_크기_제한과_상태_필터링() {
    CurationSpec spec = createSpec();
    for (int i = 0; i < 12; i++) {
      adminSpecBasedCurationService.create(createRequest(spec.getId(), "활성" + i, i, true));
    }
    adminSpecBasedCurationService.create(createRequest(spec.getId(), "비활성", 0, false));

    GlobalResponse result =
        adminSpecBasedCurationService.searchFeed(new CurationSearchRequest("", true, 0, 30));

    assertThat(result.getData()).asList().hasSize(10);
    assertThat(result.getMeta().get("size")).isEqualTo(10);
  }

  @Test
  @DisplayName("Admin feed는 x-feed 필드만 projection하고 disabled 필드는 제외한다")
  void searchFeed_xFeed_projection() {
    CurationSpec spec = createSpec();
    adminSpecBasedCurationService.create(createRequest(spec.getId()));

    GlobalResponse result =
        adminSpecBasedCurationService.searchFeed(new CurationSearchRequest("", null, 0, 10));

    Object first = ((List<?>) result.getData()).stream().findFirst().orElseThrow();
    assertThat(first).isInstanceOf(CurationFeedItemResponse.class);
    CurationFeedItemResponse item = (CurationFeedItemResponse) first;
    assertThat(item.feedFields().get(0).path()).isEqualTo("alcohol");
  }

  private CurationSpec createSpec() {
    return curationFixtureFactory.saveSpec(
        "RECOMMENDED_WHISKY",
        "추천 위스키",
        null,
        Map.of("type", "object", "required", List.of("source", "alcohol")),
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "source",
                Map.of("type", "string"),
                "alcohol",
                Map.of(
                    "type",
                    "object",
                    "x-feed",
                    Map.of(
                        "enabled",
                        true,
                        "role",
                        "item-list",
                        "order",
                        10,
                        "description",
                        "피드 카드 위스키 정보를 구성하기 위해 포함한다.")),
                "comment",
                Map.of(
                    "type",
                    "string",
                    "x-feed",
                    Map.of(
                        "enabled",
                        false,
                        "role",
                        "description",
                        "order",
                        20,
                        "description",
                        "비활성 메타는 feed 응답에서 제외된다.")))),
        "alcohol",
        1);
  }

  private CurationSpec createPairingSpec() {
    return curationFixtureFactory.saveSpec(
        "WHISKY_PAIRING",
        "위스키 페어링",
        "안주 조합 스펙 설명",
        Map.of("type", "object", "required", List.of("source", "alcohol")),
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "source",
                Map.of("type", "string"),
                "alcohol",
                Map.of(
                    "type",
                    "object",
                    "x-feed",
                    Map.of(
                        "enabled",
                        true,
                        "role",
                        "item-list",
                        "order",
                        10,
                        "description",
                        "피드 카드 페어링 정보를 구성하기 위해 포함한다.")))),
        "alcohol",
        1);
  }

  private CurationCreateRequest createRequest(Long specId) {
    return createRequest(specId, "비 오는 날 위스키", 1, true);
  }

  private CurationCreateRequest createRequest(
      Long specId, String name, int displayOrder, boolean active) {
    return new CurationCreateRequest(
        specId,
        name,
        "스모키 위스키 추천",
        List.of("https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        displayOrder,
        active,
        Map.of("source", "BOTTLE_NOTE", "alcohol", Map.of("korName", "테스트 위스키")));
  }

  private static final class NoopGraphQLCurationExecutor implements GraphQLCurationExecutor {

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, GraphQLCurationQueryBuilder.Result query) {
      return Map.of();
    }
  }
}
