package app.bottlenote.review.fixture;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewResponse;
import java.util.List;
import java.util.Optional;

public class InmemoryReviewRepository implements ReviewRepository {
	@Override
	public Review save(Review review) {
		return null;
	}

	@Override
	public Optional<Review> findById(Long id) {
		return Optional.empty();
	}

	@Override
	public List<Review> findAll() {
		return List.of();
	}

	@Override
	public ReviewResponse getReview(Long reviewId, Long userId) {
		return null;
	}

	@Override
	public PageResponse<ReviewListResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId) {
		return null;
	}

	@Override
	public PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, PageableRequest pageableRequest, Long userId) {
		return null;
	}

	@Override
	public Optional<Review> findByIdAndUserId(Long reviewId, Long userId) {
		return Optional.empty();
	}

	@Override
	public Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId) {
		return Optional.empty();
	}

	@Override
	public List<ReviewReplyInfo> getReviewReplies(Long reviewId) {
		return null;
	}
}
