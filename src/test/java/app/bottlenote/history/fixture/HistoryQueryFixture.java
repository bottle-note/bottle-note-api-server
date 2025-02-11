package app.bottlenote.history.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class HistoryQueryFixture {

	public PageResponse<UserHistorySearchResponse> getUserHistorySearchResponse() {
		UserHistoryDetail detail1 = UserHistoryDetail.builder()
			.historyId(1L)
			.createdAt(LocalDateTime.now())
			.eventCategory(EventCategory.PICK)
			.eventType(EventType.IS_PICK)
			.alcoholId(1L)
			.alcoholName("소주")
			.dynamicMessage(null)
			.imageUrl("imageUrl")
			.redirectUrl("redirectUrl")
			.build();

		UserHistoryDetail detail2 = UserHistoryDetail.builder()
			.historyId(1L)
			.createdAt(LocalDateTime.now())
			.eventCategory(EventCategory.RATING)
			.eventType(EventType.REVIEW_CREATE)
			.alcoholId(1L)
			.alcoholName("소주")
			.imageUrl("imageUrl")
			.redirectUrl("redirectUrl")
			.dynamicMessage(Map.of("currentValue", "4.0"))
			.build();

		Long total = 5L;

		List<UserHistoryDetail> details = List.of(detail1, detail2);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(4L)
			.pageSize(3L)
			.hasNext(true)
			.build();
		UserHistorySearchResponse response = UserHistorySearchResponse.of(total, LocalDateTime.now(), details);
		return PageResponse.of(response, cursorPageable);

	}


}
