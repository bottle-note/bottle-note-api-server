package app.bottlenote.rating.repository;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;

public interface CustomRatingQueryRepository {
  KeysetPageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria);
}
