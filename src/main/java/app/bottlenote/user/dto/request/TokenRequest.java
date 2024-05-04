package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record TokenRequest(
	@NotNull(message = "accessToken 값은 Null일 수 없습니다.")
	String accessToken,
	@NotNull(message = "refreshToken 값은 Null 일 수 없습니다.")
	String refreshToken
) {

}
