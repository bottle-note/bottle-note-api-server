package app.bottlenote.curation.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.curation.service.GraphQLCurationAlcoholService;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.response.AlcoholPicksCountResponse;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.AlcoholRatingStatsResponse;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class GraphQLCurationAlcoholResolverTest {

  @Test
  @DisplayName("MANUAL 항목처럼 alcoholId가 null이면 GraphQL 조회에서 제외하고 실제 도메인 통계를 반환한다")
  void alcohols_whenManualItemAlcoholIdIsNull_excludesNullAndMissingIdsAndResolvesStats() {
    InMemoryAlcoholQueryRepository alcoholRepository = new InMemoryAlcoholQueryRepository();
    FakeRatingRepository ratingRepository = new FakeRatingRepository();
    InMemoryReviewRepository reviewRepository = new InMemoryReviewRepository();
    FakePicksRepository picksRepository = new FakePicksRepository();
    GraphQLCurationAlcoholResolver resolver =
        new GraphQLCurationAlcoholResolver(
            new GraphQLCurationAlcoholService(
                alcoholRepository, ratingRepository, reviewRepository, picksRepository));

    Alcohol alcohol = alcohol(1L);
    alcoholRepository.save(alcohol);
    ratingRepository.save(rating(10L, 1L, 4.5));
    ratingRepository.save(rating(11L, 1L, 3.5));
    ratingRepository.save(rating(12L, 1L, 0.0));
    reviewRepository.save(review(1L, ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PUBLIC));
    reviewRepository.save(review(1L, ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PRIVATE));
    reviewRepository.save(review(1L, ReviewActiveStatus.DELETED, ReviewDisplayStatus.PUBLIC));
    picksRepository.save(picks(1L, 20L, PicksStatus.PICK));
    picksRepository.save(picks(1L, 21L, PicksStatus.UNPICK));

    List<Alcohol> alcohols = resolver.alcohols(Arrays.asList(null, 1L, 999L));
    List<Alcohol> manualOnly = resolver.alcohols(Collections.singletonList(null));

    assertThat(alcohols).extracting(Alcohol::getId).containsExactly(1L);
    assertThat(manualOnly).isEmpty();
    assertThat(resolver.alcoholId(alcohol)).isEqualTo(1L);
    assertThat(resolver.regionName(alcohol)).isEqualTo("스코틀랜드");
    assertThat(resolver.ratings(alcohols)).containsEntry(alcohol, 4.0);
    assertThat(resolver.totalRatingsCounts(alcohols)).containsEntry(alcohol, 2L);
    assertThat(resolver.reviewCounts(alcohols)).containsEntry(alcohol, 1L);
    assertThat(resolver.totalPickCounts(alcohols)).containsEntry(alcohol, 1L);
  }

  private static Alcohol alcohol(Long alcoholId) {
    Region region = Region.builder().id(1L).korName("스코틀랜드").engName("Scotland").build();
    return Alcohol.builder()
        .id(alcoholId)
        .korName("테스트 위스키")
        .engName("Test Whisky")
        .abv("40%")
        .type(AlcoholType.WHISKY)
        .korCategory("위스키")
        .engCategory("Whisky")
        .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
        .region(region)
        .cask("Oak")
        .imageUrl("https://example.com/test-whisky.jpg")
        .volume("700ml")
        .build();
  }

  private static Rating rating(Long userId, Long alcoholId, Double rating) {
    return Rating.builder()
        .id(RatingId.is(userId, alcoholId))
        .ratingPoint(RatingPoint.of(rating))
        .build();
  }

  private static Review review(
      Long alcoholId, ReviewActiveStatus activeStatus, ReviewDisplayStatus displayStatus) {
    return Review.builder()
        .userId(1L)
        .alcoholId(alcoholId)
        .content("리뷰")
        .activeStatus(activeStatus)
        .status(displayStatus)
        .build();
  }

  private static Picks picks(Long alcoholId, Long userId, PicksStatus status) {
    return Picks.builder().alcoholId(alcoholId).userId(userId).status(status).build();
  }

  private static final class FakeRatingRepository implements RatingRepository {

    private final Map<RatingId, Rating> ratings = new HashMap<>();

    @Override
    public Rating save(Rating rating) {
      ratings.put(rating.getId(), rating);
      return rating;
    }

    @Override
    public Optional<Rating> findById(RatingId ratingId) {
      return Optional.ofNullable(ratings.get(ratingId));
    }

    @Override
    public List<Rating> findAll() {
      return List.copyOf(ratings.values());
    }

    @Override
    public List<Rating> findAllByIdIn(List<RatingId> ids) {
      return ids.stream().map(ratings::get).filter(Objects::nonNull).toList();
    }

    @Override
    public Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
      return findById(RatingId.is(userId, alcoholId));
    }

    @Override
    public PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria) {
      throw new UnsupportedOperationException("not used in GraphQL resolver test");
    }

    @Override
    public Optional<UserRatingResponse> fetchUserRating(Long alcoholId, Long userId) {
      throw new UnsupportedOperationException("not used in GraphQL resolver test");
    }

    @Override
    public Double findAverageRatingByAlcoholId(Long alcoholId) {
      return ratings.values().stream()
          .filter(rating -> Objects.equals(rating.getId().getAlcoholId(), alcoholId))
          .mapToDouble(rating -> rating.getRatingPoint().getRating())
          .filter(rating -> rating > 0.0)
          .average()
          .orElse(0.0);
    }

    @Override
    public Long countByAlcoholId(Long alcoholId) {
      return ratings.values().stream()
          .filter(rating -> Objects.equals(rating.getId().getAlcoholId(), alcoholId))
          .map(rating -> rating.getRatingPoint().getRating())
          .filter(rating -> rating > 0.0)
          .count();
    }

    @Override
    public List<AlcoholRatingStatsResponse> findStatsByAlcoholIds(List<Long> alcoholIds) {
      return alcoholIds.stream()
          .map(
              alcoholId ->
                  new AlcoholRatingStatsResponse(
                      alcoholId,
                      findAverageRatingByAlcoholId(alcoholId),
                      countByAlcoholId(alcoholId)))
          .filter(stats -> stats.totalRatingsCount() > 0)
          .toList();
    }

    @Override
    public boolean existsByAlcoholId(Long alcoholId) {
      return ratings.values().stream()
          .anyMatch(rating -> Objects.equals(rating.getId().getAlcoholId(), alcoholId));
    }
  }

  private static final class FakePicksRepository implements PicksRepository {

    private final List<Picks> picks = new java.util.ArrayList<>();

    @Override
    public Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
      return picks.stream()
          .filter(pick -> Objects.equals(pick.getAlcoholId(), alcoholId))
          .filter(pick -> Objects.equals(pick.getUserId(), userId))
          .findFirst();
    }

    @Override
    public Long countByAlcoholIdAndStatus(Long alcoholId, PicksStatus status) {
      return picks.stream()
          .filter(pick -> Objects.equals(pick.getAlcoholId(), alcoholId))
          .filter(pick -> pick.getStatus() == status)
          .count();
    }

    @Override
    public List<AlcoholPicksCountResponse> countByAlcoholIdsAndStatus(
        List<Long> alcoholIds, PicksStatus status) {
      return alcoholIds.stream()
          .map(
              alcoholId ->
                  new AlcoholPicksCountResponse(
                      alcoholId, countByAlcoholIdAndStatus(alcoholId, status)))
          .filter(count -> count.totalPickCount() > 0)
          .toList();
    }

    @Override
    public Picks save(Picks pick) {
      picks.add(pick);
      return pick;
    }
  }
}
