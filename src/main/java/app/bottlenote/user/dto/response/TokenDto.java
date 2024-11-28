package app.bottlenote.user.dto.response;

import lombok.Builder;


@Builder
public record TokenDto(
	String accessToken,
	String refreshToken
) {
	public static TokenDto of(String accessToken, String refreshToken) {
		return new TokenDto(accessToken, refreshToken);
	}
}
