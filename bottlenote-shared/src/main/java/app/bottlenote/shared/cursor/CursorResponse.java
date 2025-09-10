package app.bottlenote.shared.cursor;

import java.util.List;

public record CursorResponse<T>(List<T> items, CursorPageable pageable) {
	public static <T> CursorResponse<T> of(List<T> items, CursorPageable pageable) {
		return new CursorResponse<>(items, pageable);
	}

	public static <T> CursorResponse<T> of(List<T> items, Long cursor, Integer size) {
		CursorPageable pageable = CursorPageable.of(items, cursor, size);
		return new CursorResponse<>(items, pageable);
	}

	public static <T> CursorResponse<T> from(PageResponse<List<T>> pageResponse) {
		return new CursorResponse<>(pageResponse.content(), pageResponse.cursorPageable());
	}

	public PageResponse<List<T>> toPageResponse() {
		return PageResponse.of(items, pageable);
	}
}