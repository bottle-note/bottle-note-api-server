package app.bottlenote.review.repository;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewRepository
    extends JpaRepository<Review, Long>,
        ReviewRepository,
        CustomReviewRepository,
        CustomReviewReplyRepository {

  @Override
  Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

  @Override
  @Query("select r from review r where r.userId = :userId")
  List<Review> findByUserId(@Param("userId") Long userId);

  @Override
  @Query(
      "select case when count(r) > 0 then true else false end from review r where r.alcoholId = :alcoholId")
  boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);
}
