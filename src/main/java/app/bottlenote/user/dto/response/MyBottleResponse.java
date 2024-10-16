package app.bottlenote.user.dto.response;

import app.bottlenote.global.service.cursor.CursorPageable;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

public record MyBottleResponse(
	Long userId,
	Boolean isMyPage,
	Long totalCount,
	List<MyBottleInfo> myBottleList,
	CursorPageable cursorPageable
) {

	@Builder
	public static MyBottleResponse create(
		Long userId,
		Boolean isMyPage,
		Long totalCount,
		List<MyBottleInfo> myBottleList,
		CursorPageable cursorPageable
	) {
		return new MyBottleResponse(
			userId,
			isMyPage,
			totalCount,
			myBottleList != null ? myBottleList : Collections.emptyList(),
			cursorPageable
		);
	}

	public record MyBottleInfo(
		Long alcoholId,
		String korName,
		String engName,
		String korCategoryName,
		String imageUrl,
		Boolean isPicked,
		Double rating,
		Boolean hasReviewByMe
	) {
	}
}
