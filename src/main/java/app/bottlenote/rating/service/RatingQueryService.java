package app.bottlenote.rating.service;

import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingQueryService {
	private final RatingRepository ratingRepository;

	public RatingQueryService(RatingRepository ratingRepository) {
		this.ratingRepository = ratingRepository;
	}

	public List<?> getRatingList(RatingListFetchRequest request, Long userId) {
		return List.of();
	}
}
