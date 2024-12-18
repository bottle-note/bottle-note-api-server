package app.bottlenote.user.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.domain.constant.MyBottleSortType;
import app.bottlenote.user.domain.constant.MyBottleTabType;
import lombok.Builder;

public record MyBottleRequest(
	String keyword, // 알코올 이름 검색 키워드
	Long regionId,
	MyBottleTabType tabType,
	MyBottleSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize

) {
	@Builder
	public MyBottleRequest {
		tabType = tabType != null ? tabType : MyBottleTabType.ALL;
		sortType = sortType != null ? sortType : MyBottleSortType.LATEST;
		sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 50L;
	}

}
