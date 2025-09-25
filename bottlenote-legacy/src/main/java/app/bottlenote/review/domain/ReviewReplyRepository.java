package app.bottlenote.review.domain;

import app.bottlenote.shared.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.shared.review.dto.response.SubReviewReplyResponse;
import java.util.List;
import java.util.Optional;

public interface ReviewReplyRepository {

  ReviewReply save(ReviewReply reviewReply);

  Optional<ReviewReply> findReplyById(Long id);

  List<ReviewReply> findAllReply();

  Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

  RootReviewReplyResponse getReviewRootReplies(Long reviewId, Long cursor, Long pageSize);

  SubReviewReplyResponse getSubReviewReplies(
      Long reviewId, Long replyId, Long cursor, Long pageSize);

  Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long reviewId, Long replyId);
}
