package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] Popular API")
class PopularIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Nested
  @DisplayName("주간 인기 API")
  class WeeklyPopularApi {

    @Test
    @DisplayName("최신 WEEK Snapshot의 최종 인기도순으로 조회한다")
    void getPopularOfWeek_ordersByLatestWeeklyPopularitySnapshot() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);
      LocalDateTime previousBucket = weeklyBucket().minusWeeks(1);
      LocalDateTime latestBucket = weeklyBucket();
      alcoholTestFactory.persistPopularitySnapshot(
          alcohols.get(0).getId(),
          BucketGranularity.WEEK,
          previousBucket,
          score("0.1"),
          score("1.0"));
      persistSnapshot(alcohols.get(0), BucketGranularity.WEEK, latestBucket, "0.3", "0.2");
      persistSnapshot(alcohols.get(1), BucketGranularity.WEEK, latestBucket, "0.1", "0.9");
      persistSnapshot(alcohols.get(2), BucketGranularity.WEEK, latestBucket, "0.2", "0.5");

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/week", 3);

      assertEquals(
          List.of(alcohols.get(1).getId(), alcohols.get(2).getId(), alcohols.get(0).getId()),
          alcoholIds(response));
      assertEquals(0.9, response.getAlcohols().getFirst().popularScore());
    }

    @Test
    @DisplayName("삭제 처리된 알코올은 WEEK Snapshot 인기 목록에서 제외한다")
    void getPopularOfWeek_excludesDeletedAlcohol() throws Exception {
      Alcohol visible = alcoholTestFactory.persistAlcohol();
      Alcohol deleted = alcoholTestFactory.persistDeletedAlcohol();
      persistSnapshot(visible, BucketGranularity.WEEK, weeklyBucket(), "0.4", "0.4");
      persistSnapshot(deleted, BucketGranularity.WEEK, weeklyBucket(), "0.9", "0.9");

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/week", 5);

      assertTrue(alcoholIds(response).contains(visible.getId()));
      assertFalse(alcoholIds(response).contains(deleted.getId()));
    }
  }

  @Nested
  @DisplayName("관심도 기반 인기 API")
  class InterestPopularApi {

    @Test
    @DisplayName("주간 목록은 WEEK Snapshot 관심도순으로 조회한다")
    void getPopularViewWeekly_ordersByWeeklyInterestSnapshot() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);
      LocalDateTime bucket = weeklyBucket();
      persistSnapshot(alcohols.get(0), BucketGranularity.WEEK, bucket, "0.8", "0.1");
      persistSnapshot(alcohols.get(1), BucketGranularity.WEEK, bucket, "0.3", "0.9");
      persistSnapshot(alcohols.get(2), BucketGranularity.WEEK, bucket, "0.5", "0.5");

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/view/week", 3);

      assertEquals(
          List.of(alcohols.get(0).getId(), alcohols.get(2).getId(), alcohols.get(1).getId()),
          alcoholIds(response));
      assertEquals(0.8, response.getAlcohols().getFirst().popularScore());
    }

    @Test
    @DisplayName("주간 Snapshot에 없는 주류를 평점으로 보충하지 않는다")
    void getPopularViewWeekly_doesNotFillMissingSnapshotWithRating() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(2);
      User user = userTestFactory.persistUser();
      persistSnapshot(alcohols.getFirst(), BucketGranularity.WEEK, weeklyBucket(), "0.5", "0.5");
      ratingTestFactory.persistRating(user, alcohols.get(1), 5);

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/view/week", 10);

      assertEquals(List.of(alcohols.getFirst().getId()), alcoholIds(response));
    }

    @Test
    @DisplayName("삭제 처리된 알코올은 WEEK Snapshot 관심도 목록에서 제외한다")
    void getPopularViewWeekly_excludesDeletedAlcohol() throws Exception {
      Alcohol visible = alcoholTestFactory.persistAlcohol();
      Alcohol deleted = alcoholTestFactory.persistDeletedAlcohol();
      persistSnapshot(visible, BucketGranularity.WEEK, weeklyBucket(), "0.4", "0.4");
      persistSnapshot(deleted, BucketGranularity.WEEK, weeklyBucket(), "0.9", "0.9");

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/view/week", 5);

      assertTrue(alcoholIds(response).contains(visible.getId()));
      assertFalse(alcoholIds(response).contains(deleted.getId()));
    }

    @Test
    @DisplayName("월간 목록은 MONTH Snapshot 관심도순으로 조회한다")
    void getPopularViewMonthly_ordersByMonthlyInterestSnapshot() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);
      LocalDateTime bucket = monthlyBucket();
      persistSnapshot(alcohols.get(0), BucketGranularity.MONTH, bucket, "0.2", "0.9");
      persistSnapshot(alcohols.get(1), BucketGranularity.MONTH, bucket, "0.7", "0.1");
      persistSnapshot(alcohols.get(2), BucketGranularity.MONTH, bucket, "0.4", "0.5");

      PopularsOfWeekResponse response = getPopular("/api/v1/popular/view/monthly", 3);

      assertEquals(
          List.of(alcohols.get(1).getId(), alcohols.get(2).getId(), alcohols.get(0).getId()),
          alcoholIds(response));
      assertEquals(0.7, response.getAlcohols().getFirst().popularScore());
    }
  }

  private PopularsOfWeekResponse getPopular(String path, int top) throws Exception {
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri(path)
            .param("top", String.valueOf(top))
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();
    return extractData(result, PopularsOfWeekResponse.class);
  }

  private void persistSnapshot(
      Alcohol alcohol,
      BucketGranularity granularity,
      LocalDateTime bucketAt,
      String interestScore,
      String popularityScore) {
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(), granularity, bucketAt, score(interestScore), score(popularityScore));
  }

  private static List<Long> alcoholIds(PopularsOfWeekResponse response) {
    return response.getAlcohols().stream().map(item -> item.alcoholId()).toList();
  }

  private static BigDecimal score(String value) {
    return new BigDecimal(value);
  }

  private static LocalDateTime weeklyBucket() {
    return BucketGranularity.WEEK.startAt(LocalDateTime.now());
  }

  private static LocalDateTime monthlyBucket() {
    return BucketGranularity.MONTH.startAt(LocalDateTime.now());
  }
}
