package app.bottlenote.history.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.history.domain.AlcoholsViewHistory;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.history.domain.AlcoholsViewHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAlcoholsViewHistoryRepository
    extends AlcoholsViewHistoryRepository,
        JpaRepository<AlcoholsViewHistory, AlcoholsViewHistoryId> {}
