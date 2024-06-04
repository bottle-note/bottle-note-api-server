package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryResponse;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
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

	AlcoholDetailInfo findAlcoholDetailById(Long alcoholId, Long userId);

	List<CategoryResponse> findAllCategories(AlcoholType type);
}
