package app.bottlenote.user.service;

import app.bottlenote.user.constant.UserType;
import app.bottlenote.common.jwt.dto.request.OauthRequest;
import app.bottlenote.common.jwt.dto.response.OauthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import app.bottlenote.user.domain.Users;
import app.bottlenote.user.repository.OauthRepository;
import app.bottlenote.common.jwt.JwtTokenProvider;
@Service
@RequiredArgsConstructor
public class OauthService {

	private final OauthRepository oauthLoginRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public OauthResponse oauthLogin(OauthRequest oauthReq) {
		Users user = oauthLoginRepository.findByEmailAndSocialType(
				oauthReq.getEmail(), oauthReq.getSocialType())
			.orElseGet(() -> oauthSignUp(oauthReq));

		return jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
	}

	private Users oauthSignUp(OauthRequest request) {
		return oauthLoginRepository.save(Users.builder()
			.email(request.getEmail())
			.socialType(request.getSocialType())
			.role(UserType.ROLE_USER)
			.gender(request.getGender())
				.age(request.getAge())
			.build());
	}
}
