package app.bottlenote.rating.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaRatingRepository
    extends RatingRepository, JpaRepository<Rating, RatingId>, CustomRatingQueryRepository {

  @Query("SELECT r FROM rating r WHERE r.id.alcoholId = :alcoholId AND r.id.userId = :userId")
  Optional<Rating> findByAlcoholIdAndUserId(
      @Param("alcoholId") Long alcoholId, @Param("userId") Long userId);

  @Query(
      """
			SELECT new app.bottlenote.rating.dto.response.UserRatingResponse(
			    r.ratingPoint.rating,
			    r.id.alcoholId,
			    r.id.userId
			)
			FROM rating r
			WHERE r.id.alcoholId = :alcoholId
			  AND r.id.userId = :userId
			""")
  Optional<UserRatingResponse> fetchUserRating(
      @Param("alcoholId") Long alcoholId, @Param("userId") Long userId);

  @Override
  @Query(
      "select case when count(r) > 0 then true else false end from rating r where r.id.alcoholId = :alcoholId")
  boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);
}
