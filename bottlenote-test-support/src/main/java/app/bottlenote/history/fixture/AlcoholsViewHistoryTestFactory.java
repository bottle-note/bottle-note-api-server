package app.bottlenote.history.fixture;

import app.bottlenote.history.domain.AlcoholsViewHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AlcoholsViewHistoryTestFactory {

  @PersistenceContext private EntityManager em;

  @Transactional
  @NotNull
  public AlcoholsViewHistory persistAlcoholsViewHistory(
      @NotNull Long userId, @NotNull Long alcoholId, @NotNull LocalDateTime viewAt) {
    AlcoholsViewHistory history = AlcoholsViewHistory.of(userId, alcoholId, viewAt);
    em.persist(history);
    em.flush();
    return history;
  }

  @Transactional
  @NotNull
  public AlcoholsViewHistory persistAlcoholsViewHistory(
      @NotNull Long userId, @NotNull Long alcoholId) {
    return persistAlcoholsViewHistory(userId, alcoholId, LocalDateTime.now());
  }

  @Transactional
  @NotNull
  public List<AlcoholsViewHistory> persistAlcoholsViewHistories(
      @NotNull Long userId, @NotNull List<Long> alcoholIds, @NotNull LocalDateTime viewAt) {
    List<AlcoholsViewHistory> histories = new ArrayList<>();
    for (Long alcoholId : alcoholIds) {
      histories.add(persistAlcoholsViewHistory(userId, alcoholId, viewAt));
    }
    return histories;
  }

  @Transactional
  @NotNull
  public List<AlcoholsViewHistory> persistAlcoholsViewHistories(
      @NotNull Long userId, @NotNull List<Long> alcoholIds) {
    return persistAlcoholsViewHistories(userId, alcoholIds, LocalDateTime.now());
  }
}
