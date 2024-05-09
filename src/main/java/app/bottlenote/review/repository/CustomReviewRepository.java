package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.response.ReviewResponse;

public interface CustomReviewRepository {

	PageResponse<ReviewResponse> getReviews(Long alcoholId, CursorPageable cursorPageable,
		Long userId);
}
