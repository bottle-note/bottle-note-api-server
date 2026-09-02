package app.bottlenote.history.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.fixture.AlcoholsViewHistoryTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] 주류 조회 이력 인기도")
class ViewHistoryPopularityIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private AlcoholsViewHistoryTestFactory viewHistoryTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("조회 이력은 역사상 legacy 최고점이 아니라 최신 HOUR Snapshot 점수를 반환한다")
  void viewHistory_returnsLatestHourlyPopularitySnapshot() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    LocalDateTime currentBucket = BucketGranularity.HOUR.startAt(LocalDateTime.now());
    viewHistoryTestFactory.persistAlcoholsViewHistory(
        user.getId(), alcohol.getId(), LocalDateTime.now());
    alcoholTestFactory.persistPopularAlcohol(alcohol.getId(), new BigDecimal("0.9"));
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(),
        BucketGranularity.HOUR,
        currentBucket.minusHours(1),
        BigDecimal.ZERO,
        new BigDecimal("0.8"));
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(),
        BucketGranularity.HOUR,
        currentBucket,
        BigDecimal.ZERO,
        new BigDecimal("0.4"));

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/history/view/alcohols")
            .param("size", "10")
            .header("Authorization", "Bearer " + getToken(user).accessToken())
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[0].popularScore")
        .isEqualTo(0.4);
  }

  @Test
  @DisplayName("조회 이력은 평균 별점을 소수점 첫째 자리로 노출한다")
  void viewHistory_displaysRatingToOneDecimalPlace() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    viewHistoryTestFactory.persistAlcoholsViewHistory(
        user.getId(), alcohol.getId(), LocalDateTime.now());
    for (double point : List.of(4.0, 4.5, 5.0, 5.0)) {
      ratingTestFactory.persistRating(userTestFactory.persistUser(), alcohol, point);
    }

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/history/view/alcohols")
            .param("size", "10")
            .header("Authorization", "Bearer " + getToken(user).accessToken())
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();

    result
        .assertThat()
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.data.items[0].rating")
        .isEqualTo(4.6);
  }
}
