package app.bottlenote.review.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewSortType {
	DATE("날짜 순"),
	LIKES("좋아요 순");

	private final String name;

	@JsonCreator
	public static ReviewSortType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(ReviewSortType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(DATE);
	}

}
