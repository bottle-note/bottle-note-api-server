package app.external.push.domain;

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
