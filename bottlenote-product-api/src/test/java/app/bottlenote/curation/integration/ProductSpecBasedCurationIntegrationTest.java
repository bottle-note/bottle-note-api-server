package app.bottlenote.curation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.service.AdminSpecBasedCurationService;
import app.bottlenote.curation.service.CurationSpecResourceSyncService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] Product Spec Based Curation API 통합 테스트")
class ProductSpecBasedCurationIntegrationTest extends IntegrationTestSupport {

  @Autowired private CurationSpecResourceSyncService curationSpecResourceSyncService;
  @Autowired private CurationSpecRepository curationSpecRepository;
  @Autowired private AdminSpecBasedCurationService adminSpecBasedCurationService;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private AlcoholQueryRepository alcoholRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private PicksTestFactory picksTestFactory;

  private CurationSpec recommendedSpec;
  private CurationSpec tastingEventSpec;

  @BeforeEach
  void setUp() {
    curationSpecResourceSyncService.sync();
    recommendedSpec = curationSpecRepository.findByCode("RECOMMENDED_WHISKY").orElseThrow();
    tastingEventSpec = curationSpecRepository.findByCode("WHISKY_TASTING_EVENT").orElseThrow();
  }

  @Nested
  @DisplayName("큐레이션 스펙 조회 API")
  class ListCurationSpecs {

    @Test
    @DisplayName("Product v2에서 활성 큐레이션 스펙 목록을 조회할 수 있다")
    void listCurationSpecs_returnsMetaOnly() throws Exception {
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curation-specs")
              .contentType(APPLICATION_JSON)
              .exchange();

      JsonNode data = dataNode(result);
      assertThat(data).isNotEmpty();
      assertThat(data.get(0).path("code").asText()).isEqualTo("RECOMMENDED_WHISKY");
      assertThat(data.get(0).has("requestSpec")).isFalse();
      assertThat(data.get(0).has("responseSpec")).isFalse();
      assertThat(data.get(0).has("hydratorKey")).isFalse();
    }

    @Test
    @DisplayName("Product v2에서 큐레이션 스펙 상세를 조회할 수 있다")
    void getCurationSpec_returnsDetailSpec() throws Exception {
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curation-specs/{specId}", recommendedSpec.getId())
              .contentType(APPLICATION_JSON)
              .exchange();

      JsonNode data = dataNode(result);
      assertThat(data.path("code").asText()).isEqualTo("RECOMMENDED_WHISKY");
      assertThat(data.path("hydratorKey").asText()).isEqualTo("alcohol");
      assertThat(data.path("requestSpec").has("type")).isTrue();
      assertThat(data.path("responseSpec").has("properties")).isTrue();
    }

    @Test
    @DisplayName("Product v2에서 존재하지 않는 큐레이션 스펙 상세 조회는 404를 반환한다")
    void getCurationSpec_whenMissing_returnsNotFound() {
      assertThat(mockMvcTester.get().uri("/api/v2/curation-specs/{specId}", 999999L))
          .hasStatus4xxClientError()
          .bodyJson()
          .extractingPath("$.code")
          .isEqualTo(404);
    }
  }

  @Nested
  @DisplayName("큐레이션 목록 조회 API")
  class ListCurations {

    @Test
    @DisplayName("활성이고 노출 기간에 포함된 큐레이션만 displayOrder와 id 순서로 조회할 수 있다")
    void listActiveCurations_whenMixedStatus_returnsOnlyActiveItemsInDisplayOrder()
        throws Exception {
      // given
      Long laterId = createCuration("뒤 큐레이션", 20, true, List.of(manualItem("뒤")));
      createCuration("비활성 큐레이션", 1, false, List.of(manualItem("비활성")));
      createCuration(
          "미노출 큐레이션",
          1,
          true,
          LocalDate.now().plusDays(1),
          LocalDate.now().plusDays(5),
          List.of(manualItem("미노출")));
      createCuration(
          "노출종료 큐레이션",
          1,
          true,
          LocalDate.now().minusDays(5),
          LocalDate.now().minusDays(1),
          List.of(manualItem("노출종료")));
      Long firstId = createCuration("앞 큐레이션", 1, true, List.of(manualItem("앞")));

      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v2/curations").contentType(APPLICATION_JSON).exchange();

      // then
      JsonNode data = dataNode(result);
      assertThat(data).hasSize(2);
      assertThat(data.get(0).path("id").asLong()).isEqualTo(firstId);
      assertThat(data.get(1).path("id").asLong()).isEqualTo(laterId);
      assertThat(data.get(0).path("specCode").asText()).isEqualTo("RECOMMENDED_WHISKY");
      assertThat(data.get(0).path("imageUrls").get(0).asText())
          .isEqualTo("https://cdn.example.com/cover.jpg");
    }

