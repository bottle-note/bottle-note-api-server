package app.bottlenote.rating.service;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import org.springframework.stereotype.Service;

@Service
public class RatingQueryService {
	private final RatingRepository ratingRepository;

	public RatingQueryService(RatingRepository ratingRepository) {
		this.ratingRepository = ratingRepository;
	}

	public PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchRequest request, Long userId) {
		var criteria = RatingListFetchCriteria.of(request, userId);
		return ratingRepository.fetchRatingList(criteria);
	}
}
