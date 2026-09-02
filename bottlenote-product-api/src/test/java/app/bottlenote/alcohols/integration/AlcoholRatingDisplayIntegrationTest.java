package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] 알코올 별점 노출")
class AlcoholRatingDisplayIntegrationTest extends IntegrationTestSupport {

  private static final String EXPLORE_ENDPOINT = "/api/v1/alcohols/explore/standard";

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("목록은 alcoholId별 평균을 소수점 첫째 자리 숫자로 노출한다")
  void 목록은_alcoholId별_평균을_소수점_첫째_자리로_노출한다() throws Exception {
    Alcohol exact = alcoholTestFactory.persistAlcoholWithName("별점노출정수", "Rating Integer");
    Alcohol half = alcoholTestFactory.persistAlcoholWithName("별점노출반점", "Rating Half");
    Alcohol decimal = alcoholTestFactory.persistAlcoholWithName("별점노출소수", "Rating Decimal");
    Alcohol unrated = alcoholTestFactory.persistAlcoholWithName("별점노출없음", "Rating None");
    persistRatings(exact, 4.0, 4.0);
    persistRatings(half, 4.0, 5.0);
    persistRatings(decimal, 4.0, 4.5, 5.0, 5.0);

    MvcTestResult result = getExplore(userTestFactory.persistUser());

    assertThat(ratingOf(result, exact)).isEqualTo(4.0);
    assertThat(ratingOf(result, half)).isEqualTo(4.5);
    assertThat(ratingOf(result, decimal)).isEqualTo(4.6);
    assertThat(ratingOf(result, unrated)).isEqualTo(0.0);
  }

  @Test
  @DisplayName("상세는 같은 평균과 인증 사용자의 리뷰 1건 별점을 기존 JSON 필드에 노출한다")
  void 상세는_같은_평균과_사용자_리뷰_별점을_노출한다() {
    User currentUser = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("상세별점노출", "Detail Rating");
    persistRatings(alcohol, 4.0, 4.5, 5.0, 5.0);
    reviewTestFactory.persistReview(
        Review.builder()
            .userId(currentUser.getId())
            .alcoholId(alcohol.getId())
            .content("사용자 리뷰 별점")
            .reviewRating(3.5));

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/alcohols/{alcoholId}", alcohol.getId())
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken(currentUser).accessToken())
            .with(csrf())
            .exchange();

    result.assertThat().hasStatusOk();
    result.assertThat().bodyJson().extractingPath("$.data.alcohols.rating").isEqualTo(4.6);
    result.assertThat().bodyJson().extractingPath("$.data.alcohols.myRating").isEqualTo(0.0);
    result.assertThat().bodyJson().extractingPath("$.data.alcohols.myAvgRating").isEqualTo(3.5);
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

  private MvcTestResult getExplore(User user) {
    return mockMvcTester
        .get()
        .uri(EXPLORE_ENDPOINT)
        .contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + getToken(user).accessToken())
        .param("keyword", "별점노출")
        .param("sortType", "RATING")
        .param("sortOrder", "DESC")
        .param("size", "10")
        .with(csrf())
        .exchange();
  }

  private double ratingOf(MvcTestResult result, Alcohol alcohol) throws Exception {
    result.assertThat().hasStatusOk();
    List<Number> ratings =
        JsonPath.read(
            result.getMvcResult().getResponse().getContentAsString(),
            "$.data.items[?(@.alcoholId == " + alcohol.getId() + ")].rating");
    assertThat(ratings).hasSize(1);
    return ratings.getFirst().doubleValue();
  }
}
