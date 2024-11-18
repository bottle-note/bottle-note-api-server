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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 추후에 로그인, 회원가입 관련 기능 추가시 proxy transactional 분리를 고려해야함.(ex. 로그인은 읽기전용, 회원가입은 쓰기전용.. 등)
public class OauthService {
	private final OauthRepository oauthRepository;
	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationManager authenticationManager;
	private final JsonArrayConverter converter;


	public TokenDto oauthLogin(OauthRequest oauthReq) {
		final String email = oauthReq.email();
		final SocialType socialType = oauthReq.socialType();
		final GenderType genderType = oauthReq.gender();
		final Integer age = oauthReq.age();

		User user = oauthRepository.findByEmail(email).orElseGet(() -> oauthSignUp(email, socialType, genderType, age));
		user.addSocialType(oauthReq.socialType());
		TokenDto token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.getRefreshToken());
		return token;
	}

	public User oauthSignUp(
		String email,
		SocialType socialType,
		GenderType genderType,
		Integer age
	) {

		User user = User.builder()
			.email(email)
			.socialType(converter.convertToEntityAttribute(socialType.toString()))
			.role(UserType.ROLE_USER)
			.gender(String.valueOf(genderType))
			.age(age)
			.nickName(generateNickname())
			.build();

		return oauthRepository.save(user);
	}

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
		user.updateRefreshToken(reissuedToken.getRefreshToken());

		return reissuedToken;
	}

	public TokenDto guestLogin() {
		User guest = oauthRepository.loadGuestUser()
			.orElseGet(() -> oauthRepository.save(User.builder()
				.email("guest@bottlenote.com")
				.nickName(generateNickname())
				.role(UserType.ROLE_GUEST)
				.build()));

		return tokenProvider.generateToken(guest.getEmail(), guest.getRole(), guest.getId());
	}

	public String generateNickname() {
		Random random = new Random();
		List<String> a = Arrays.asList("부드러운", "향기로운", "숙성된", "풍부한", "깊은", "황금빛", "오크향의", "스모키한", "달콤한", "강렬한");
		List<String> b = Arrays.asList("몰트", "버번", "위스키", "바텐더", "오크통", "싱글몰트", "블렌디드", "아이리시", "스카치", "캐스크");
		List<String> c = Arrays.asList("글렌피딕", "맥캘란", "라가불린", "탈리스커", "조니워커", "제임슨", "야마자키", "부카나스", "불릿", "잭다니엘스");
		String key = a.get(random.nextInt(a.size()));
		if (random.nextInt() % 2 == 0) key += b.get(random.nextInt(b.size()));
		else key += c.get(random.nextInt(c.size()));
		return key + oauthRepository.getNextNicknameSequence();
	}
}
