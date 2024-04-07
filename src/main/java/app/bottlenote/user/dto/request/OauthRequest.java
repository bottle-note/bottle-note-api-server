package app.bottlenote.user.dto.request;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
public class OauthRequest {

	@NotNull(message = "로그인 및 회원가입에 필요한 이메일이 없습니다.")
	@NotBlank(message = "로그인 및 회원가입에 필요한 이메일이 없습니다.")
	@Email(message = "올바른 이메일형식이 아닙니다.")
	private final String email;

	@NotNull(message = "로그인 및 회원가입에 필요한 소셜타입이 없습니다.")
	private final SocialType socialType;

	private final GenderType gender;

	private final int age;

	@Builder
	public OauthRequest(String email, GenderType gender, int age, SocialType socialType) {
		this.email = email;
		this.gender = gender;
		this.age = age;
		this.socialType = socialType;
	}
}
