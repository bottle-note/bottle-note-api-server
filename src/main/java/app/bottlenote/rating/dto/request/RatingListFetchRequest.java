package app.bottlenote.rating.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.domain.constant.SearchSortType;
import lombok.Builder;

public record RatingListFetchRequest(
	String keyword,
	String category,
	Long regionId,

	SearchSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize

) {
	@Builder
	public RatingListFetchRequest {
		sortType = sortType != null ? sortType : SearchSortType.RANDOM;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
	}
}
