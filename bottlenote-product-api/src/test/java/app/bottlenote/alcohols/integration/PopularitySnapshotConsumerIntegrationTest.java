package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.PopularQueryRepository;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.ReviewMyBottleItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] Product 인기도 Snapshot 소비")
class PopularitySnapshotConsumerIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private PopularQueryRepository popularQueryRepository;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("봄 추천은 랜덤 선정을 유지하면서 최신 WEEK Snapshot 점수를 반환한다")
  void springRecommendation_returnsLatestWeeklySnapshotScore() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TastingTag tag =
        TastingTag.builder().korName("봄 추천 태그").engName("Spring Recommendation").build();
    alcoholTestFactory.appendTastingTag(alcohol, tag);
    LocalDateTime bucket = BucketGranularity.WEEK.startAt(LocalDateTime.now());
    alcoholTestFactory.persistPopularAlcohol(alcohol.getId(), new BigDecimal("0.9"));
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(), BucketGranularity.WEEK, bucket, BigDecimal.ZERO, new BigDecimal("0.4"));

    List<PopularItem> result =
        popularQueryRepository.getSpringItems(
            -1L, List.of(tag.getId()), List.of(Long.MAX_VALUE), Pageable.ofSize(6));

    assertThat(result).singleElement().extracting(PopularItem::popularScore).isEqualTo(0.4);
  }

  @Test
  @DisplayName("봄 추천의 평점 개수는 여러 허용 태그와 조인되어도 중복 집계하지 않는다")
  void springRecommendation_countsRatingsWithoutTagJoinDuplication() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TastingTag firstTag = TastingTag.builder().korName("봄 태그 1").engName("Spring Tag 1").build();
    TastingTag secondTag = TastingTag.builder().korName("봄 태그 2").engName("Spring Tag 2").build();
    alcoholTestFactory.appendTastingTag(alcohol, firstTag);
    alcoholTestFactory.appendTastingTag(alcohol, secondTag);
    for (int i = 0; i < 3; i++) {
      ratingTestFactory.persistRating(userTestFactory.persistUser(), alcohol, 4);
    }
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(),
        BucketGranularity.WEEK,
        BucketGranularity.WEEK.startAt(LocalDateTime.now()),
        BigDecimal.ZERO,
        new BigDecimal("0.4"));

    List<PopularItem> result =
        popularQueryRepository.getSpringItems(
            -1L,
            List.of(firstTag.getId(), secondTag.getId()),
            List.of(Long.MAX_VALUE),
            Pageable.ofSize(6));

    assertThat(result).singleElement().extracting(PopularItem::ratingCount).isEqualTo(3L);
  }

  @Test
  @DisplayName("인기 주류는 평균 별점을 소수점 첫째 자리로 노출한다")
  void popularAlcohol_displaysRatingToOneDecimalPlace() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    for (double point : List.of(4.0, 4.5, 5.0, 5.0)) {
      User user = userTestFactory.persistUser();
      ratingTestFactory.persistRating(
          Rating.builder()
              .id(Rating.RatingId.is(user.getId(), alcohol.getId()))
              .ratingPoint(RatingPoint.of(point)));
    }
    alcoholTestFactory.persistPopularitySnapshot(
        alcohol.getId(),
        BucketGranularity.WEEK,
        BucketGranularity.WEEK.startAt(LocalDateTime.now()),
        BigDecimal.ZERO,
        new BigDecimal("0.4"));

    List<PopularItem> result =
        popularQueryRepository.getPopularOfWeeks(-1L, Pageable.ofSize(1));

    assertThat(result).singleElement().extracting(PopularItem::rating).isEqualTo(4.6);
  }

  @Test
  @DisplayName("마이보틀 isHot5는 최신 WEEK Snapshot 상위 5개만 true로 반환한다")
  void myBottle_marksOnlyTopFiveWeeklySnapshotsAsHot() throws Exception {
    User target = userTestFactory.persistUser();
    User viewer = userTestFactory.persistUser();
    List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(6);
    LocalDateTime bucket = BucketGranularity.WEEK.startAt(LocalDateTime.now());
    for (int i = 0; i < alcohols.size(); i++) {
      Alcohol alcohol = alcohols.get(i);
      reviewTestFactory.persistReview(target, alcohol);
      alcoholTestFactory.persistPopularitySnapshot(
          alcohol.getId(),
          BucketGranularity.WEEK,
          bucket,
          BigDecimal.ZERO,
          BigDecimal.valueOf(alcohols.size() - i, 1));
    }

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/my-page/{userId}/my-bottle/reviews", target.getId())
            .param("sortType", "LATEST")
            .param("sortOrder", "DESC")
            .param("size", "10")
            .header("Authorization", "Bearer " + getToken(viewer).accessToken())
            .contentType(APPLICATION_JSON)
            .with(csrf())
            .exchange();

    MyBottleResponse response = extractData(result, MyBottleResponse.class);
    Map<Long, Boolean> hotByAlcoholId =
        response.myBottleList().stream()
            .map(item -> mapper.convertValue(item, ReviewMyBottleItem.class))
            .collect(
                Collectors.toMap(
                    item -> item.baseMyBottleInfo().alcoholId(),
                    item -> item.baseMyBottleInfo().isHot5()));

    assertThat(hotByAlcoholId.get(alcohols.get(5).getId())).isFalse();
    assertThat(alcohols.subList(0, 5))
        .allSatisfy(alcohol -> assertThat(hotByAlcoholId.get(alcohol.getId())).isTrue());
  }
}
