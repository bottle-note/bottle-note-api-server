package app.bottlenote.history.domain;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import java.util.List;
import java.util.Optional;

public interface UserHistoryRepository {
	UserHistory save(UserHistory userHistory);

	Optional<UserHistory> findById(Long id);

	List<UserHistory> findAll();

	void delete(UserHistory userHistory);

	PageResponse<UserHistorySearchResponse> findUserHistoryListByUserId(
		Long userId,
		UserHistorySearchRequest userHistorySearchRequest
	);
}
