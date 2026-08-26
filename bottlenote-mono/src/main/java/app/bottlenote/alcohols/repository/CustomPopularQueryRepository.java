package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.response.PopularItem;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Product 인기도 Snapshot 조회를 위한 QueryDSL Custom Repository */
public interface CustomPopularQueryRepository {

  List<PopularItem> getPopularOfWeeks(Long userId, Pageable pageable);

  List<PopularItem> getSpringItems(
      Long userId, List<Long> tags, List<Long> excludedTags, Pageable pageable);

  List<PopularItem> getPopularByInterestWeekly(Long userId, int limit);

  List<PopularItem> getPopularByInterestMonthly(Long userId, int limit);
}
