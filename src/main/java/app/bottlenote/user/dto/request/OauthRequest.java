package app.bottlenote.user.dto.request;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OauthRequest
	(@NotBlank(message = "로그인 및 회원가입에 필요한 이메일이 없습니다.")
	 @Email(message = "올바른 이메일형식이 아닙니다.") String email,
	 @NotNull(message = "로그인 및 회원가입에 필요한 소셜타입이 없습니다.")
	 SocialType socialType,
	 GenderType gender,
	 @Min(value = 0, message = "나이가 잘못 입력됐습니다.")
	 Integer age
	) {

}
