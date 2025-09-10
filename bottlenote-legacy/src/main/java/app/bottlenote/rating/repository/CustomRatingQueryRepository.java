package app.bottlenote.rating.repository;

import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.shared.cursor.PageResponse;

public interface CustomRatingQueryRepository {
  PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria);
}
