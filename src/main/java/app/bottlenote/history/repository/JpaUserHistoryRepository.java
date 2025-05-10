package app.bottlenote.history.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaUserHistoryRepository extends
		UserHistoryRepository,
		JpaRepository<UserHistory, Long>,
		CustomUserHistoryRepository {
}
