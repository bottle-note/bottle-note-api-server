package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.Optional;
import java.util.Set;

/** 큐레이션 키워드 조회 질의에 관한 애그리거트를 정의합니다. */
public interface CurationKeywordRepository {

  Optional<CurationKeyword> findById(Long id);

  Optional<CurationKeyword> findByNameContainingAndIsActiveTrue(String name);

  CursorResponse<CurationKeywordResponse> searchCurationKeywords(
      String keyword, Long alcoholId, Long cursor, Integer pageSize);

  CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Integer pageSize);

  Optional<Set<Long>> findAlcoholIdsByKeyword(String keyword);
}