    @Test
    @DisplayName("활성 큐레이션이 없으면 빈 배열을 반환한다")
    void listActiveCurations_whenNoActiveCuration_returnsEmptyArray() throws Exception {
      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v2/curations").contentType(APPLICATION_JSON).exchange();

      // then
      assertThat(dataNode(result)).isEmpty();
    }

    @Test
    @DisplayName("Product feed는 상세 응답 형태에서 spec을 제외하고 x-feed payload 구조를 유지한다")
    void searchFeed_returnsDetailShapeWithoutSpecAndKeepsXFeedPayloadStructure() throws Exception {
      // given
      Long curationId = createCuration("피드 큐레이션", 1, true, List.of(manualItem("피드 위스키")));

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?size=10")
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      JsonNode item = dataNode(result).path("items").get(0);
      JsonNode payloadItem = item.path("payload").get(0);
      assertThat(item.path("id").asLong()).isEqualTo(curationId);
      assertThat(item.has("spec")).isFalse();
      assertThat(item.has("specId")).isFalse();
      assertThat(item.has("feedFields")).isFalse();
      assertThat(item.path("createAt").isMissingNode()).isFalse();
      assertThat(payloadItem.has("source")).isFalse();
      assertThat(payloadItem.has("stats")).isFalse();
      assertThat(payloadItem.path("alcohol").path("korName").asText()).isEqualTo("피드 위스키");
      assertThat(payloadItem.path("comment").asText()).isEqualTo("테스트 코멘트");
    }

    @Test
    @DisplayName("Product feed는 시음회 상세 라인업처럼 x-feed가 없는 배열 payload를 제외한다")
    void searchFeed_whenTastingEventAlcoholsHasNoXFeed_excludesAlcoholsPayload() throws Exception {
      // given
      Long curationId = createTastingEventCuration();

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?size=10")
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      JsonNode item = dataNode(result).path("items").get(0);
      JsonNode payload = item.path("payload");
      assertThat(item.path("id").asLong()).isEqualTo(curationId);
      assertThat(payload.has("alcohols")).isFalse();
      assertThat(payload.path("eventDate").asText()).isEqualTo("2026-06-21");
      assertThat(payload.path("eventTime").asText()).isEqualTo("19:00");
      assertThat(payload.path("isRecruiting").asBoolean()).isTrue();
      assertThat(payload.path("applicationLink").asText()).isEqualTo("https://example.com/apply");
    }

    @Test
    @DisplayName("Product feed는 keyword와 code 조건을 분리해 필터링하고 빈 조건은 전체 조건으로 처리한다")
    void searchFeed_whenKeywordAndCodeProvided_filtersBySearchContract() throws Exception {
      // given
      createCuration("제목 매치 큐레이션", "일반 설명", 1, true, List.of(manualItem("제목")));
      createCuration("일반 큐레이션", "설명 매치 큐레이션", 2, true, List.of(manualItem("설명")));
      Long tastingId = createTastingEventCuration();

      // when
      MvcTestResult keywordResult =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?keyword=큐레이션&size=10")
              .contentType(APPLICATION_JSON)
              .exchange();
      MvcTestResult codeResult =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?code=WHISKY_TASTING_EVENT&size=10")
              .contentType(APPLICATION_JSON)
              .exchange();
      MvcTestResult negativeResult =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?keyword=큐레이션&code=UNKNOWN_CODE&size=10")
              .contentType(APPLICATION_JSON)
              .exchange();
      MvcTestResult boundaryResult =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/feed?keyword=%20%20%20&code=%20%20%20&size=10")
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      assertThat(dataNode(keywordResult).path("items")).hasSize(3);
      assertThat(dataNode(codeResult).path("items")).hasSize(1);
      assertThat(dataNode(codeResult).path("items").get(0).path("id").asLong())
          .isEqualTo(tastingId);
      assertThat(dataNode(negativeResult).path("items")).isEmpty();
      assertThat(dataNode(boundaryResult).path("items")).hasSizeGreaterThanOrEqualTo(3);
    }
  }

