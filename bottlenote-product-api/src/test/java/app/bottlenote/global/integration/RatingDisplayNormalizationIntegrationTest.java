package app.bottlenote.global.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] 별점 노출 정규화")
class RatingDisplayNormalizationIntegrationTest extends IntegrationTestSupport {

  private static final String DETAIL_ENDPOINT = "/api/v1/alcohols/{alcoholId}";
  private static final String EXPLORE_ENDPOINT = "/api/v1/alcohols/explore/standard";

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("집계 평점이 0.75일 때 0.5 단위로 올리지 않고 0.8로 노출한다")
  void 집계_평점을_소수점_첫째_자리로_노출한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    persistRatings(alcohol, 0.5, 1.0);

    assertDisplayedRating(alcohol, 0.8);
  }

  @Test
  @DisplayName("반올림 경계인 1.25는 half-up으로 1.3에 노출한다")
  void 반올림_경계는_올림한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    persistRatings(alcohol, 1.0, 1.5);

    assertDisplayedRating(alcohol, 1.3);
  }

  @Test
  @DisplayName("이미 소수점 첫째 자리인 4.6은 4.5나 5.0으로 바뀌지 않는다")
  void 첫째_자리_값은_그대로_노출한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    persistRatings(alcohol, 4.5, 4.5, 4.5, 4.5, 5.0);

    assertDisplayedRating(alcohol, 4.6);
  }

  @Test
  @DisplayName("정수에 해당하는 집계는 4.0 형태로 노출한다")
  void 정수_집계도_한_자리로_노출한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    persistRatings(alcohol, 3.5, 4.5);

    assertDisplayedRating(alcohol, 4.0);
  }

  @Test
  @DisplayName("0점 별점은 노출 평균과 참여자 수에서 모두 제외한다")
  void 영점_별점은_집계에서_제외한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    persistRatings(alcohol, 0.0, 4.0);

    MvcTestResult result = getDetail(alcohol);

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.alcohols.rating")
        .isEqualTo(4.0);
    result.assertThat().bodyJson().extractingPath("$.data.alcohols.totalRatingsCount").isEqualTo(1);
  }

  @Test
  @DisplayName("둘러보기 목록의 표시 평점은 상세와 같은 값이다")
  void 목록과_상세의_표시_평점이_같다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("정규화표시위스키", "Normalized Display");
    persistRatings(alcohol, 0.5, 1.0);

    MvcTestResult explore =
        mockMvcTester
            .get()
            .uri(EXPLORE_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .param("keyword", "정규화표시위스키")
            .exchange();

    explore
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[0].rating")
        .isEqualTo(0.8);
    getDetail(alcohol)
        .assertThat()
        .bodyJson()
        .extractingPath("$.data.alcohols.rating")
        .isEqualTo(0.8);
  }

  private void assertDisplayedRating(Alcohol alcohol, double expected) {
    getDetail(alcohol)
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.alcohols.rating")
        .isEqualTo(expected);
  }

  private MvcTestResult getDetail(Alcohol alcohol) {
    return mockMvcTester
        .get()
        .uri(DETAIL_ENDPOINT, alcohol.getId())
        .contentType(APPLICATION_JSON)
        .with(csrf())
        .exchange();
  }

  private void persistRatings(Alcohol alcohol, double... points) {
    for (double point : points) {
      User user = userTestFactory.persistUser();
      ratingTestFactory.persistRating(
          Rating.builder()
              .id(Rating.RatingId.is(user.getId(), alcohol.getId()))
              .ratingPoint(RatingPoint.of(point)));
    }
  }
}
