package app.bottlenote.review.domain;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import java.util.List;
import java.util.Optional;

public interface ReviewReplyRepository {

  ReviewReply save(ReviewReply reviewReply);

  Optional<ReviewReply> findReplyById(Long id);

  List<ReviewReply> findAllReply();

  Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

  KeysetPageResponse<RootReviewReplyResponse> getReviewRootReplies(
      Long reviewId, String cursor, Integer size);

  KeysetPageResponse<SubReviewReplyResponse> getSubReviewReplies(
      Long reviewId, Long replyId, String cursor, Integer size);

  Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long reviewId, Long replyId);
}
