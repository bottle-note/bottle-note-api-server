package app.bottlenote.history.repository;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.response.UserHistoryDetail;
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
						join alcohol a1_0 on u.alcoholId = a1_0.id
				WHERE u.userId = :userId
				ORDER BY u.createAt DESC
		""")
	List<UserHistoryDetail> findUserHistoryListByUserId(@Param("userId") Long userId, Pageable pageable);
}
