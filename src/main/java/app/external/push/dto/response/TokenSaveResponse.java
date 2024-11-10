package app.external.push.dto.response;

import app.external.push.domain.Platform;
import app.external.push.dto.model.TokenMessage;

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
