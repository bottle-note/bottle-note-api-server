package app.bottlenote.review.domain;

import app.bottlenote.review.dto.response.RootReviewReplyInfo;
import app.bottlenote.review.dto.response.SubReviewReplyInfo;
import java.util.List;
import java.util.Optional;

public interface ReviewReplyRepository {

	ReviewReply save(ReviewReply reviewReply);

	Optional<ReviewReply> findReplyById(Long id);

	List<ReviewReply> findAllReply();

	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

	RootReviewReplyInfo getReviewRootReplies(Long reviewId, Long cursor, Long pageSize);

	SubReviewReplyInfo getSubReviewReplies(Long reviewId, Long replyId, Long cursor, Long pageSize);

	Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long reviewId, Long replyId);

}
