package app.bottlenote.review.facade;

import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.shared.review.payload.ReviewInfo;

public interface ReviewFacade {
  // todo : core 모듈로 이동
  ReviewListResponse getReviewInfoList(Long alcoholId, Long userId);

  Long getAlcoholIdByReviewId(Long reviewId);

  boolean isExistReview(Long reviewId);

  void requestBlockReview(Long reviewId);

  ReviewInfo getReviewInfo(Long reviewId, Long userId);
}
