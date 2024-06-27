package app.bottlenote.review.repository.custom;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResponse;

public interface CustomReviewRepository {

	ReviewResponse getReview(Long reviewId, Long userId);

	PageResponse<ReviewListResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId);

	PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, PageableRequest pageableRequest, Long userId);
}
