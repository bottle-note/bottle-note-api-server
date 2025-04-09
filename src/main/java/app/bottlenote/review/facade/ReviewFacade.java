package app.bottlenote.review.facade;

import app.bottlenote.review.dto.response.ReviewListResponse;

public interface ReviewFacade {
	ReviewListResponse getReviewInfoList(Long alcoholId, Long userId);

	Long getAlcoholIdByReviewId(Long reviewId);

	boolean isExistReview(Long reviewId);

	void requestBlockReview(Long reviewId);
}
