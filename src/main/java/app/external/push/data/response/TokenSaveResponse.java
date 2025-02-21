package app.external.push.data.response;

import app.external.notification.domain.constant.Platform;
import app.external.push.data.payload.TokenMessage;

public record TokenSaveResponse(
	String deviceToken,
	Platform platform,
	TokenMessage message
) {
	public static TokenSaveResponse of(
		String deviceToken,
		Platform platform,
		TokenMessage message
	) {
		return new TokenSaveResponse(deviceToken, platform, message);
	}
}
