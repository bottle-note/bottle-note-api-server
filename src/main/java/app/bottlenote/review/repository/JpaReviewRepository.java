package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.repository.custom.CustomReviewReplyRepository;
import app.bottlenote.review.repository.custom.CustomReviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaReviewRepository extends JpaRepository<Review, Long>, ReviewRepository, CustomReviewRepository, CustomReviewReplyRepository {

	Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

	@Query("select r from review_reply r left join review_reply rr on r.rootReviewReply.id = rr.id where r.review.id = :reviewId and r.id = :parentReplyId")
	Optional<ReviewReply> isEligibleParentReply(Long reviewId, Long parentReplyId);
}