  @Nested
  @DisplayName("큐레이션 상세 조회 API")
  class GetCurationDetail {

    @Test
    @DisplayName("BOTTLE_NOTE 항목은 GraphQL로 통계를 보강하고 MANUAL 항목은 stats를 null로 응답한다")
    void getDetail_whenBottleNoteAndManualItems_returnsPayloadMatchingResponseSpec()
        throws Exception {
      // given
      Alcohol alcohol =
          alcoholTestFactory.persistAlcoholWithName("통합 테스트 위스키", "Integration Whisky");
      User userA = userTestFactory.persistUser();
      User userB = userTestFactory.persistUser();
      ratingTestFactory.persistRating(userA, alcohol, 4);
      ratingTestFactory.persistRating(userB, alcohol, 5);
      reviewTestFactory.persistReview(userA, alcohol);
      picksTestFactory.persistPicks(alcohol.getId(), userA.getId());
      Long curationId =
          createCuration(
              "상세 큐레이션", 1, true, List.of(bottleNoteItem(alcohol), manualItem("수동 위스키")));

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/{curationId}", curationId)
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      JsonNode data = dataNode(result);
      JsonNode payload = data.path("payload");
      assertThat(data.path("id").asLong()).isEqualTo(curationId);
      assertThat(data.path("spec").path("code").asText()).isEqualTo("RECOMMENDED_WHISKY");
      assertThat(data.path("spec").path("container").asText()).isEqualTo("array");
      assertThat(payload).hasSize(2);
      assertThat(payload.get(0).path("stats").path("rating").asDouble()).isEqualTo(4.5);
      assertThat(payload.get(0).path("stats").path("totalRatingsCount").asLong()).isEqualTo(2L);
      assertThat(payload.get(0).path("stats").path("reviewCount").asLong()).isEqualTo(1L);
      assertThat(payload.get(0).path("stats").path("totalPickCount").asLong()).isEqualTo(1L);
      assertThat(payload.get(0).path("stats").has("alcoholId")).isFalse();
      assertThat(payload.get(1).path("stats").isNull()).isTrue();
    }

    @Test
    @DisplayName("Product v2에서 알코올 원본 정보가 변경된 후 조회할 경우 큐레이션 payload의 저장 시점 메타 정보로 응답하고 현재 통계를 보강한다")
    void getDetail_whenSourceAlcoholMetadataChanged_returnsSnapshotMetadataAndCurrentStats()
        throws Exception {
      // given
      Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("저장 시점 위스키", "Snapshot Whisky");
      Long curationId = createCuration("스냅샷 큐레이션", 1, true, List.of(bottleNoteItem(alcohol)));
      changeSourceAlcoholMetadata(alcohol);
      assertThat(alcoholRepository.findById(alcohol.getId()).orElseThrow().getKorName())
          .isEqualTo("변경된 원본 위스키");
      User userA = userTestFactory.persistUser();
      User userB = userTestFactory.persistUser();
      ratingTestFactory.persistRating(userA, alcohol, 3);
      ratingTestFactory.persistRating(userB, alcohol, 5);
      reviewTestFactory.persistReview(userA, alcohol);
      picksTestFactory.persistPicks(alcohol.getId(), userA.getId());

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v2/curations/{curationId}", curationId)
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      JsonNode payloadItem = dataNode(result).path("payload").get(0);
      assertThat(payloadItem.path("alcohol").path("korName").asText()).isEqualTo("저장 시점 위스키");
      assertThat(payloadItem.path("alcohol").path("korName").asText()).isNotEqualTo("변경된 원본 위스키");
      assertThat(payloadItem.path("alcohol").path("selectedTags").get(0).asText()).isEqualTo("셰리");
      assertThat(payloadItem.path("stats").path("rating").asDouble()).isEqualTo(4.0);
      assertThat(payloadItem.path("stats").path("totalRatingsCount").asLong()).isEqualTo(2L);
      assertThat(payloadItem.path("stats").path("reviewCount").asLong()).isEqualTo(1L);
      assertThat(payloadItem.path("stats").path("totalPickCount").asLong()).isEqualTo(1L);
      assertThat(payloadItem.path("stats").has("alcoholId")).isFalse();
    }

