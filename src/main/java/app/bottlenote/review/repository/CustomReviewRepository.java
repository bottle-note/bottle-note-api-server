package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewResponse;

public interface CustomReviewRepository {

	PageResponse<ReviewResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId);

	PageResponse<ReviewResponse> getReviewsByMe(Long alcoholId, PageableRequest pageableRequest, Long userId);
}
