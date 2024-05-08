package app.bottlenote.review.repository;

import app.bottlenote.review.dto.response.ReviewResponse;
import java.util.List;

public interface CustomReviewRepository {

	//TODO : 리턴타입 PageResponse로 수정예정
	List<ReviewResponse> getReviews(Long alcoholId);
}
