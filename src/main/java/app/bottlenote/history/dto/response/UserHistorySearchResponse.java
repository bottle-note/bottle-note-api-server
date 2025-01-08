package app.bottlenote.history.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserHistorySearchResponse(
	Long totalCount,
	LocalDateTime subscriptionDate,
	List<UserHistoryDetail> userHistories
) {

	public static UserHistorySearchResponse of(Long totalCount, LocalDateTime subscriptionDate, List<UserHistoryDetail> userHistories) {
		return new UserHistorySearchResponse(totalCount, subscriptionDate, userHistories);
	}

}
