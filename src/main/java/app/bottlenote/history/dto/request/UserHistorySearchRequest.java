package app.bottlenote.history.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.history.exception.UserHistoryException;
import app.bottlenote.history.exception.UserHistoryExceptionCode;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import java.time.LocalDate;
import java.util.List;

public record UserHistorySearchRequest(
	List<RatingPoint> ratingPoint,
	ReviewFilterType reviewFilterType,
	PicksStatus picksStatus,
	LocalDate startDate,
	LocalDate endDate,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize
) {

	public UserHistorySearchRequest {
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
		endDate = endDate != null ? endDate : LocalDate.now();
		startDate = startDate != null ? startDate : LocalDate.now().minusYears(2);
		if (endDate.isBefore(startDate)) {
			throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE);
		}
		if (startDate.plusYears(2).isBefore(endDate)) {
			throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE_RANGE);
		}
	}

}
