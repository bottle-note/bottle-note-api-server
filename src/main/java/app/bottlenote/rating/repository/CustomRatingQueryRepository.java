package app.bottlenote.rating.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;

public interface CustomRatingQueryRepository {
	PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria);
}
