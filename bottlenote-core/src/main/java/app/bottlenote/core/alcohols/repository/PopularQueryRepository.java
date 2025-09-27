package app.bottlenote.core.alcohols.repository;

import app.bottlenote.shared.alcohols.dto.response.PopularItem;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** 인기 알코올 조회 질의에 관한 애그리거트를 정의합니다. */
public interface PopularQueryRepository {

  List<PopularItem> getPopularOfWeeks(Long userId, Pageable size);

  List<PopularItem> getSpringItems(
      Long userId,
      List<Long> tags,
      List<Long> excludedTags,
      Pageable size);
}
