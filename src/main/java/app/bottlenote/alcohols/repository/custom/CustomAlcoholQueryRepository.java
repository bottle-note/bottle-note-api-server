package app.bottlenote.alcohols.repository.custom;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSummaryItem;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.Optional;

public interface CustomAlcoholQueryRepository {

	PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

	AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

	Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);
}
