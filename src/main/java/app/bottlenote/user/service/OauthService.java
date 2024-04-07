package app.bottlenote.user.service;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.repository.OauthRepository;
import app.bottlenote.common.jwt.JwtTokenProvider;
import org.springframework.transaction.annotation.Transactional;



@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OauthService {

	private final OauthRepository oauthRepository;
	private final JwtTokenProvider jwtTokenProvider;


	public OauthResponse oauthLogin(OauthRequest oauthReq) {

		String email = oauthReq.getEmail();
		SocialType socialType = oauthReq.getSocialType();
		GenderType genderType = oauthReq.getGender();
		int age = oauthReq.getAge();

		User user;

		user = oauthRepository.findByEmailAndSocialType(email, socialType)
				.orElseGet(() -> oauthSignUp(email, socialType, genderType, age));

		jwtTokenProvider.generateToken(email, UserType.ROLE_USER, user.getId());


		OauthResponse oauthResponse = jwtTokenProvider.generateToken(email, UserType.ROLE_USER, user.getId());
		user.updateRefreshToken(oauthResponse.getRefreshToken());

		return oauthResponse;
    }

	public User oauthSignUp(String email, SocialType socialType, GenderType genderType, int age) {

		NicknameGenerator nicknameGenerator = new NicknameGenerator();
		String nickName = nicknameGenerator.generateNickname();

		User user = User.builder()
			.email(email)
			.socialType(socialType)
			.role(UserType.ROLE_USER)
			.gender(String.valueOf(genderType))
			.age(age)
			.nickName(nickName)
			.build();

			return oauthRepository.save(user);

	}



}
