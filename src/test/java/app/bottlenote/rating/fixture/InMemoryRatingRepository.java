package app.bottlenote.rating.fixture;

import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRatingRepository implements RatingRepository {

	private final Map<RatingId, Rating> ratings = new HashMap<>();

	@Override
	public Rating save(Rating rating) {
		return ratings.put(rating.getId(), rating);
	}

	@Override
	public Optional<Rating> findById(RatingId ratingId) {
		return Optional.of(ratings.get(ratingId));
	}

	@Override
	public List<Rating> findAll() {
		return ratings.values().stream().toList();
	}

	@Override
	public List<Rating> findAllByIdIn(List<RatingId> ids) {
		return ratings.values().stream().filter(rating -> ids.contains(rating.getId())).toList();
	}

	@Override
	public Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
		return ratings.values().stream()
			.filter(
				rating -> rating.getId().getAlcoholId().equals(alcoholId) &&
					rating.getId().getUserId().equals(userId))
			.findFirst();
	}
}
