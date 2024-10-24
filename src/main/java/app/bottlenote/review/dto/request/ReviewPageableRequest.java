package app.bottlenote.review.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import lombok.Builder;

public record ReviewPageableRequest(
	ReviewSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize
) {

	@Builder
	public ReviewPageableRequest {
		sortType = sortType != null ? sortType : ReviewSortType.POPULAR;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
	}

}
