package app.bottlenote.rating.repository;

import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRatingRepository extends RatingRepository, JpaRepository<Rating, RatingId>, CustomRatingQueryRepository {

	@Query("SELECT r FROM rating r WHERE r.id.alcoholId = :alcoholId AND r.id.userId = :userId")
	Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

	@Query("SELECT new app.bottlenote.rating.dto.response.UserRatingResponse(r.ratingPoint.rating, r.id.alcoholId, r.id.userId) " +
            "FROM rating r " +
            "WHERE r.id.alcoholId = :alcoholId " +
            "AND r.id.userId = :userId")
	Optional<UserRatingResponse> fetchUserRating(Long alcoholId, Long userId);
}
