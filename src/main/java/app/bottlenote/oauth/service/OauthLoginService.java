package app.bottlenote.oauth.service;

import app.bottlenote.oauth.constant.UserType;
import app.bottlenote.oauth.dto.OauthLoginRequest;
import app.bottlenote.security.JwtDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import app.bottlenote.user.domain.Users;
import app.bottlenote.oauth.repository.OauthLoginRepository;
import app.bottlenote.security.JwtTokenProvider;
@Service
@RequiredArgsConstructor
public class OauthLoginService {

	private final OauthLoginRepository oauthLoginRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public JwtDto oauthLogin(OauthLoginRequest oauthLoginReq) {
		Optional<Users> userCheck = oauthLoginRepository.findByEmailAndSocialType(
			oauthLoginReq.getEmail(), oauthLoginReq.getSocialType());

		Users user;

		if (userCheck.isPresent()) {
			user = userCheck.get();
		} else {
			user = oauthSignUp(oauthLoginReq);
		}
		return jwtTokenProvider.GenerateToken(user.getEmail(), user.getRole());
	}

	private Users oauthSignUp(OauthLoginRequest request) {
		return oauthLoginRepository.save(Users.builder()
			.email(request.getEmail())
			.socialType(request.getSocialType())
			.role(UserType.ROLE_USER)
			.build());
	}
}
