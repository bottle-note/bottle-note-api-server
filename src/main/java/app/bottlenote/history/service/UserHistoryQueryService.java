package app.bottlenote.history.service;

import static java.lang.Boolean.FALSE;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserHistoryQueryService {

	private final UserHistoryRepository userHistoryRepository;
	private final UserFacade userFacade;

	@Transactional(readOnly = true)
	public PageResponse<UserHistorySearchResponse> findUserHistoryList(Long targetUserId, Integer cursor, Integer pageSize) {
		if (FALSE.equals(userFacade.existsByUserId(targetUserId))) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
		List<UserHistoryDetail> userHistoryDetails = userHistoryRepository.findUserHistoryListByUserId(targetUserId, PageRequest.of(cursor, pageSize));

		CursorPageable pageable = getCursorPageable(userHistoryDetails, cursor, pageSize);
		return PageResponse.of(UserHistorySearchResponse.of((long) userHistoryDetails.size(), userHistoryDetails), pageable);
	}

	public CursorPageable getCursorPageable(List<UserHistoryDetail> fetch, Integer cursor, Integer pageSize) {
		boolean hasNext = isHasNext(pageSize, fetch);
		return CursorPageable.builder()
			.currentCursor((long) cursor)
			.cursor((long) cursor + (long) pageSize)  // 다음 페이지가 있는 경우 마지막으로 가져온 ID를 다음 커서로 사용
			.pageSize((long) pageSize)
			.hasNext(hasNext)
			.build();
	}

	public boolean isHasNext(Integer pageSize, List<UserHistoryDetail> fetch) {
		boolean hasNext = fetch.size() > pageSize;

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

}
