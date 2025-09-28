package app.bottlenote.history.service;

import static java.lang.Boolean.FALSE;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserHistoryQueryService {

  private final UserHistoryRepository userHistoryRepository;
  private final UserFacade userFacade;

  @Transactional(readOnly = true)
  public PageResponse<UserHistorySearchResponse> findUserHistoryList(
      final Long targetUserId, final UserHistorySearchRequest userHistorySearchRequest) {
    if (FALSE.equals(userFacade.existsByUserId(targetUserId))) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    return userHistoryRepository.findUserHistoryListByUserId(
        targetUserId, userHistorySearchRequest);
  }
}
