package app.bottlenote.history.domain;

import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.shared.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.shared.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@DomainRepository
public interface AlcoholsViewHistoryRepository {

  AlcoholsViewHistory save(AlcoholsViewHistory entity);

  <S extends AlcoholsViewHistory> List<S> saveAll(Iterable<S> entities);

  Optional<AlcoholsViewHistory> findById(AlcoholsViewHistoryId id);

  List<ViewHistoryItem> findAllByUserId(Long userId, Pageable pageable);

  int countByUserId(Long userId);
}
