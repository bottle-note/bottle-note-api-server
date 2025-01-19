package app.bottlenote.history.repository;

import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface CustomUserHistoryRepository {

	List<UserHistoryDetail> findUserHistoryListByUserId(
		@Param("userId") Long userId,
		@Param("request") UserHistorySearchRequest request
	);

}
