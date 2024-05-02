package app.bottlenote.rating.repository;

import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import org.springframework.data.repository.CrudRepository;

public interface RatingRepository extends CrudRepository<Rating, RatingId> {
}
