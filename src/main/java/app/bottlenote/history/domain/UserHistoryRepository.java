package app.bottlenote.history.domain;

import java.util.List;
import java.util.Optional;

public interface UserHistoryRepository {
	UserHistory save(UserHistory userHistory);

	Optional<UserHistory> findById(Long id);

	List<UserHistory> findAll();

	void delete(UserHistory userHistory);
}
