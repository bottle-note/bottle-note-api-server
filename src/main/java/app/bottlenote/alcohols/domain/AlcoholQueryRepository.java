package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.List;
import java.util.Optional;

/**
 * 알코올 조회 질의에 관한 애그리거트를 정의합니다.
 */
public interface AlcoholQueryRepository {

	Alcohol save(Alcohol alcohol);

	Optional<Alcohol> findById(Long alcoholId);

	List<Alcohol> findAll();

	List<Alcohol> findAllByIdIn(List<Long> ids);

	PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

	AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

	Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);

	List<CategoryItem> findAllCategories(AlcoholType type);

	Boolean existsByAlcoholId(Long alcoholId);

	Pair<Long, PageResponse<List<AlcoholDetailItem>>> getStandardExplore(Long userId,String keyword, Long cursor, Integer size);
}
