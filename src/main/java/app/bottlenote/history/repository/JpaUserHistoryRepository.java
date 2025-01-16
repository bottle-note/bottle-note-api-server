package app.bottlenote.history.repository;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.request.HistoryReviewFilterType;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserHistoryRepository extends UserHistoryRepository, JpaRepository<UserHistory, Long> {

	@Query("""
				SELECT new app.bottlenote.history.dto.response.UserHistoryDetail(
						u.id,
						u.createAt,
						u.eventCategory,
						u.eventType,
						u.alcoholId,
						a1_0.korName,
						u.imageUrl,
						u.redirectUrl,
						u.description,
						u.message,
						u.dynamicMessage
				)
				FROM user_history u
					 	left join alcohol a1_0 on u.alcoholId = a1_0.id
						left join rating r1_0 on u.alcoholId = r1_0.id.alcoholId	
								AND r1_0.id.userId = :userId
						left join picks p1_0 on u.alcoholId = p1_0.alcoholId
				WHERE u.userId = :userId
						AND (:historyReviewFilterType IS NULL OR u.eventType = :historyReviewFilterType)
		    			AND (:ratingPoint IS NULL OR r1_0.ratingPoint IN :ratingPoint)
		    			AND (:picksStatus IS NULL OR p1_0.status = :picksStatus)
		    			AND (:startDate IS NULL OR u.createAt >= :startDate)
		    			AND (:endDate IS NULL OR u.createAt <= :endDate)
		""")
	List<UserHistoryDetail> findUserHistoryListByUserId(
		@Param("userId") Long userId,
		@Param("historyReviewFilterType") HistoryReviewFilterType historyReviewFilterType,
		@Param("ratingPoint") List<RatingPoint> ratingPoint,
		@Param("picksStatus") PicksStatus picksStatuses,
		@Param("startDate") LocalDateTime startDate,
		@Param("endDate") LocalDateTime endDate,
		Pageable pageable);
}
