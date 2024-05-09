package app.bottlenote.review.dto.request;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import lombok.Builder;

public record PageableRequest(
	SearchSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize
) {

	@Builder
	public PageableRequest {
		sortType = sortType != null ? sortType : SearchSortType.POPULAR;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
	}

}
