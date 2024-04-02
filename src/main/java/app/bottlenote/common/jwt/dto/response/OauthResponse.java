package app.bottlenote.common.jwt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OauthResponse {
	private String accessToken;
	private String refreshToken;
}

