package app.bottlenote.history.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;

import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholsViewHistoryRepository {

	AlcoholsViewHistory save(AlcoholsViewHistory entity);

	<S extends AlcoholsViewHistory> List<S> saveAll(Iterable<S> entities);

	Optional<AlcoholsViewHistory> findById(AlcoholsViewHistoryId id);
}
