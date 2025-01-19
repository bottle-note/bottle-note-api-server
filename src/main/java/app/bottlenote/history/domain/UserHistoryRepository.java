package app.bottlenote.history.domain;

import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import java.util.List;
import java.util.Optional;

public interface UserHistoryRepository {
	UserHistory save(UserHistory userHistory);

	Optional<UserHistory> findById(Long id);

	List<UserHistory> findAll();

	void delete(UserHistory userHistory);

	List<UserHistoryDetail> findUserHistoryListByUserId(
		Long userId,
		UserHistorySearchRequest userHistorySearchRequest
	);
}
