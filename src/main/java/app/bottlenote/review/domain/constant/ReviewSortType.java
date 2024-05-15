package app.bottlenote.review.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewSortType {
	POPULAR("인기순"),
	LIKES("좋아요순"),
	RATING("별점순"),
	BOTTLE_PRICE("보틀 가격"),
	GLASS_PRICE("잔 가격");

	private final String name;

	@JsonCreator
	public static ReviewSortType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(ReviewSortType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(POPULAR);
	}

}
