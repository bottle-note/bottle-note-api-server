package app.bottlenote.user.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum MyBottleSortType {

	LATEST("최신"),
	RATING("별점"),
	REVIEW("리뷰");

	private final String name;

	@JsonCreator
	public static MyBottleSortType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(MyBottleSortType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(LATEST);
	}
}
