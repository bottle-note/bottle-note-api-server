package app.bottlenote.alcohols.repository.custom;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.response.AlcoholInfo;

public interface CustomAlcoholQueryRepository {

	PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

	AlcoholDetailInfo findAlcoholDetailById(Long alcoholId, Long userId);

	AlcoholInfo findAlcoholById(Long alcoholId, Long userId);
}
