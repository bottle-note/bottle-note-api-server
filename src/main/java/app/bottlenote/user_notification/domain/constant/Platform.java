package app.bottlenote.user_notification.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Platform {
	ANDROID, IOS;

	@JsonCreator
	public static Platform fromString(String value) {
		if (value == null) {
			return null;
		}
		return Platform.valueOf(value.toUpperCase());
	}
}
