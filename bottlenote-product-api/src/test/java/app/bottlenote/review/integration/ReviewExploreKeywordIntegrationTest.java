package app.bottlenote.review.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] ReviewExplore keyword 검색")
class ReviewExploreKeywordIntegrationTest extends IntegrationTestSupport {

  private static final String ENDPOINT = "/api/v1/reviews/explore/standard";

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @Test
  @DisplayName("단일 keyword를 토큰으로 분리해 토큰 간 AND와 검색 필드 간 OR로 조회한다")
  void keyword_tokens_match_across_search_fields() {
    User user = userTestFactory.persistUser();
    Alcohol macallan = alcoholTestFactory.persistAlcoholWithName("맥캘란 12년", "Macallan 12yo");
    Alcohol ardbeg = alcoholTestFactory.persistAlcoholWithName("아드벡 10년", "Ardbeg 10yo");
    Review matched = persistReview(user, macallan, "피트 향이 선명하다");
    Review missingAlcoholToken = persistReview(user, ardbeg, "피트 향이 선명하다");

    MvcTestResult result = explore("  피트   맥캘란  ");

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .contains(matched.getId().intValue())
        .doesNotContain(missingAlcoholToken.getId().intValue());
    result
        .assertThat()
        .bodyJson()
        .extractingPath("$.meta.searchParameters.keyword")
        .isEqualTo("피트   맥캘란");
    explore("맥캘란 피트")
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .contains(matched.getId().intValue())
        .doesNotContain(missingAlcoholToken.getId().intValue());
  }

  @ParameterizedTest(name = "keyword={0}")
  @ValueSource(strings = {"%", "_"})
  @DisplayName("LIKE wildcard 문자는 검색 패턴이 아니라 일반 문자로 처리한다")
  void keyword_like_wildcard_is_literal(String wildcard) {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("와일드카드 위스키", "Wildcard Whisky");
    persistReview(user, alcohol, "일반 리뷰 내용");

    MvcTestResult result = explore(wildcard);

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items")
        .asArray()
        .isEmpty();
  }

  @Test
  @DisplayName("동일 keyword와 cursor로 다음 페이지를 중복 없이 조회한다")
  void keyword_cursor_continues_without_duplicates() throws Exception {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("커서 위스키", "Cursor Whisky");
    persistReview(user, alcohol, "커서검색 첫 번째 리뷰");
    persistReview(user, alcohol, "커서검색 두 번째 리뷰");
    Review excluded = persistReview(user, alcohol, "다른 리뷰");

    MvcTestResult first =
        mockMvcTester
            .get()
            .uri(ENDPOINT)
            .param("keyword", "커서검색")
            .param("size", "1")
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();
    String nextCursor =
        com.jayway.jsonpath.JsonPath.read(
            first.getMvcResult().getResponse().getContentAsString(),
            "$.meta.pagination.nextCursor");

    MvcTestResult second =
        mockMvcTester
            .get()
            .uri(ENDPOINT)
            .param("keyword", "커서검색")
            .param("cursor", nextCursor)
            .param("size", "1")
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();

    List<Integer> firstIds =
        com.jayway.jsonpath.JsonPath.read(
            first.getMvcResult().getResponse().getContentAsString(), "$.data.items[*].reviewId");
    second
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .isNotEmpty()
        .doesNotContainAnyElementsOf(firstIds)
        .doesNotContain(excluded.getId().intValue());
  }

  private Review persistReview(User user, Alcohol alcohol, String content) {
    return reviewTestFactory.persistReview(
        user, alcohol, content, SizeType.BOTTLE, BigDecimal.valueOf(50_000));
  }

  private MvcTestResult explore(String keyword) {
    return mockMvcTester
        .get()
        .uri(ENDPOINT)
        .param("keyword", keyword)
        .contentType(APPLICATION_JSON)
        .with(csrf())
        .exchange();
  }
}
