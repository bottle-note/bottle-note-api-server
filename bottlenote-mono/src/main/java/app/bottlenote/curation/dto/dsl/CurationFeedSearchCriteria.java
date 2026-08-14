package app.bottlenote.curation.dto.dsl;

import java.time.LocalDate;
import java.util.Set;

public record CurationFeedSearchCriteria(
    String keyword,
    Set<Long> specIds,
    Set<Long> keywordMatchedSpecIds,
    LocalDate today,
    Integer lastDisplayOrder,
    Long lastId,
    int fetchSize) {

  public CurationFeedSearchCriteria {
    specIds = Set.copyOf(specIds);
    keywordMatchedSpecIds = Set.copyOf(keywordMatchedSpecIds);
  }
}
