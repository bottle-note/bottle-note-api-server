package app.bottlenote.history.domain;

import app.bottlenote.history.dto.request.ReviewFilterType;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface UserHistoryRepository {
	UserHistory save(UserHistory userHistory);

	Optional<UserHistory> findById(Long id);

	List<UserHistory> findAll();

	void delete(UserHistory userHistory);

	List<UserHistoryDetail> findUserHistoryListByUserId(
		Long userId,
		ReviewFilterType reviewFilterType,
		List<RatingPoint> ratingPoint,
		PicksStatus picksStatuses,
		LocalDateTime startDate,
		LocalDateTime endDate,
		Pageable pageable);
}
