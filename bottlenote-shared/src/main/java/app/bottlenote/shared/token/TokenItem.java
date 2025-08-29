package app.bottlenote.shared.token;

import lombok.Builder;

@Builder
public record TokenItem(String accessToken, String refreshToken) {
	public static TokenItem of(String accessToken, String refreshToken) {
		return new TokenItem(accessToken, refreshToken);
	}
}
