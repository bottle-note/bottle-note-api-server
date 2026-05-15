package app.bottlenote.review.fixture;

import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.AlcoholReviewCountResponse;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryReviewRepository implements ReviewRepository {

  private static final Logger log = LogManager.getLogger(InMemoryReviewRepository.class);

  Map<Long, Review> database = new HashMap<>();

  @Override
  public Review save(Review review) {
    Long id = review.getId();
    if (Objects.isNull(id)) {
      id = database.size() + 1L;
    }
    ReflectionTestUtils.setField(review, "id", id);
    database.put(id, review);
    log.info("[InMemory] review repository save = {}", review);

    return review;
  }

  @Override
  public Optional<Review> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Review> findAll() {
    return List.copyOf(database.values());
  }

  @Override
  public ReviewInfo getReview(Long reviewId, Long userId) {
    return null;
  }

  @Override
  public PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
    return null;
  }

  @Override
  public PageResponse<ReviewListResponse> getReviewsByMe(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
    return null;
  }

  @Override
  public Optional<Review> findByIdAndUserId(Long reviewId, Long userId) {
    return database.values().stream()
        .filter(r -> r.getId().equals(reviewId) && r.getUserId().equals(userId))
        .findFirst();
  }

  @Override
  public List<Review> findByUserId(Long userId) {
    return List.of();
  }

  @Override
  public Long countByAlcoholIdAndActiveStatusAndStatus(
      Long alcoholId, ReviewActiveStatus activeStatus, ReviewDisplayStatus status) {
    return database.values().stream()
        .filter(review -> Objects.equals(review.getAlcoholId(), alcoholId))
        .filter(review -> review.getActiveStatus() == activeStatus)
        .filter(review -> review.getStatus() == status)
        .count();
  }

  @Override
  public List<AlcoholReviewCountResponse> countByAlcoholIdsAndActiveStatusAndStatus(
      List<Long> alcoholIds, ReviewActiveStatus activeStatus, ReviewDisplayStatus status) {
    return alcoholIds.stream()
        .map(
            alcoholId ->
                new AlcoholReviewCountResponse(
                    alcoholId,
                    countByAlcoholIdAndActiveStatusAndStatus(alcoholId, activeStatus, status)))
        .filter(count -> count.reviewCount() > 0)
        .toList();
  }

  @Override
  public boolean existsById(Long reviewId) {
    return database.containsKey(reviewId);
  }

  @Override
  public Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size) {
    return null;
  }

  @Override
  public boolean existsByAlcoholId(Long alcoholId) {
    return database.values().stream().anyMatch(review -> review.getAlcoholId().equals(alcoholId));
  }
}
