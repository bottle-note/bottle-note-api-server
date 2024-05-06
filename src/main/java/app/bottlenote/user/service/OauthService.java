package app.bottlenote.user.service;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;

import app.bottlenote.global.security.SecurityUtil;
import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.TokenRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 추후에 로그인, 회원가입 관련 기능 추가시 proxy transactional 분리를 고려해야함.(ex. 로그인은 읽기전용, 회원가입은 쓰기전용.. 등)
public class OauthService {

	private final OauthRepository oauthRepository;
	private final NicknameGenerator nicknameGenerator;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationManager jwtAuthenticationManager;
	private final SecurityUtil securityUtil;


	public OauthResponse oauthLogin(OauthRequest oauthReq) {

		String email = oauthReq.email();
		SocialType socialType = oauthReq.socialType();
		GenderType genderType = oauthReq.gender();
		Integer age = oauthReq.age();

		User user;

		// 회원가입용 옵셔널 유저필요
		User optionalUser = oauthRepository.findByEmailAndSocialType(email, socialType)
			.orElse(null);

		//db에 유저가 존재하지 않는다면 회원가입, 존재한다면 토큰 발급 후 반환
		if (optionalUser == null) {
			user = oauthSignUp(email, socialType, genderType, age);
		} else {
			user = optionalUser;
		}
		OauthResponse oauthResponse = jwtTokenProvider.generateToken(user.getEmail(),
			user.getRole(),
			user.getId());

		user.updateRefreshToken(oauthResponse.getRefreshToken());
		//db에 리프레쉬 토큰 저장
		oauthRepository.save(user);

		return oauthResponse;
	}

	public User oauthSignUp(String email, SocialType socialType, GenderType genderType,
		Integer age) {

		String nickName = nicknameGenerator.generateNickname();

		log.info("nickname is :{}", nickName);

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

	public OauthResponse refresh(TokenRequest tokenRequest) {

		//refresh Token 검증
		if (!JwtTokenValidator.validateToken(tokenRequest.refreshToken())) {
			throw new UserException(INVALID_REFRESH_TOKEN);
		}

		Authentication authentication = jwtAuthenticationManager.getAuthentication(
			tokenRequest.accessToken());

		log.info("USER ID is : {}", authentication.getName());

		//refresh Token DB에 존재하는지 검사
		User user = oauthRepository.findByRefreshToken(tokenRequest.refreshToken()).orElseThrow(
			() -> new UserException(INVALID_REFRESH_TOKEN)
		);
		OauthResponse reissuedToken = jwtTokenProvider.generateToken(user.getEmail(),
			user.getRole(), user.getId());

		// DB에 저장된 refresh 토큰을 재발급한 refresh 토큰으로 업데이트
		user.updateRefreshToken(reissuedToken.getRefreshToken());

		return reissuedToken;
	}

	public String getCurrentUser() {
		log.info("info {}", SecurityContextHolder.getContext().getAuthentication());
		return String.valueOf(securityUtil.getCurrentUserId());
	}
}
