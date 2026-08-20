package app.bottlenote.curation.repository;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.dto.request.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCurationAdminRepository {

  Page<Curation> searchForAdmin(
      String keyword,
      Long specId,
      Boolean isActive,
      Pageable pageable,
      CurationSortType sortType,
      SortOrder sortOrder);
}
