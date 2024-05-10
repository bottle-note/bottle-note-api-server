package app.bottlenote.review.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import lombok.Builder;

public record PageableRequest(
	ReviewSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize
) {

	@Builder
	public PageableRequest {
		sortType = sortType != null ? sortType : ReviewSortType.DATE;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.ASC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
	}

}
