package app.bottlenote.user.service;

import app.bottlenote.common.jwt.JwtTokenProvider;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NICKNAME_NOT_VALID;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 추후에 로그인, 회원가입 관련 기능 추가시 proxy transactional 분리를 고려해야함.(ex. 로그인은 읽기전용, 회원가입은 쓰기전용.. 등)
public class OauthService {

	private final OauthRepository oauthRepository;
	private final JwtTokenProvider jwtTokenProvider;


	public OauthResponse oauthLogin(OauthRequest oauthReq) {

		String email = oauthReq.getEmail();
		SocialType socialType = oauthReq.getSocialType();
		GenderType genderType = oauthReq.getGender();
		int age = oauthReq.getAge();

		User user;

		// 회원가입용 옵셔널 유저필요
		User optionalUser = oauthRepository.findByEmailAndSocialType(email, socialType).orElse(null);

		if (optionalUser == null) {
			user = oauthSignUp(email, socialType, genderType, age);
		} else {
			user = optionalUser;
		}

		jwtTokenProvider.generateToken(email, UserType.ROLE_USER, user.getId());

		OauthResponse oauthResponse = jwtTokenProvider.generateToken(email, UserType.ROLE_USER, user.getId());


		user.updateRefreshToken(oauthResponse.getRefreshToken());

		return oauthResponse;
	}

	public User oauthSignUp(String email, SocialType socialType, GenderType genderType, int age) {

		NicknameGenerator nicknameGenerator = NicknameGenerator.getInstance();
		String nickName = nicknameGenerator.generate();

		int attempts = 0;
		while (oauthRepository.findByNickName(nickName).isPresent()) {
			if (attempts >= 10) {
				throw new UserException(USER_NICKNAME_NOT_VALID);
			}
			nickName = nicknameGenerator.generate();
			attempts++;
		}

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
