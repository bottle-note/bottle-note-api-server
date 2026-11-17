package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.dto.response.TastingTagAlcoholCountItem;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 태그별 위스키 수를 Map으로 만든다. 빈 목록이면 쿼리 없이 빈 Map이고, 매핑이 없는 태그는 소비처가 0으로 채운다. */
final class TastingTagAlcoholCounts {

  private TastingTagAlcoholCounts() {}

  static Map<Long, Long> byTagId(
      AlcoholsTastingTagsRepository repository, Collection<Long> tastingTagIds) {
    if (tastingTagIds == null || tastingTagIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> counts = new HashMap<>();
    for (TastingTagAlcoholCountItem item : repository.countAlcoholsByTastingTagIds(tastingTagIds)) {
      counts.put(item.tastingTagId(), item.alcoholCount());
    }
    return counts;
  }
}
