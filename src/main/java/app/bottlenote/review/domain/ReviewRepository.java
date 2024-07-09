package app.bottlenote.review.domain;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewResponse;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findById(Long id);

	List<Review> findAll();

	ReviewResponse getReview(Long reviewId, Long userId);

	PageResponse<ReviewListResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId);

	PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, PageableRequest pageableRequest, Long userId);

	Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

	List<ReviewReplyInfo> getReviewReplies(Long reviewId);
}
