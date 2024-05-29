package app.bottlenote.rating.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository {
	Rating save(Rating rating);

	Optional<Rating> findById(UUID id);

	List<Rating> findAll();

	List<Rating> findAllByIdIn(List<RatingId> ids);

	List<Rating> findAllByProductId(RatingId ratingId);
}
