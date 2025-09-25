package app.bottlenote.alcohols.repository;

import app.bottlenote.shared.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.shared.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.shared.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.shared.alcohols.payload.AlcoholSummaryItem;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public interface CustomAlcoholQueryRepository {

  PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

  AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

  Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);

  Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer size);
}
