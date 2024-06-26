package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.repository.custom.CustomReviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, CustomReviewRepository {

    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

    @Query("select rr.id from review r join review_reply rr on r.id = rr.review.id and rr.id = :parentReplyId where r.id = :reviewId")
    Long isEligibleParentReply(Long reviewId, Long parentReplyId);
}
