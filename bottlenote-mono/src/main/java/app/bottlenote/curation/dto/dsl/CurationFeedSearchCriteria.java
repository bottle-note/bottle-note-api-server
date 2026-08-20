package app.bottlenote.curation.dto.dsl;

import app.bottlenote.curation.constant.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import java.time.LocalDate;
import java.util.Set;

public record CurationFeedSearchCriteria(
    String keyword,
    Set<Long> specIds,
    Set<Long> keywordMatchedSpecIds,
    LocalDate today,
    CurationSortType sortType,
    SortOrder sortOrder,
    LocalDate lastExposureStartDate,
    Integer lastDisplayOrder,
    Long lastId,
    int fetchSize) {

  public CurationFeedSearchCriteria {
    specIds = Set.copyOf(specIds);
    keywordMatchedSpecIds = Set.copyOf(keywordMatchedSpecIds);
  }
}
