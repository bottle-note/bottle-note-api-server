package app.bottlenote.review.fixture;

import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.shared.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.shared.review.dto.response.SubReviewReplyResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InMemoryReviewReplyRepository implements ReviewReplyRepository {

  private static final Logger log = LogManager.getLogger(InMemoryReviewReplyRepository.class);

  Map<Long, ReviewReply> reviewReplyDatabase = new HashMap<>();

  @Override
  public ReviewReply save(ReviewReply reviewReply) {
    Long id = reviewReply.getId();
    if (id == null) {
      id = reviewReplyDatabase.size() + 1L;
    }
    reviewReplyDatabase.put(id, reviewReply);
    log.info("[InMemory] review reply repository save = {}", reviewReply);
    return reviewReply;
  }

  @Override
  public Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId) {
    Optional<ReviewReply> first =
        reviewReplyDatabase.values().stream()
            .filter(
                reply ->
                    reply.getReviewId().equals(reviewId) && reply.getId().equals(parentReplyId))
            .findFirst();

    log.info(
        "[InMemory] isEligibleParentReply(reviewId = {}, parentReplyId = {}) = {}",
        reviewId,
        parentReplyId,
        first);
    return first;
  }

  @Override
  public RootReviewReplyResponse getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
    return RootReviewReplyResponse.of(0L, List.of());
  }

  @Override
  public SubReviewReplyResponse getSubReviewReplies(
      Long reviewId, Long replyId, Long cursor, Long pageSize) {
    return SubReviewReplyResponse.of(0L, List.of());
  }

  @Override
  public Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long review, Long replyId) {
    return reviewReplyDatabase.values().stream()
        .filter(reply -> reply.getReviewId().equals(review) && reply.getId().equals(replyId))
        .findFirst();
  }

  @Override
  public Optional<ReviewReply> findReplyById(Long id) {
    return Optional.ofNullable(reviewReplyDatabase.get(id));
  }

  @Override
  public List<ReviewReply> findAllReply() {
    return List.copyOf(reviewReplyDatabase.values());
  }
}
