package app.bottlenote.rating.service;

import app.bottlenote.rating.domain.RatingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingQueryService {
	private final RatingRepository ratingRepository;

	public RatingQueryService(RatingRepository ratingRepository) {
		this.ratingRepository = ratingRepository;
	}

	public List<?> getRatingList() {
		return List.of();
	}
}
