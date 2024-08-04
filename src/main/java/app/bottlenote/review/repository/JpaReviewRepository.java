package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.repository.custom.CustomReviewReplyRepository;
import app.bottlenote.review.repository.custom.CustomReviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaReviewRepository extends
	JpaRepository<Review, Long>,
	ReviewRepository,
	CustomReviewRepository,
	CustomReviewReplyRepository {

	@Override
	Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

	@Override
	@Query("select r from review_reply r left join review_reply rr on r.rootReviewReply.id = rr.id where r.review.id = :reviewId and r.id = :parentReplyId")
	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);

	@Override
	@Query("select r from review_reply r where r.review.id = :review and r.id = :replyId")
	Optional<ReviewReply> findReplyByReviewIdAndReplyId(Long review, Long replyId);

	@Override
	@Query("select r from review_reply r where r.id = :id")
	Optional<ReviewReply> findReplyById(Long id);

	@Override
	@Query("select r from review_reply r")
	List<ReviewReply> findAllReply();

}
