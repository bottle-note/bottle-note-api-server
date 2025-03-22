package app.bottlenote.alcohols.repository.custom;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.Optional;

public interface CustomAlcoholQueryRepository {

	PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

	AlcoholDetailInfo findAlcoholDetailById(Long alcoholId, Long userId);

	Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long userId);
}
