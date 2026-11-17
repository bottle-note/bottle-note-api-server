package app.bottlenote.history.domain;

import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.observability.annotation.SkipTrace;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholsViewHistoryRepository {

  AlcoholsViewHistory save(AlcoholsViewHistory entity);

  @SkipTrace
  <S extends AlcoholsViewHistory> List<S> saveAll(Iterable<S> entities);

  Optional<AlcoholsViewHistory> findById(AlcoholsViewHistoryId id);

  List<ViewHistoryItem> findPageByUserId(
      Long userId, LocalDateTime cursorViewAt, Long cursorAlcoholId, int limit);
}