    @Test
    @DisplayName("비활성 큐레이션 상세 조회는 404를 반환한다")
    void getDetail_whenInactiveCuration_returnsNotFound() {
      // given
      Long curationId = createCuration("비활성 상세", 1, false, List.of(manualItem("비활성")));

      // when & then
      assertThat(mockMvcTester.get().uri("/api/v2/curations/{curationId}", curationId))
          .hasStatus4xxClientError()
          .bodyJson()
          .extractingPath("$.code")
          .isEqualTo(404);
    }

    @Test
    @DisplayName("노출 기간 밖 큐레이션 상세 조회는 404를 반환한다")
    void getDetail_whenOutsideExposureWindow_returnsNotFound() {
      // given
      Long curationId =
          createCuration(
              "미노출 상세",
              1,
              true,
              LocalDate.now().plusDays(1),
              LocalDate.now().plusDays(5),
              List.of(manualItem("미노출")));

      // when & then
      assertThat(mockMvcTester.get().uri("/api/v2/curations/{curationId}", curationId))
          .hasStatus4xxClientError()
          .bodyJson()
          .extractingPath("$.code")
          .isEqualTo(404);
    }

    @Test
    @DisplayName("존재하지 않는 큐레이션 상세 조회는 404를 반환한다")
    void getDetail_whenMissingCuration_returnsNotFound() {
      assertThat(mockMvcTester.get().uri("/api/v2/curations/{curationId}", 999999L))
          .hasStatus4xxClientError()
          .bodyJson()
          .extractingPath("$.code")
          .isEqualTo(404);
    }
  }

  private Long createCuration(
      String name, int displayOrder, boolean active, List<Map<String, Object>> payload) {
    return createCuration(name, "통합 테스트 큐레이션", displayOrder, active, payload);
  }

  private Long createCuration(
      String name,
      String description,
      int displayOrder,
      boolean active,
      List<Map<String, Object>> payload) {
    return createCuration(
        name,
        description,
        displayOrder,
        active,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(1),
        payload);
  }

  private Long createCuration(
      String name,
      int displayOrder,
      boolean active,
      LocalDate exposureStartDate,
      LocalDate exposureEndDate,
      List<Map<String, Object>> payload) {
    return createCuration(
        name, "통합 테스트 큐레이션", displayOrder, active, exposureStartDate, exposureEndDate, payload);
  }

  private Long createCuration(
      String name,
      String description,
      int displayOrder,
      boolean active,
      LocalDate exposureStartDate,
      LocalDate exposureEndDate,
      List<Map<String, Object>> payload) {
    return adminSpecBasedCurationService
        .create(
            new CurationCreateRequest(
                recommendedSpec.getId(),
                name,
                description,
                List.of("https://cdn.example.com/cover.jpg"),
                exposureStartDate,
                exposureEndDate,
                displayOrder,
                active,
                payload))
        .targetId();
  }

  private Long createTastingEventCuration() {
    return adminSpecBasedCurationService
        .create(
            new CurationCreateRequest(
                tastingEventSpec.getId(),
                "시음회 큐레이션",
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
                    List.of(manualItem("시음 위스키")))))
        .targetId();
  }

  private JsonNode dataNode(MvcTestResult result) throws Exception {
    result.assertThat().hasStatusOk();
    GlobalResponse response =
        mapper.readValue(result.getResponse().getContentAsString(), GlobalResponse.class);
    return mapper.valueToTree(response.getData());
  }

  private Map<String, Object> bottleNoteItem(Alcohol alcohol) {
    return item(
        "BOTTLE_NOTE",
        map(
            "alcoholId",
            alcohol.getId(),
            "korName",
            alcohol.getKorName(),
            "selectedTags",
            List.of("셰리", "오크")));
  }

  private void changeSourceAlcoholMetadata(Alcohol alcohol) {
    Alcohol sourceAlcohol = alcoholRepository.findById(alcohol.getId()).orElseThrow();
    sourceAlcohol.update(
        "변경된 원본 위스키",
        "Changed Source Whisky",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "https://cdn.example.com/changed-source.jpg",
        null,
        null);
    alcoholRepository.save(sourceAlcohol);
  }

  private Map<String, Object> manualItem(String name) {
    return item("MANUAL", map("alcoholId", null, "korName", name, "selectedTags", List.of("오크")));
  }

  private Map<String, Object> item(String source, Map<String, Object> alcohol) {
    return map("source", source, "alcohol", alcohol, "comment", "테스트 코멘트");
  }

  private Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
