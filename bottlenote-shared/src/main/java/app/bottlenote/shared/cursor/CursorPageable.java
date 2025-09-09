package app.bottlenote.shared.cursor;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"currentCursor", "cursor", "pageSize", "hasNext"})
public class CursorPageable {
	private final Long currentCursor;
	private final Long cursor;
	private final Long pageSize;
	private final Boolean hasNext;

	@Builder
	public CursorPageable(Long currentCursor, Long cursor, Long pageSize, Boolean hasNext) {
		this.currentCursor = currentCursor;
		this.cursor = cursor;
		this.pageSize = pageSize;
		this.hasNext = hasNext;
	}

	public static <T> CursorPageable of(List<T> items, Long currentCursor, Long pageSize) {
		boolean hasNext = items.size() > pageSize;
		List<T> result = hasNext ? items.subList(0, items.size() - 1) : items;

		return CursorPageable.builder()
			.currentCursor(currentCursor)
			.cursor(currentCursor + pageSize)
			.pageSize(pageSize)
			.hasNext(hasNext)
			.build();
	}

	public static <T> CursorPageable of(List<T> items, Long currentCursor, Integer pageSize) {
		return of(items, currentCursor, pageSize.longValue());
	}
}