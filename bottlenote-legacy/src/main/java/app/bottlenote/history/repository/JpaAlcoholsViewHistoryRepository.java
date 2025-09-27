package app.bottlenote.history.repository;

import app.bottlenote.history.domain.AlcoholsViewHistory;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.history.domain.AlcoholsViewHistoryRepository;
import app.bottlenote.shared.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.shared.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholsViewHistoryRepository
    extends AlcoholsViewHistoryRepository,
        JpaRepository<AlcoholsViewHistory, AlcoholsViewHistoryId> {

  @Override
  @Query(
      """
             SELECT
              new app.bottlenote.shared.alcohols.dto.response.ViewHistoryItem(
                 			    a.id.alcoholId,
                 			    al.korName,
                 			    al.engName,
                 			    CAST(ROUND((SELECT COALESCE(AVG(r.ratingPoint.rating), 0.0) FROM rating r WHERE r.id.alcoholId = a.id.alcoholId),2) AS double),
                 			    (SELECT COUNT(r) FROM rating r WHERE r.id.alcoholId = a.id.alcoholId),
                 			    al.korCategory,
                 			    al.engCategory,
                 			    al.imageUrl,
                 			    (CASE
            			     			    WHEN EXISTS ( SELECT 1 FROM picks p
            												WHERE p.alcoholId = a.id.alcoholId
															 AND p.userId = :userId
															 AND p.status = app.bottlenote.picks.constant.PicksStatus.PICK
                			     ) THEN true
                						     ELSE false END),
                 			    COALESCE((SELECT CAST(MAX(pa.popularScore) AS double) FROM popular_alcohol pa WHERE pa.alcoholId = a.id.alcoholId), 0.0)
                 			)
                 			FROM alcohols_view_history a
                 			JOIN alcohol al ON al.id = a.id.alcoholId
                 			WHERE a.id.userId = :userId
                 			ORDER BY a.viewAt DESC
            """)
  List<ViewHistoryItem> findAllByUserId(Long userId, Pageable pageable);

  @Override
  @Query(" SELECT COUNT(*) FROM alcohols_view_history a WHERE a.id.userId = :userId ")
  int countByUserId(Long userId);
}
