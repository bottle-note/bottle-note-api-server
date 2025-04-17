package app.bottlenote.user.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import lombok.Builder;

public record MyBottleRequest(
		String keyword, // 알코올 이름 검색 키워드
		Long regionId,
		MyBottleSortType sortType,
		SortOrder sortOrder,
		Long cursor,
		Long pageSize

) {
	@Builder
	public MyBottleRequest {
		sortType = sortType != null ? sortType : MyBottleSortType.LATEST;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 50L;
	}

}
