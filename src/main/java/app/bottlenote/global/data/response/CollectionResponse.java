package app.bottlenote.global.data.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor(staticName = "of")
public class CollectionResponse<T> {
	private Integer totalCount;
	private Collection<T> items;

	public static <T> CollectionResponse<T> empty() {
		return new CollectionResponse<>(0, Collections.emptyList());
	}

	public CollectionResponse<T> items(int totalCount, Collection<T> items) {
		if (items == null) {
			return empty();
		}
		this.totalCount = totalCount;
		this.items = items;
		return this;
	}
}
