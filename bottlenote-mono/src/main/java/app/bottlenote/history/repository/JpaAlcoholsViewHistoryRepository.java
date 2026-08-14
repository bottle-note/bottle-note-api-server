package app.bottlenote.history.repository;

import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.history.domain.AlcoholsViewHistory;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.history.domain.AlcoholsViewHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholsViewHistoryRepository
    extends AlcoholsViewHistoryRepository,
        JpaRepository<AlcoholsViewHistory, AlcoholsViewHistoryId> {

  @Query(
      """
      SELECT new app.bottlenote.alcohols.dto.response.ViewHistoryItem(
          a.id.alcoholId,
          al.korName,
          al.engName,
          CAST(ROUND((SELECT COALESCE(AVG(r.ratingPoint.rating), 0.0) FROM rating r WHERE r.id.alcoholId = a.id.alcoholId), 2) AS double),
          (SELECT COUNT(r) FROM rating r WHERE r.id.alcoholId = a.id.alcoholId),
          al.korCategory,
          al.engCategory,
          al.imageUrl,
          (CASE
              WHEN EXISTS (
                  SELECT 1 FROM picks p
                  WHERE p.alcoholId = a.id.alcoholId
                    AND p.userId = :userId
                    AND p.status = app.bottlenote.picks.constant.PicksStatus.PICK
              ) THEN true
              ELSE false END),
          COALESCE((SELECT CAST(MAX(pa.popularScore) AS double) FROM popular_alcohol pa WHERE pa.alcoholId = a.id.alcoholId), 0.0),
          a.viewAt)
      FROM alcohols_view_history a
      JOIN alcohol al ON al.id = a.id.alcoholId
      WHERE a.id.userId = :userId
        AND (:cursorViewAt IS NULL
             OR a.viewAt < :cursorViewAt
             OR (a.viewAt = :cursorViewAt AND a.id.alcoholId < :cursorAlcoholId))
      ORDER BY a.viewAt DESC, a.id.alcoholId DESC
      """)
  List<ViewHistoryItem> queryPageByUserId(
      @Param("userId") Long userId,
      @Param("cursorViewAt") LocalDateTime cursorViewAt,
      @Param("cursorAlcoholId") Long cursorAlcoholId,
      Pageable pageable);

  @Override
  default List<ViewHistoryItem> findPageByUserId(
      Long userId, LocalDateTime cursorViewAt, Long cursorAlcoholId, int limit) {
    return queryPageByUserId(userId, cursorViewAt, cursorAlcoholId, Pageable.ofSize(limit));
  }
}
