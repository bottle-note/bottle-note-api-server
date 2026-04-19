package app.bottlenote.alcohols.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * 위스키 둘러보기 컨트롤러({@code /api/v1/alcohols/explore/standard}) 통합 테스트.
 *
 * <p>검색({@link AlcoholQueryIntegrationTest})과는 별도 컨트롤러이므로 분리 관리한다. 시나리오별로 {@code @Nested} 그룹을 사용한다.
 */
@Tag("integration")
@DisplayName("[integration] [controller] AlcoholExplore")
class AlcoholExploreControllerIntegrationTest extends IntegrationTestSupport {

  private static final String ENDPOINT = "/api/v1/alcohols/explore/standard";

  @Autowired private AlcoholTestFactory alcoholTestFactory;

  private MvcTestResult exchangeGet(
      java.util.function.Consumer<
              org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder>
          customizer) {
    var builder =
        mockMvcTester
            .get()
            .uri(ENDPOINT)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf());
    customizer.accept(builder);
    return builder.exchange();
  }

  // =============================================================================================
  // 기본 동작
  // =============================================================================================

  @Nested
  @DisplayName("기본 동작")
  class DefaultBehavior {

    @Test
    @DisplayName("기본 호출 시 200과 필수 응답 필드가 반환된다")
    void explore_default() {
      alcoholTestFactory.persistAlcohols(5);

      MvcTestResult result = exchangeGet(b -> {});

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items")
          .asArray()
          .isNotEmpty();
      result.assertThat().bodyJson().extractingPath("$.meta.pageable.hasNext").isNotNull();
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.meta.searchParameters.sortType")
          .isEqualTo("RANDOM");
    }

    @Test
    @DisplayName("매칭 없는 키워드 조회 시 빈 items와 hasNext=false 반환")
    void explore_empty_result() {
      alcoholTestFactory.persistAlcohols(3);

      MvcTestResult result = exchangeGet(b -> b.param("keywords", "존재하지_않는_키워드_9999"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items")
          .asArray()
          .isEmpty();
      result.assertThat().bodyJson().extractingPath("$.meta.pageable.hasNext").isEqualTo(false);
    }

    @Test
    @DisplayName("응답 item에 reviewCount, pickCount 필드가 포함된다")
    void explore_response_includes_count_fields() {
      alcoholTestFactory.persistAlcohols(1);

      MvcTestResult result = exchangeGet(b -> {});

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[0].reviewCount")
          .isNotNull();
      result.assertThat().bodyJson().extractingPath("$.data.items[0].pickCount").isNotNull();
    }
  }

  // =============================================================================================
  // 입력 검증 (400)
  // =============================================================================================

  @Nested
  @DisplayName("입력 검증")
  class InputValidation {

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("잘못된 파라미터는 400을 반환한다")
    @CsvSource(
        textBlock =
            """
            'size 상한 초과 (101)',    size,    101
            'size 하한 미만 (0)',      size,    0
            'cursor 음수 (-1)',       cursor, -1
            """)
    void rejects_invalid_request(String description, String paramName, String paramValue) {
      MvcTestResult result = exchangeGet(b -> b.param(paramName, paramValue));

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    }
  }

  // =============================================================================================
  // 필터
  // =============================================================================================

  @Nested
  @DisplayName("필터")
  class Filters {

    @Test
    @DisplayName("regionIds 복수 OR 필터: 지정되지 않은 지역은 제외된다")
    void filter_by_regionIds() {
      Region regionA = alcoholTestFactory.persistRegion("지역A", "Region A");
      Region regionB = alcoholTestFactory.persistRegion("지역B", "Region B");
      Region regionC = alcoholTestFactory.persistRegion("지역C", "Region C");
      Distillery distillery = alcoholTestFactory.persistDistillery("증류소", "Distillery");
      Alcohol inA = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionA, distillery);
      Alcohol inB = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionB, distillery);
      Alcohol inC = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionC, distillery);

      MvcTestResult result =
          exchangeGet(
              b ->
                  b.param("regionIds", String.valueOf(regionA.getId()))
                      .param("regionIds", String.valueOf(regionB.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactlyInAnyOrder(inA.getId().intValue(), inB.getId().intValue())
          .doesNotContain(inC.getId().intValue());
    }

    @Test
    @DisplayName("distilleryIds 복수 OR 필터: 지정되지 않은 증류소는 제외된다")
    void filter_by_distilleryIds() {
      Region region = alcoholTestFactory.persistRegion("지역A", "Region A");
      Distillery distA = alcoholTestFactory.persistDistillery("디스틸러리A", "Distillery A");
      Distillery distB = alcoholTestFactory.persistDistillery("디스틸러리B", "Distillery B");
      Distillery distC = alcoholTestFactory.persistDistillery("디스틸러리C", "Distillery C");
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distA);
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distB);
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distC);

      MvcTestResult result =
          exchangeGet(
              b ->
                  b.param("distilleryIds", String.valueOf(distA.getId()))
                      .param("distilleryIds", String.valueOf(distB.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].korDistillery")
          .asArray()
          .doesNotContain("디스틸러리C");
    }

    @Test
    @DisplayName("curationId 필터: 큐레이션에 포함된 알코올만 조회된다")
    void filter_by_curationId() {
      Alcohol a1 = alcoholTestFactory.persistAlcoholWithName("큐레이션A1", "Curation A1");
      Alcohol a2 = alcoholTestFactory.persistAlcoholWithName("큐레이션A2", "Curation A2");
      Alcohol excluded = alcoholTestFactory.persistAlcoholWithName("큐레이션외", "Out-of-curation");
      var curation = alcoholTestFactory.persistCurationKeyword("봄 추천 위스키", List.of(a1, a2));

      MvcTestResult result =
          exchangeGet(b -> b.param("curationId", String.valueOf(curation.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(a1.getId().intValue(), a2.getId().intValue())
          .doesNotContain(excluded.getId().intValue());
    }

    @Test
    @DisplayName("keywords 다중 입력 시 AND로 결합된다")
    void filter_keywords_AND() {
      Alcohol both =
          alcoholTestFactory.persistAlcoholWithName("글렌피딕 시그니처", "Glenfiddich Signature");
      Alcohol onlyA = alcoholTestFactory.persistAlcoholWithName("글렌피딕 12", "Glenfiddich 12");
      Alcohol onlyB = alcoholTestFactory.persistAlcoholWithName("맥캘란 시그니처", "Macallan Signature");

      MvcTestResult result =
          exchangeGet(b -> b.param("keywords", "글렌피딕").param("keywords", "시그니처"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(both.getId().intValue())
          .doesNotContain(onlyA.getId().intValue(), onlyB.getId().intValue());
    }
  }

  // =============================================================================================
  // 정렬
  // =============================================================================================

  @Nested
  @DisplayName("정렬")
  class Sort {

    @ParameterizedTest(name = "sortType={0}")
    @DisplayName("sortType 파라미터가 응답 meta.searchParameters.sortType에 반영된다")
    @EnumSource(SearchSortType.class)
    void sort_reflected_in_meta(SearchSortType sortType) {
      alcoholTestFactory.persistAlcohols(3);

      MvcTestResult result =
          exchangeGet(b -> b.param("sortType", sortType.name()).param("sortOrder", "DESC"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.meta.searchParameters.sortType")
          .isEqualTo(sortType.name());
    }
  }

  // =============================================================================================
  // 페이징
  // =============================================================================================

  @Nested
  @DisplayName("페이징")
  class Pagination {

    @Test
    @DisplayName("커서 기반 페이징 시 페이지 간 중복 데이터가 발생하지 않는다")
    void cursor_pagination_no_duplicates() throws Exception {
      alcoholTestFactory.persistAlcohols(15);

      MvcTestResult firstPage =
          exchangeGet(
              b ->
                  b.param("cursor", "0")
                      .param("size", "5")
                      .param("sortType", "RATING")
                      .param("sortOrder", "DESC"));

      MvcTestResult secondPage =
          exchangeGet(
              b ->
                  b.param("cursor", "5")
                      .param("size", "5")
                      .param("sortType", "RATING")
                      .param("sortOrder", "DESC"));

      firstPage.assertThat().hasStatusOk();
      secondPage.assertThat().hasStatusOk();

      // 첫 페이지의 alcoholId 들을 JsonPath로 직접 추출해 두 번째 페이지에 중복되지 않음을 확인
      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              firstPage.getMvcResult().getResponse().getContentAsString(),
              "$.data.items[*].alcoholId");
      secondPage
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .doesNotContainAnyElementsOf(firstIds);
    }
  }
}
