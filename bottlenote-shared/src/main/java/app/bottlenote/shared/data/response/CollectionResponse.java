package app.bottlenote.shared.data.response;

import app.bottlenote.shared.cursor.CursorResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

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