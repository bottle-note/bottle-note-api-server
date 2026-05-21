package app.bottlenote.review.repository;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.response.AlcoholReviewCountResponse;
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
      """
      select new app.bottlenote.review.dto.response.AlcoholReviewCountResponse(r.alcoholId, count(r))
      from review r
      where r.alcoholId in :alcoholIds
        and r.activeStatus = :activeStatus
        and r.status = :status
      group by r.alcoholId
      """)
  List<AlcoholReviewCountResponse> countByAlcoholIdsAndActiveStatusAndStatus(
      @Param("alcoholIds") List<Long> alcoholIds,
      @Param("activeStatus") ReviewActiveStatus activeStatus,
      @Param("status") ReviewDisplayStatus status);

  @Override
  @Query(
      "select case when count(r) > 0 then true else false end from review r where r.alcoholId = :alcoholId")
  boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);
}
