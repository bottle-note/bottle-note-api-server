package app.bottlenote.user.dto.response;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@EqualsAndHashCode
public class TokenDto {

	private final String accessToken;
	private final String refreshToken;

	@Builder
	public TokenDto(String accessToken, String refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}

