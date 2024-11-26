package app.bottlenote.rating.service;

import app.bottlenote.alcohols.service.domain.AlcoholDomainSupport;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.user.service.domain.UserFacade;
import org.springframework.stereotype.Service;

@Service
public class RatingQueryService {
	private final RatingRepository ratingRepository;
	private final UserFacade userDomainSupport;
	private final AlcoholDomainSupport alcoholDomainSupport;

	public RatingQueryService(
		RatingRepository ratingRepository,
		UserFacade userDomainSupport,
		AlcoholDomainSupport alcoholDomainSupport
	) {
		this.ratingRepository = ratingRepository;
		this.userDomainSupport = userDomainSupport;
		this.alcoholDomainSupport = alcoholDomainSupport;
	}

	public PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchRequest request, Long userId) {
		var criteria = RatingListFetchCriteria.of(request, userId);
		return ratingRepository.fetchRatingList(criteria);
	}

	public UserRatingResponse fetchUserRating(Long alcoholId, Long userId) {
		userDomainSupport.isValidUserId(userId);
		alcoholDomainSupport.isValidAlcoholId(alcoholId);

		return ratingRepository.fetchUserRating(alcoholId, userId)
			.orElse(UserRatingResponse.empty(alcoholId, userId));
	}
}
