package app.bottlenote.global.data.response;


import app.bottlenote.global.service.cursor.CursorResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class CollectionResponse<T> {
	private long totalCount;
	private Collection<T> items;

	public static <T> CollectionResponse<T> empty() {
		return new CollectionResponse<>(0, Collections.emptyList());
	}

	public static <T> CollectionResponse<T> of(long totalCount, CursorResponse<T> cursorItem) {
		if (cursorItem == null) {
			return empty();
		}
		return new CollectionResponse<>(Math.toIntExact(totalCount), cursorItem.items());
	}

	public static <T> CollectionResponse<T> of(long totalCount, List<T> item) {
		if (item == null) {
			return empty();
		}
		return new CollectionResponse<>(Math.toIntExact(totalCount), item);
	}
}
