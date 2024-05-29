package app.bottlenote.rating.service;

import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingRepository;
import org.springframework.stereotype.Service;

@Service
public class RatingCommandService {
	private final RatingRepository ratingRepository;

	public RatingCommandService(RatingRepository ratingRepository) {
		this.ratingRepository = ratingRepository;
	}

	public Object register(Rating rating) {
		return null;
	}
}
