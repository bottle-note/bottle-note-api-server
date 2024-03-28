package app.bottlenote.oauth.service;

import app.bottlenote.oauth.dto.OauthLoginRequest;
import app.bottlenote.security.JwtDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OauthLoginService {


	public JwtDto OauthLogin(OauthLoginRequest oauthLoginReq) {

		return null;

	}
}
