package app.bottlenote.review.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.review.facade.payload.ReviewInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@FacadeService
@RequiredArgsConstructor
public class DefaultReviewFacade implements ReviewFacade {
  private final ReviewRepository reviewRepository;

  @Override
  @Transactional(readOnly = true)
  public ReviewListResponse getReviewInfoList(Long alcoholId, Long userId) {
    ReviewPageableRequest pageableRequest =
        ReviewPageableRequest.builder().cursor(0L).pageSize(6L).build();
    PageResponse<ReviewListResponse> reviews =
        reviewRepository.getReviews(alcoholId, pageableRequest, userId);
    return reviews.content();
  }

  @Override
  @Transactional(readOnly = true)
  public Long getAlcoholIdByReviewId(Long reviewId) {
    return reviewRepository
        .findById(reviewId)
        .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND))
        .getAlcoholId();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isExistReview(Long reviewId) {
    return reviewRepository.existsById(reviewId);
  }

  @Override
  @Transactional
  public void requestBlockReview(Long reviewId) {
    reviewRepository.findById(reviewId).ifPresent(Review::blockReview);
  }

  @Override
  @Transactional(readOnly = true)
  public ReviewInfo getReviewInfo(Long reviewId, Long userId) {
    return reviewRepository.getReview(reviewId, userId);
  }
}
