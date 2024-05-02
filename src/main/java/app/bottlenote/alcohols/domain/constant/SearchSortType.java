package app.bottlenote.alcohols.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum SearchSortType {
	POPULAR("인기순"),
	HIGH_RATING("높은 별점순"),
	LOW_RATING("낮은 별점순"),
	LIKE("찜 많은순"),
	REVIEW("리뷰 많은순");

	private final String name;


	@JsonCreator
	public static SearchSortType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(SearchSortType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(null);
	}
}
