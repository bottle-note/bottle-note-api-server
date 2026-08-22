package app.bottlenote.history.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface UserHistoryRepository {
  UserHistory save(UserHistory userHistory);

  Optional<UserHistory> findById(Long id);

  List<UserHistory> findAll();

  void delete(UserHistory userHistory);

  KeysetPageResponse<UserHistorySearchResponse> findUserHistoryListByUserId(
      Long userId, UserHistorySearchRequest userHistorySearchRequest);
}
