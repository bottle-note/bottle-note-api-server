package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.shared.alcohols.constant.AlcoholType;
import app.bottlenote.shared.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.shared.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.shared.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.shared.alcohols.dto.response.CategoryItem;
import app.bottlenote.shared.alcohols.payload.AlcoholSummaryItem;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

/** 알코올 조회 질의에 관한 애그리거트를 정의합니다. */
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

  Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer size);
}
