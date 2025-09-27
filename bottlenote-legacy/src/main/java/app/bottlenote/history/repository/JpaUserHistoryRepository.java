package app.bottlenote.history.repository;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.shared.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaUserHistoryRepository
    extends UserHistoryRepository, JpaRepository<UserHistory, Long>, CustomUserHistoryRepository {}
