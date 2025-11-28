package app.bottlenote.banner.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TextPosition {
	LT("좌상단"),
	LB("좌하단"),
	RT("우상단"),
	RB("우하단"),
	CENTER("중앙");

	private final String description;

	@JsonCreator
	public static TextPosition parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return TextPosition.valueOf(source.toUpperCase());
	}
}
