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

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;


@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {
	private final OauthRepository oauthRepository;
	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationManager authenticationManager;
	private final JsonArrayConverter converter;
	private final SecureRandom randomValue = new SecureRandom();

	@Transactional
	public TokenDto login(OauthRequest oauthReq) {
		final String email = oauthReq.email();
		final SocialType socialType = oauthReq.socialType();
		final GenderType genderType = oauthReq.gender();
		final Integer age = oauthReq.age();

		User user = oauthRepository.findByEmail(email).orElseGet(() -> oauthSignUp(email, socialType, genderType, age, UserType.ROLE_USER));
		user.addSocialType(oauthReq.socialType());
		TokenDto token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.refreshToken());
		return token;
	}

	@Transactional
	public TokenDto guestLogin() {
		User guest = oauthRepository.loadGuestUser()
			.orElseGet(() -> oauthSignUp("guest@bottlenote.com", SocialType.APPLE, GenderType.MALE, 30, UserType.ROLE_GUEST));
		return tokenProvider.generateToken(guest.getEmail(), guest.getRole(), guest.getId());
	}

	public User oauthSignUp(
		String email,
		SocialType socialType,
		GenderType genderType,
		Integer age,
		UserType userType
	) {
		User user = User.builder()
			.email(email)
			.socialType(converter.convertToEntityAttribute(socialType.toString()))
			.role(userType)
			.gender(String.valueOf(genderType))
			.age(age)
			.nickName(generateNickname())
			.build();

		return oauthRepository.save(user);
	}

	@Transactional
	public TokenDto refresh(String refreshToken) {
		//refresh Token 검증
		if (!JwtTokenValidator.validateToken(refreshToken)) {
			throw new UserException(INVALID_REFRESH_TOKEN);
		}

		Authentication authentication = authenticationManager.getAuthentication(refreshToken);
		log.info("USER ID is : {}", authentication.getName());

		//refresh Token DB에 존재하는지 검사
		User user = oauthRepository.findByRefreshToken(refreshToken).orElseThrow(
			() -> new UserException(INVALID_REFRESH_TOKEN)
		);

		TokenDto reissuedToken = tokenProvider.generateToken(user.getEmail(),
			user.getRole(), user.getId());

		// DB에 저장된 refresh 토큰을 재발급한 refresh 토큰으로 업데이트
		user.updateRefreshToken(reissuedToken.refreshToken());

		return reissuedToken;
	}

	public String generateNickname() {
		List<String> a = Arrays.asList("부드러운", "향기로운", "숙성된", "풍부한", "깊은", "황금빛", "오크향의", "스모키한", "달콤한", "강렬한");
		List<String> b = Arrays.asList("몰트", "버번", "위스키", "바텐더", "오크통", "싱글몰트", "블렌디드", "아이리시", "스카치", "캐스크");
		List<String> c = Arrays.asList("글렌피딕", "맥캘란", "라가불린", "탈리스커", "조니워커", "제임슨", "야마자키", "부카나스", "불릿", "잭다니엘스");
		String key = a.get(randomValue.nextInt(a.size()));
		if (randomValue.nextInt() % 2 == 0) key += b.get(randomValue.nextInt(b.size()));
		else key += c.get(randomValue.nextInt(c.size()));
		return key + oauthRepository.getNextNicknameSequence();
	}
}
