package app.bottlenote.support.help.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HelpType {
	WHISKEY("위스키 관련 문의"),
	REVIEW("리뷰 관련 문의"),
	USER("회원 관련 문의"),
	ETC("그 외 모든 문의");

	private final String description;

	@JsonCreator
	public static HelpType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return HelpType.valueOf(source.toUpperCase());
	}
}
