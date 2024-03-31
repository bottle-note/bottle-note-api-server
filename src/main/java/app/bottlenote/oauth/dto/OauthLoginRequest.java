package app.bottlenote.oauth.dto;

import app.bottlenote.oauth.constant.SocialType;
import lombok.Getter;

@Getter
public class OauthLoginRequest {

	private String email;
	private String gender;
	private String age;
	private SocialType socialType;

}
