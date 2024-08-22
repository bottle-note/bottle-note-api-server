package app.bottlenote.user.dto.dsl;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.domain.constant.MyBottleSortType;
import app.bottlenote.user.domain.constant.MyBottleTabType;
import app.bottlenote.user.dto.request.MyBottleRequest;

public record MyBottlePageableCriteria(
	Long userId,
	String keyword, // 알코올 이름 검색 키워드
	Long regionId,
	MyBottleTabType tabType,
	MyBottleSortType sortType,
	SortOrder sortOrder,
	Long cursor,
	Long pageSize,
	Long currentUserId
) {
	public static MyBottlePageableCriteria of(MyBottleRequest request, Long userId, Long currentUserId) {
		return new MyBottlePageableCriteria(
			userId,
			request.keyword(),
			request.regionId(),
			request.tabType(),
			request.sortType(),
			request.sortOrder(),
			request.cursor(),
			request.pageSize(),
			currentUserId
		);
	}
}
