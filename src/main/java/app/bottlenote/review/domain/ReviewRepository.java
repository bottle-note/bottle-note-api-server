package app.bottlenote.review.domain;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findById(Long id);

	List<Review> findAll();

	ReviewDetailResponse.ReviewInfo getReview(Long reviewId, Long userId);

	PageResponse<ReviewListResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId);

	PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, PageableRequest pageableRequest, Long userId);

	Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

	List<ReviewReplyInfo> getReviewRootReplies(Long reviewId, Long cursor, Long pageSize);

	List<?> getReviewChildReplies(Long reviewId, Long parentReplyId, Long cursor, Long pageSize);
}
