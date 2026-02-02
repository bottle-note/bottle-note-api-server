package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCurationKeywordRepository {

  CursorResponse<CurationKeywordResponse> searchCurationKeywords(
      String keyword, Long alcoholId, Long cursor, Integer pageSize);

  CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Integer pageSize);

  Optional<Set<Long>> findAlcoholIdsByKeyword(String keyword);

  Page<AdminCurationListResponse> searchForAdmin(
      AdminCurationSearchRequest request, Pageable pageable);
}
