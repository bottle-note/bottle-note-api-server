package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.PopularItem;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PopularQueryRepository {

  List<PopularItem> getPopularOfWeeks(Long userId, Pageable size);

  List<PopularItem> getSpringItems(
      Long userId, List<Long> tags, List<Long> excludedTags, Pageable size);
}
