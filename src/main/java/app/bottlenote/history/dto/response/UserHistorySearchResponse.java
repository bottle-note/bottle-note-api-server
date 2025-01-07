package app.bottlenote.history.dto.response;

import java.util.List;

public record UserHistorySearchResponse(
	Long totalCount,
	List<UserHistoryDetail> userHistories
) {

	public static UserHistorySearchResponse of(Long totalCount, List<UserHistoryDetail> userHistories) {
		return new UserHistorySearchResponse(totalCount, userHistories);
	}

}
