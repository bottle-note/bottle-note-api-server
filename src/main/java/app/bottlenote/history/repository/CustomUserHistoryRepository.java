package app.bottlenote.history.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import org.springframework.data.repository.query.Param;

public interface CustomUserHistoryRepository {

  PageResponse<UserHistorySearchResponse> findUserHistoryListByUserId(
      @Param("userId") Long userId, @Param("request") UserHistorySearchRequest request);
}
