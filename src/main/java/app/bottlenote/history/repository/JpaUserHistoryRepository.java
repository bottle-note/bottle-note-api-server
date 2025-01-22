package app.bottlenote.history.repository;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserHistoryRepository extends UserHistoryRepository, JpaRepository<UserHistory, Long>, CustomUserHistoryRepository {
}
