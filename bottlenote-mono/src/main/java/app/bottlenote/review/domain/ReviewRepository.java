package app.bottlenote.review.domain;

import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.dto.request.AdminReviewSearchRequest;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.AdminReviewListResponse;
import app.bottlenote.review.dto.response.AlcoholReviewCountResponse;
import app.bottlenote.review.dto.response.ReviewExploreListResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ReviewRepository {

  Review save(Review review);

  Optional<Review> findById(Long id);

  List<Review> findAll();

  ReviewInfo getReview(Long reviewId, Long userId);

  PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  PageResponse<ReviewListResponse> getReviewsByMe(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  Page<AdminReviewListResponse> searchAdminReviews(AdminReviewSearchRequest request);

  Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

  List<Review> findByUserId(Long userId);

  Long countByAlcoholIdAndActiveStatusAndStatus(
      Long alcoholId, ReviewActiveStatus activeStatus, ReviewDisplayStatus status);

  List<AlcoholReviewCountResponse> countByAlcoholIdsAndActiveStatusAndStatus(
      List<Long> alcoholIds, ReviewActiveStatus activeStatus, ReviewDisplayStatus status);

  boolean existsById(Long reviewId);

  boolean existsByAlcoholId(Long alcoholId);

  PageResponse<ReviewExploreListResponse> getStandardExplore(
      Long userId, List<String> keywords, String cursor, Integer size);
}
