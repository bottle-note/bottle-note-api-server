package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.repository.custom.CustomReviewReplyRepository;
import app.bottlenote.review.repository.custom.CustomReviewRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewRepository extends
	JpaRepository<Review, Long>,
	ReviewRepository,
	CustomReviewRepository,
	CustomReviewReplyRepository {

	@Override
	Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

	@Override
	@Query("select r from review r where r.userId = :userId")
	List<Review> findByUserId(Long userId);
}
