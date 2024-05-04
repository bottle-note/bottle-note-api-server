package app.bottlenote.user.dto.response;

import lombok.Builder;
import lombok.Getter;


@Getter
public class OauthResponse {

	private final String accessToken;
	private final String refreshToken;

	@Builder
	public OauthResponse(String accessToken, String refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}

