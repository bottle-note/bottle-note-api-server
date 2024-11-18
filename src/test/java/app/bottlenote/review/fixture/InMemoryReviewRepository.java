package app.bottlenote.review.fixture;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.RootReviewReplyInfo;
import app.bottlenote.review.dto.response.SubReviewReplyInfo;
import app.bottlenote.review.dto.vo.CommonReviewInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryReviewRepository implements ReviewRepository {

	private static final Logger log = LogManager.getLogger(InMemoryReviewRepository.class);

	Map<Long, Review> database = new HashMap<>();
	Map<Long, ReviewReply> reviewReplyDatabase = new HashMap<>();

	@Override
	public Review save(Review review) {
		Long id = review.getId();
		if (Objects.isNull(id)) {
			id = database.size() + 1L;
		}
		ReflectionTestUtils.setField(review, "id", id);
		database.put(id, review);
		log.info("[InMemory] review repository save = {}", review);

		review.getReviewReplies().forEach(reply -> {
			Long replyId = reply.getId();
			if (Objects.isNull(replyId)) {
				replyId = reviewReplyDatabase.size() + 1L;
			}
			ReflectionTestUtils.setField(reply, "id", replyId);
			reviewReplyDatabase.put(replyId, reply);
			log.info("[InMemory] review reply repository save = {}", reply);
		});

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
	public Optional<ReviewReply> findReplyById(Long id) {
		return Optional.ofNullable(reviewReplyDatabase.get(id));
	}

	@Override
	public List<ReviewReply> findAllReply() {
		return List.copyOf(reviewReplyDatabase.values());
	}

	@Override
	public CommonReviewInfo getReview(Long reviewId, Long userId) {
		return null;
	}

	@Override
	public PageResponse<ReviewListResponse> getReviews(Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
		return null;
	}

	@Override
	public PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
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
	public RootReviewReplyInfo getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
		return RootReviewReplyInfo.of(0L, List.of());
	}

	@Override
	public SubReviewReplyInfo getSubReviewReplies(Long reviewId, Long replyId, Long cursor, Long pageSize) {
		return SubReviewReplyInfo.of(0L, List.of());
	}

	@Override
	public Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long review, Long replyId) {
		return reviewReplyDatabase.values()
			.stream()
			.filter(reply -> reply.getReview().getId().equals(review) && reply.getId().equals(replyId))
			.findFirst();
	}
}
