package app.bottlenote.global.service.cursor;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CursorPageable {
	private final Long currentCursor;
	private final Long cursor;
	private final Long pageSize;
	private final Boolean hasNext;

	/**
	 * Instantiates a new Cursor pageable.
	 *
	 * @param currentCursor 현재 페이지의 커서 위치 ex) 최초 조회 시 0
	 * @param cursor        다음 페이지를 위한 커서 위치 ex) 최초 조회 시 currentCursor + pageSize +1
	 * @param pageSize      페이지당 항목 조회할 갯수
	 * @param hasNext       다음 페이지 존재 여부
	 */
	@Builder
	public CursorPageable(Long currentCursor, Long cursor, Long pageSize, Boolean hasNext) {
		this.currentCursor = currentCursor;
		this.cursor = cursor;
		this.pageSize = pageSize;
		this.hasNext = hasNext;
	}
}
