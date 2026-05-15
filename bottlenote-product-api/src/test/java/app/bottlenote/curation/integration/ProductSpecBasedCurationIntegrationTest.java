package app.bottlenote.curation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
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
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private PicksTestFactory picksTestFactory;

  private CurationSpec recommendedSpec;

  @BeforeEach
  void setUp() {
    curationSpecResourceSyncService.sync();
    recommendedSpec = curationSpecRepository.findByCode("RECOMMENDED_WHISKY").orElseThrow();
  }

  @Nested
  @DisplayName("큐레이션 목록 조회 API")
  class ListCurations {

    @Test
    @DisplayName("활성 큐레이션만 displayOrder와 id 순서로 조회할 수 있다")
    void listActiveCurations_whenMixedStatus_returnsOnlyActiveItemsInDisplayOrder()
        throws Exception {
      // given
      Long laterId = createCuration("뒤 큐레이션", 20, true, List.of(manualItem("뒤")));
      createCuration("비활성 큐레이션", 1, false, List.of(manualItem("비활성")));
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
    return adminSpecBasedCurationService
        .create(
            new CurationCreateRequest(
                recommendedSpec.getId(),
                name,
                "통합 테스트 큐레이션",
                List.of("https://cdn.example.com/cover.jpg"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                displayOrder,
                active,
                payload))
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
