package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.List;
import java.util.Optional;

public interface CustomAlcoholQueryRepository {

	PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

	AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

	Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);

	Pair<Long, PageResponse<List<AlcoholDetailItem>>> getStandardExplore(Long userId, String keyword, Long cursor, Integer size);
}
