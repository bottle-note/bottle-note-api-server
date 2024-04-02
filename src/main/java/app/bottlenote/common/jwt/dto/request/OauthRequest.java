package app.bottlenote.common.jwt.dto.request;

import app.bottlenote.user.constant.SocialType;
import lombok.Getter;

@Getter
public class OauthRequest {

	private String email;
	private String gender;
	private String age;
	private SocialType socialType;

}
