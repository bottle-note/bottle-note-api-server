package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.global.service.converter.JsonArrayConverter;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenDto;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 추후에 로그인, 회원가입 관련 기능 추가시 proxy transactional 분리를 고려해야함.(ex. 로그인은 읽기전용, 회원가입은 쓰기전용.. 등)
public class OauthService {
	private final OauthRepository oauthRepository;
	private final NicknameGenerator nicknameGenerator;
	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationManager authenticationManager;
	private final JsonArrayConverter converter;


	public TokenDto oauthLogin(OauthRequest oauthReq) {
		String email = oauthReq.email();
		SocialType socialType = oauthReq.socialType();
		GenderType genderType = oauthReq.gender();
		Integer age = oauthReq.age();

		User user;
		User optionalUser = oauthRepository.findByEmail(email).orElse(null);

		if (Objects.isNull(optionalUser)) {
			log.debug("요청으로 들어온 유저가 DB에 존재하지 않음");
			user = oauthSignUp(email, socialType, genderType, age);
		} else {
			log.debug("요청으로 들어온 유저가 이미 가입된 유저임");
			user = optionalUser;
			user.addSocialType(oauthReq.socialType());
		}

		TokenDto token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());

		//재 로그인시 발급된 refresh token 업데이트
		user.updateRefreshToken(token.getRefreshToken());
		return token;
	}

	public User oauthSignUp(String email, SocialType socialType, GenderType genderType,
							Integer age) {

		String nickName = nicknameGenerator.generateNickname();

		log.info("nickname is :{}", nickName);
		User user = User.builder()
			.email(email)
			.socialType(converter.convertToEntityAttribute(socialType.toString()))
			.role(UserType.ROLE_USER)
			.gender(String.valueOf(genderType))
			.age(age)
			.nickName(nickName)
			.build();

		return oauthRepository.save(user);
	}

	public TokenDto refresh(String refreshToken) {

		//refresh Token 검증
		if (!JwtTokenValidator.validateToken(refreshToken)) {
			throw new UserException(INVALID_REFRESH_TOKEN);
		}

		Authentication authentication = authenticationManager.getAuthentication(
			refreshToken);

		log.info("USER ID is : {}", authentication.getName());

		//refresh Token DB에 존재하는지 검사
		User user = oauthRepository.findByRefreshToken(refreshToken).orElseThrow(
			() -> new UserException(INVALID_REFRESH_TOKEN)
		);
		TokenDto reissuedToken = tokenProvider.generateToken(user.getEmail(),
			user.getRole(), user.getId());

		// DB에 저장된 refresh 토큰을 재발급한 refresh 토큰으로 업데이트
		user.updateRefreshToken(reissuedToken.getRefreshToken());

		return reissuedToken;
	}

	public TokenDto guestLogin() {
		User user = User.builder()
			.email("guest")
			.nickName("guest")
			.role(UserType.ROLE_GUEST)
			.build();
		TokenDto token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.getRefreshToken());
		return token;
	}
}
