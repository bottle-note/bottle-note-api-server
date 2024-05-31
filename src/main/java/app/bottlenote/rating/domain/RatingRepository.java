package app.bottlenote.rating.domain;

import java.util.List;
import java.util.Optional;

public interface RatingRepository {
	Rating save(Rating rating);

	Optional<Rating> findById(RatingId ratingId);

	List<Rating> findAll();

	List<Rating> findAllByIdIn(List<RatingId> ids);

	Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
