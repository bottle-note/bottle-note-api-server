package app.bottlenote.review.repository;


import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.review.repository.custom.CustomReviewReplyRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewReplyRepository extends ReviewReplyRepository, JpaRepository<ReviewReply, Long>, CustomReviewReplyRepository {

	@Override
	@Query("select r from review_reply r left join review_reply rr on r.rootReviewReply.id = rr.id where r.reviewId = :reviewId and r.id = :parentReplyId")
	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

	@Override
	@Query("select r from review_reply r where r.reviewId = :review and r.id = :replyId")
	Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long review, Long replyId);

	@Override
	@Query("select r from review_reply r where r.id = :id")
	Optional<ReviewReply> findReplyById(Long id);

	@Override
	@Query("select r from review_reply r")
	List<ReviewReply> findAllReply();

}
