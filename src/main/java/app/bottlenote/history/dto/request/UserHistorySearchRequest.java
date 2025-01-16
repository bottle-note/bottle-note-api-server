package app.bottlenote.history.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.history.exception.UserHistoryException;
import app.bottlenote.history.exception.UserHistoryExceptionCode;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import java.time.LocalDateTime;
import java.util.List;

public record UserHistorySearchRequest(
	List<RatingPoint> ratingPoint,
	HistoryReviewFilterType historyReviewFilterType,
	PicksStatus picksStatus,
	LocalDateTime startDate,
	LocalDateTime endDate,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize
) {

	public UserHistorySearchRequest {
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
		endDate = endDate != null ? endDate.plusDays(1) : LocalDateTime.now().plusDays(1);
		startDate = startDate != null ? startDate : LocalDateTime.now().minusYears(2);
		if (endDate.isBefore(startDate)) {
			throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE);
		}
		if (startDate.plusYears(2).plusDays(1).isBefore(endDate)) {
			throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE_RANGE);
		}
	}
}
