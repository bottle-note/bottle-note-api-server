package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.dto.request.AppleLoginRequest;
import app.bottlenote.user.dto.request.BasicAccountRequest;
import app.bottlenote.user.dto.request.BasicLoginRequest;
import app.bottlenote.user.dto.request.GuestCodeRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.BasicAccountResponse;
import app.bottlenote.user.dto.response.NonceResponse;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.NonceService;
import app.bottlenote.user.service.OauthService;
import app.external.push.data.request.SingleTokenRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OauthController {
	private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";
	private final OauthService oauthService;
	private final OauthConfigProperties configProperties;


	@PostMapping("/basic/signup")
	public ResponseEntity<?> executeBasicSignup(
			@RequestBody @Valid BasicAccountRequest request,
			HttpServletResponse response
	) {
		BasicAccountResponse token = oauthService.basicSignup(request.getEmail(), request.getPassword(), request.getAge(), request.getGender());
		setRefreshTokenInCookie(response, token.refreshToken());
		return GlobalResponse.ok(token);
	}

	@PostMapping("/basic/login")
	public ResponseEntity<?> executeBasicLogin(
			@RequestBody @Valid BasicLoginRequest request,
			HttpServletResponse response
	) {
		TokenItem token = oauthService.basicLogin(request.getEmail(), request.getPassword());
		setRefreshTokenInCookie(response, token.refreshToken());
		return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
	}

	@PostMapping("/login")
	public ResponseEntity<?> executeOauthLogin(
			@RequestBody @Valid OauthRequest oauthReq,
			HttpServletResponse response
	) {
		TokenItem token = oauthService.login(oauthReq);
		setRefreshTokenInCookie(response, token.refreshToken());
		return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
	}


	@PostMapping("/guest-login")
	public ResponseEntity<?> executeGuestLogin(
			@RequestBody @Valid GuestCodeRequest guestCode
	) {
		final String key = Base64.getEncoder()
				.encodeToString(configProperties.getGuestCode()
						.getBytes());
		final String code = guestCode.code();

		if (!code.equals(key)) {
			throw new UserException(UserExceptionCode.NOT_MATCH_GUEST_CODE);
		}

		final String token = oauthService.guestLogin();
		final OauthResponse oauthResponse = OauthResponse.of(token);
		return GlobalResponse.ok(oauthResponse);
	}

	@PostMapping("/reissue")
	public ResponseEntity<?> reissueOauthToken(
			HttpServletRequest request,
			HttpServletResponse response) {

		String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
		log.info("refresh token in request header : {}", refreshToken);

		TokenItem token = oauthService.refresh(refreshToken);

		setRefreshTokenInCookie(response, token.refreshToken());

		return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
	}

	@PutMapping("/token/verify")
	public ResponseEntity<?> verifyToken(
			@RequestBody @Valid SingleTokenRequest token
	) {
		final String message = oauthService.verifyToken(token.token());
		return GlobalResponse.ok(message);
	}

	@PostMapping("/restore")
	public ResponseEntity<?> restoreAccount(
			@RequestBody @Valid BasicLoginRequest request
	) {
		oauthService.restoreUser(request.getEmail(), request.getPassword());
		return GlobalResponse.ok("restore success");
	}

	private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
		final int COOKIE_EXPIRE_TIME = 14 * 24 * 60 * 60;
		final int cookieExpireTime = configProperties.getCookieExpireTime();
		log.info("cookie basic expire time : {} properties time :{}", COOKIE_EXPIRE_TIME, cookieExpireTime);
		Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(COOKIE_EXPIRE_TIME);
		response.addCookie(cookie);
	}
}
