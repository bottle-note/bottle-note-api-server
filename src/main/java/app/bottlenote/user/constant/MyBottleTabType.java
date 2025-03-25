package app.bottlenote.user.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum MyBottleTabType {
	ALL("전체"),
	REVIEW("리뷰"),
	PICK("찜하기"),
	RATING("별점");

	private final String name;

	@JsonCreator
	public static MyBottleTabType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(MyBottleTabType.values())
			.filter(tabType -> tabType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(ALL);
	}
}
