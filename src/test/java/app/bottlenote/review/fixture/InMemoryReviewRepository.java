package app.bottlenote.review.fixture;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryReviewRepository implements ReviewRepository {

	private static final Logger log = LogManager.getLogger(InMemoryReviewRepository.class);
	Map<Long, Review> database = new HashMap<>();

	@Override
	public Review save(Review review) {
		Long id = review.getId();
		if (Objects.isNull(id)) {
			id = database.size() + 1L;
		}
		database.put(id, review);
		return review;
	}

	@Override
	public Optional<Review> findById(Long id) {
		return Optional.ofNullable(database.get(id));
	}

	@Override
	public List<Review> findAll() {
		return List.copyOf(database.values());
	}

	@Override
	public ReviewDetailResponse.ReviewInfo getReview(Long reviewId, Long userId) {
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
		Optional<ReviewReply> first = database.values().stream()
			.filter(review -> review.getId().equals(reviewId))
			.flatMap(review -> review.getReviewReplies().stream())
			.filter(reply -> reply.getId().equals(parentReplyId))
			.findFirst();

		log.info("[InMemory] isEligibleParentReply(reviewId = {}, parentReplyId = {}) = {}", reviewId, parentReplyId, first);
		return first;
	}


	@Override
	public List<ReviewReplyInfo> getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
		return List.of();
	}

	@Override
	public List<?> getReviewChildReplies(Long reviewId, Long parentReplyId, Long cursor, Long pageSize) {
		return List.of();
	}
}
