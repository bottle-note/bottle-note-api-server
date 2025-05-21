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

		if (Objects.requireNonNull(socialType) == SocialType.APPLE) {
			return doAppleLogin(oauthReq, socialUniqueId, email, socialType);
		}

		return doLogin(oauthReq, email, socialType);
	}

	private TokenItem doLogin(OauthRequest oauthReq, String email, SocialType socialType) {
		var existingUser = oauthRepository.findByEmail(email);
		User user = existingUser.orElseGet(() -> oauthSignUp(oauthReq, UserType.ROLE_USER));
		checkActiveUser(user);
		return getTokenItem(user, socialType);
	}

	private TokenItem doAppleLoginOld(OauthRequest oauthReq, String socialUniqueId, String email, SocialType socialType) {
		User user;

		if (socialUniqueId != null && !socialUniqueId.isBlank() && email != null && !email.isBlank()) {
			log.info("애플 신규 회원가입 시도: email={}, socialUniqueId={}", email, socialUniqueId);
			user = oauthSignUp(oauthReq, UserType.ROLE_USER);
		} else {
			if (socialUniqueId == null) {
				log.error("애플 로그인 오류: socialUniqueId is null, email={}", email);
				throw new UserException(UserExceptionCode.TEMPORARY_LOGIN_ERROR);
			}
			user = oauthRepository.findBySocialUniqueId(socialUniqueId).orElseThrow(
					() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		}
		checkActiveUser(user);
		return getTokenItem(user, socialType);
	}

	private TokenItem doAppleLogin(OauthRequest oauthReq, String socialUniqueId, String email, SocialType socialType) {
		User user;

		log.info("애플 로그인 시도: req={}, uniqueId={}, email={}", oauthReq, socialUniqueId, email);
		if (socialUniqueId != null && !socialUniqueId.isBlank()) {
			var existingUser = oauthRepository.findBySocialUniqueId(socialUniqueId);

			if (existingUser.isPresent()) {
				// 사용자를 찾은 경우 해당 사용자 반환
				user = existingUser.get();
			} else {
				// socialUniqueId로 사용자를 찾지 못한 경우
				log.info("애플 로그인: socialUniqueId로 사용자를 찾지 못함, 새 계정 생성 시도, socialUniqueId={}", socialUniqueId);
				if (email != null && !email.isBlank()) {
					var userByEmail = oauthRepository.findByEmail(email);
					if (userByEmail.isPresent()) {
						log.info("애플 로그인: 이메일로 사용자를 찾음, socialUniqueId 업데이트, email={}", email);
						user = userByEmail.get();
						user.updateSocialUniqueId(socialUniqueId);
					} else {
						log.info("애플 로그인: 새 계정 생성, email={}, socialUniqueId={}", email, socialUniqueId);
						user = oauthSignUp(oauthReq, UserType.ROLE_USER);
					}
				} else {
					log.info("애플 로그인: 이메일 없이 후속 로그인, 랜덤 계정 생성, socialUniqueId={}", socialUniqueId);
					OauthRequest newOauthReq = new OauthRequest(
							"apple" + UUID.randomUUID() + "@bottlenote.com",  // 랜덤 이메일 생성
							socialUniqueId,
							SocialType.APPLE,
							oauthReq.gender(),
							oauthReq.age()
					);

					user = oauthSignUp(newOauthReq, UserType.ROLE_USER);
				}
			}
		} else {
			throw new UserException(UserExceptionCode.TEMPORARY_LOGIN_ERROR);
		}

		checkActiveUser(user);
		return getTokenItem(user, socialType);
	}

	private void checkActiveUser(User user) {
		if (!user.isAlive())
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

		if (user.isAlive())
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

		String socialUniqueId = oauthRequest.socialUniqueId();
		SocialType socialType = Objects.requireNonNullElse(oauthRequest.socialType(), SocialType.NONE);
		if (socialType != SocialType.APPLE) {
			socialUniqueId = null;
		}

		User user = User.builder()
				.email(oauthRequest.email())
				.socialUniqueId(socialUniqueId)
				.socialType(List.of(socialType))
				.role(userType)
				.gender(oauthRequest.gender())
				.age(oauthRequest.age())
				.nickName(generateNickname())
				.build();

		User savedUser = oauthRepository.save(user);

		log.info("신규 회원 가입 email {} socialType {}", savedUser.getEmail(), savedUser.getSocialType());
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
