package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.BasicAccountResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static app.bottlenote.global.security.jwt.JwtTokenValidator.validateToken;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;


@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {
	private final OauthRepository oauthRepository;
	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationManager authenticationManager;
	private final BCryptPasswordEncoder passwordEncoder;
	private final SecureRandom randomValue = new SecureRandom();

	@Transactional
	public TokenItem login(OauthRequest oauthReq) {
		final String email = oauthReq.email();
		final String socialUniqueId = oauthReq.socialUniqueId();
		final SocialType socialType = oauthReq.socialType();

		log.info("소셜 로그인 시도: email={}, socialType={}", email, socialType);

		if (Objects.requireNonNull(socialType) == SocialType.APPLE) {
			return doAppleLogin(oauthReq, socialUniqueId, email, socialType);
		}
		return doLogin(oauthReq, email, socialType);
	}

	private TokenItem doLogin(OauthRequest oauthReq, String email, SocialType socialType) {
		User user;
		boolean isNewUser = false;

		// 사용자 조회 전에 로그 추가
		log.info("일반 소셜 로그인 진행: email={}, socialType={}", email, socialType);

		// 기존 사용자 조회 시도
		var existingUser = oauthRepository.findByEmail(email);
		if (existingUser.isEmpty()) {
			log.info("사용자 없음, 회원가입 진행: email={}, socialType={}", email, socialType);
			isNewUser = true;
			user = oauthSignUp(oauthReq, UserType.ROLE_USER);
		} else {
			user = existingUser.get();
		}

		checkActiveUser(user);
		TokenItem tokenItem = getTokenItem(user, socialType);

		if (isNewUser) {
			log.info("회원가입 및 로그인 완료: email={}, socialType={}, userId={}", email, socialType, user.getId());
		} else {
			log.info("기존 사용자 로그인 완료: email={}, socialType={}, userId={}", email, socialType, user.getId());
		}

		return tokenItem;
	}

	private TokenItem doAppleLogin(OauthRequest oauthReq, String socialUniqueId, String email, SocialType socialType) {
		User user;
		boolean isNewUser = false;

		log.info("애플 로그인 진행: email={}, socialUniqueId={}", email, socialUniqueId);

		//최초 로그인 (email과 socialUniqueId 모두 존재하는 경우)
		if (socialUniqueId != null && !socialUniqueId.isBlank() && email != null && !email.isBlank()) {
			log.info("애플 신규 회원가입 시도: email={}, socialUniqueId={}", email, socialUniqueId);
			isNewUser = true;
			user = oauthSignUp(oauthReq, UserType.ROLE_USER);
		} else {
			if (socialUniqueId == null) {
				log.error("애플 로그인 오류: socialUniqueId is null, email={}", email);
				throw new UserException(UserExceptionCode.TEMPORARY_LOGIN_ERROR);
			}
			log.info("기존 애플 사용자 조회: socialUniqueId={}", socialUniqueId);
			user = oauthRepository.findBySocialUniqueId(socialUniqueId).orElseThrow(
					() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		}
		checkActiveUser(user);
		TokenItem tokenItem = getTokenItem(user, socialType);

		if (isNewUser) {
			log.info("애플 회원가입 및 로그인 완료: email={}, userId={}", email, user.getId());
		} else {
			log.info("기존 애플 사용자 로그인 완료: email={}, userId={}", user.getEmail(), user.getId());
		}

		return tokenItem;
	}

	private void checkActiveUser(User user) {
		if (Boolean.FALSE.equals(user.isAlive()))
			throw new UserException(UserExceptionCode.USER_DELETED);
	}

	private TokenItem getTokenItem(User user, SocialType socialType) {
		user.addSocialType(socialType);
		TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.refreshToken());
		return token;
	}

	@Transactional
	public void restoreUser(String email, String password) {
		User user = oauthRepository.findByEmail(email)
				.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		if (Boolean.TRUE.equals(user.isAlive()))
			throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);


		if (user.getSocialType().contains(SocialType.BASIC)) {
			boolean matches = passwordEncoder.matches(password, user.getPassword());
			if (!matches) {
				throw new UserException(UserExceptionCode.INVALID_PASSWORD);
			}
		}

		user.restore();
	}

	@Transactional
	public String guestLogin() {
		final int expireTime = 1000 * 60 * 60 * 24;
		OauthRequest oauthRequest = new OauthRequest(
				"guest" + UUID.randomUUID() + "@bottlenote.com",
				"socialUniqueId" + UUID.randomUUID(),
				SocialType.APPLE,
				GenderType.MALE,
				30);
		User guest = oauthRepository.loadGuestUser()
				.orElseGet(() -> oauthSignUp(oauthRequest, UserType.ROLE_GUEST));
		return tokenProvider.createGuestToken(
				guest.getId(),
				expireTime
		);
	}

	private User oauthSignUp(OauthRequest oauthRequest, UserType userType) {
		// 회원가입 정보 로깅
		log.info("회원가입 시작: email={}, socialType={}, userType={}",
				oauthRequest.email(), oauthRequest.socialType(), userType);

		// 회원가입 요청 정보 상세 로깅
		log.info("회원가입 상세 정보: email={}, socialType={}, socialUniqueId={}, gender={}, age={}",
				oauthRequest.email(),
				oauthRequest.socialType(),
				oauthRequest.socialUniqueId(),
				oauthRequest.gender(),
				oauthRequest.age());

		User user = User.builder()
				.email(oauthRequest.email())
				.socialUniqueId(oauthRequest.socialUniqueId())
				.socialType(List.of(oauthRequest.socialType()))
				.role(userType)
				.gender(oauthRequest.gender())
				.age(oauthRequest.age())
				.nickName(generateNickname())
				.build();

		User savedUser = oauthRepository.save(user);
		log.info("회원가입 완료: email={}, userId={}, nickname={}",
				savedUser.getEmail(), savedUser.getId(), savedUser.getNickName());

		return savedUser;
	}

	@Transactional
	public TokenItem refresh(String refreshToken) {
		//refresh Token 검증
		if (!validateToken(refreshToken)) {
			throw new UserException(INVALID_REFRESH_TOKEN);
		}

		Authentication authentication = authenticationManager.getAuthentication(refreshToken);
		log.info("USER ID is : {}", authentication.getName());

		//refresh Token DB에 존재하는지 검사
		User user = oauthRepository.findByRefreshToken(refreshToken).orElseThrow(
				() -> new UserException(INVALID_REFRESH_TOKEN)
		);

		TokenItem reissuedToken = tokenProvider.generateToken(user.getEmail(),
				user.getRole(), user.getId());

		// DB에 저장된 refresh 토큰을 재발급한 refresh 토큰으로 업데이트
		user.updateRefreshToken(reissuedToken.refreshToken());

		return reissuedToken;
	}

	@Transactional
	public BasicAccountResponse basicSignup(String email, String password, Integer age, GenderType gender) {
		log.info("기본 회원가입 시작: email={}", email);

		oauthRepository.findByEmail(email).ifPresent(user -> {
			throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);
		});

		String encodePassword = passwordEncoder.encode(password);
		User user = oauthRepository.save(User.builder()
				.email(email)
				.password(encodePassword)
				.role(UserType.ROLE_USER)
				.socialType(List.of(SocialType.BASIC))
				.nickName(generateNickname())
				.age(age)
				.gender(gender)
				.build());

		TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.refreshToken());

		log.info("기본 회원가입 완료: email={}, userId={}, nickname={}",
				user.getEmail(), user.getId(), user.getNickName());

		return BasicAccountResponse.builder()
				.message(user.getNickName() + "님 환영합니다!")
				.email(user.getEmail())
				.nickname(user.getNickName())
				.accessToken(token.accessToken())
				.refreshToken(token.refreshToken())
				.build();
	}

	@Transactional
	public TokenItem basicLogin(String email, String password) {
		User user = oauthRepository.findByEmail(email).orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new UserException(UserExceptionCode.INVALID_PASSWORD);
		}

		checkActiveUser(user);

		TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		user.updateRefreshToken(token.refreshToken());
		return token;
	}

	protected String generateNickname() {
		List<String> a = Arrays.asList("부드러운", "향기로운", "숙성된", "풍부한", "깊은", "황금빛", "오크향의", "스모키한", "달콤한", "강렬한");
		List<String> b = Arrays.asList("몰트", "버번", "위스키", "바텐더", "오크통", "싱글몰트", "블렌디드", "아이리시", "스카치", "캐스크");
		List<String> c = Arrays.asList("글렌피딕", "맥캘란", "라가불린", "탈리스커", "조니워커", "제임슨", "야마자키", "부카나스", "불릿", "잭다니엘스");
		String key = a.get(randomValue.nextInt(a.size()));
		if (randomValue.nextInt() % 2 == 0) key += b.get(randomValue.nextInt(b.size()));
		else key += c.get(randomValue.nextInt(c.size()));
		return key + oauthRepository.getNextNicknameSequence();
	}

	@Transactional
	public String verifyToken(String token) {
		try {
			boolean validateToken = validateToken(token);
			return validateToken ? "Token is valid" : "Token is invalid {empty}";
		} catch (Exception e) {
			log.error("Token is invalid : {}", e.getMessage());
			return String.format("Token is invalid {%s}", e.getMessage());
		}
	}
}
