package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewQueryRepository extends JpaRepository<Review, Long> {
}
