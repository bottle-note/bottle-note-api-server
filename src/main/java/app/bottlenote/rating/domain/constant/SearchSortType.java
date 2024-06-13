package app.bottlenote.rating.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum SearchSortType {
	RANDOM("랜덤"),
	POPULAR("인기"),
	RATING("별점"),
	PICK("찜하기"),
	REVIEW("리뷰");

	private final String name;

	@JsonCreator
	public static SearchSortType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(SearchSortType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(POPULAR);
	}
}
