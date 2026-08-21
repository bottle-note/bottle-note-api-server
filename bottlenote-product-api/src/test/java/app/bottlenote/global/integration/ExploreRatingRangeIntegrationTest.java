package app.bottlenote.global.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] 둘러보기 별점 범위 필터")
class ExploreRatingRangeIntegrationTest extends IntegrationTestSupport {

  private static final String ALCOHOL_ENDPOINT = "/api/v1/alcohols/explore/standard";
  private static final String REVIEW_ENDPOINT = "/api/v1/reviews/explore/standard";

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("위스키 둘러보기는 표시 평점이 ratingFrom과 ratingTo 사이인 항목을 경계 포함 조회한다")
  void 위스키_둘러보기는_표시_평점의_포함_범위를_조회한다() {
    User user = userTestFactory.persistUser();
    Alcohol below = alcoholTestFactory.persistAlcoholWithName("범위위스키하한밖", "Range Below");
    Alcohol lower = alcoholTestFactory.persistAlcoholWithName("범위위스키하한", "Range Lower");
    Alcohol upper = alcoholTestFactory.persistAlcoholWithName("범위위스키상한", "Range Upper");
    Alcohol above = alcoholTestFactory.persistAlcoholWithName("범위위스키상한밖", "Range Above");
    Alcohol unrated = alcoholTestFactory.persistAlcoholWithName("범위위스키평점없음", "Range Unrated");
    persistRating(user, below, 2.5);
    persistRating(user, lower, 3.0);
    persistRating(user, upper, 4.0);
    persistRating(user, above, 4.5);

    MvcTestResult result =
        get(ALCOHOL_ENDPOINT, "keywords", "범위위스키", "ratingFrom", "3.0", "ratingTo", "4.0");

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].alcoholId")
        .asArray()
        .containsExactlyInAnyOrder(lower.getId().intValue(), upper.getId().intValue())
        .doesNotContain(
            below.getId().intValue(), above.getId().intValue(), unrated.getId().intValue());
  }

  @Test
  @DisplayName("리뷰 둘러보기는 작성 별점이 ratingFrom과 ratingTo 사이인 리뷰를 경계 포함 조회한다")
  void 리뷰_둘러보기는_작성_별점의_포함_범위를_조회한다() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review below = persistReview(user, alcohol, "범위리뷰 하한 밖", 2.5);
    Review lower = persistReview(user, alcohol, "범위리뷰 하한", 3.0);
    Review upper = persistReview(user, alcohol, "범위리뷰 상한", 4.0);
    Review above = persistReview(user, alcohol, "범위리뷰 상한 밖", 4.5);

    MvcTestResult result =
        get(REVIEW_ENDPOINT, "keyword", "범위리뷰", "ratingFrom", "3.0", "ratingTo", "4.0");

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .containsExactlyInAnyOrder(lower.getId().intValue(), upper.getId().intValue())
        .doesNotContain(below.getId().intValue(), above.getId().intValue());
  }

  @Test
  @DisplayName("ratingFrom만 보내면 해당 별점 이상을 조회하고 ratingTo만 보내면 이하를 조회한다")
  void 단일_경계도_포함_범위로_조회한다() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review low = persistReview(user, alcohol, "단일경계리뷰 낮음", 2.5);
    Review high = persistReview(user, alcohol, "단일경계리뷰 높음", 4.5);

    MvcTestResult fromResult =
        get(REVIEW_ENDPOINT, "keyword", "단일경계리뷰", "ratingFrom", "4.0");
    MvcTestResult toResult = get(REVIEW_ENDPOINT, "keyword", "단일경계리뷰", "ratingTo", "3.0");

    fromResult
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .containsExactly(high.getId().intValue());
    toResult
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[*].reviewId")
        .asArray()
        .containsExactly(low.getId().intValue());
  }

  @Test
  @DisplayName("별점 범위가 역전되거나 0.5 단위가 아니면 400과 EXPLORE_RATING_INVALID를 반환한다")
  void 유효하지_않은_별점_범위는_400을_반환한다() {
    MvcTestResult reversed = get(REVIEW_ENDPOINT, "ratingFrom", "4.0", "ratingTo", "3.5");
    MvcTestResult invalidStep = get(ALCOHOL_ENDPOINT, "ratingFrom", "3.2");

    assertInvalidRating(reversed);
    assertInvalidRating(invalidStep);
  }

  private void persistRating(User user, Alcohol alcohol, double rating) {
    ratingTestFactory.persistRating(
        Rating.builder()
            .id(Rating.RatingId.is(user.getId(), alcohol.getId()))
            .ratingPoint(RatingPoint.of(rating)));
  }

  private Review persistReview(
      User user, Alcohol alcohol, String content, double reviewRating) {
    return reviewTestFactory.persistReview(
        Review.builder()
            .userId(user.getId())
            .alcoholId(alcohol.getId())
            .content(content)
            .reviewRating(reviewRating));
  }

  private MvcTestResult get(String endpoint, String... parameters) {
    var builder = mockMvcTester.get().uri(endpoint).contentType(APPLICATION_JSON).with(csrf());
    for (int index = 0; index < parameters.length; index += 2) {
      builder.param(parameters[index], parameters[index + 1]);
    }
    return builder.exchange();
  }

  private void assertInvalidRating(MvcTestResult result) {
    result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    result
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[*].code")
        .asArray()
        .contains("EXPLORE_RATING_INVALID");
  }
}
