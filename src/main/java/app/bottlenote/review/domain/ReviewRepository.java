package app.bottlenote.review.domain;

import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

  Review save(Review review);

  Optional<Review> findById(Long id);

  List<Review> findAll();

  ReviewInfo getReview(Long reviewId, Long userId);

  PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  PageResponse<ReviewListResponse> getReviewsByMe(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

  List<Review> findByUserId(Long userId);

  boolean existsById(Long reviewId);

  Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size);
}
