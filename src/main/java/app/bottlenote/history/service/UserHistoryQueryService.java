package app.bottlenote.history.service;

import static java.lang.Boolean.FALSE;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;
import java.time.LocalDateTime;
import java.util.List;
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
	public PageResponse<UserHistorySearchResponse> findUserHistoryList(final Long targetUserId, final UserHistorySearchRequest userHistorySearchRequest) {
		if (FALSE.equals(userFacade.existsByUserId(targetUserId))) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
		LocalDateTime subscriptionDate = userFacade.getSubscriptionDate(targetUserId);

		final int cursor = userHistorySearchRequest.cursor().intValue();
		final int pageSize = userHistorySearchRequest.pageSize().intValue();

		List<UserHistoryDetail> userHistoryDetails = userHistoryRepository.findUserHistoryListByUserId(
			targetUserId,
			userHistorySearchRequest
		);

		CursorPageable pageable = getCursorPageable(userHistoryDetails, cursor, pageSize);

		return PageResponse.of(UserHistorySearchResponse.of((long) userHistoryDetails.size(), subscriptionDate, userHistoryDetails), pageable);
	}

	private CursorPageable getCursorPageable(List<UserHistoryDetail> fetch, Integer cursor, Integer pageSize) {
		boolean hasNext = isHasNext(pageSize, fetch);
		return CursorPageable.builder()
			.currentCursor((long) cursor)
			.cursor((long) cursor + (long) pageSize)
			.pageSize((long) pageSize)
			.hasNext(hasNext)
			.build();
	}
	
	private boolean isHasNext(Integer pageSize, List<UserHistoryDetail> fetch) {
		boolean hasNext = fetch.size() > pageSize;

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
