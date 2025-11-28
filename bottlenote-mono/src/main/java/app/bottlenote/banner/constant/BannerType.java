package app.bottlenote.banner.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BannerType {
	SURVEY("설문지"),
	CURATION("큐레이션"),
	AD("광고"),
	PARTNERSHIP("제휴");

	private final String description;

	@JsonCreator
	public static BannerType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return BannerType.valueOf(source.toUpperCase());
	}
}
