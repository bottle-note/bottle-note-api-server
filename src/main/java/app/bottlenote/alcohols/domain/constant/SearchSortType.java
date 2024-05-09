package app.bottlenote.alcohols.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum SearchSortType {
	POPULAR("인기순"),
	RATING("별점순"),
	PICK("찜순"),
	REVIEW("리뷰순");

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
