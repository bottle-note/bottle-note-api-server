package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.GuestCodeRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenDto;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.OauthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
	private static final int COOKIE_EXPIRE_TIME = 14 * 24 * 60 * 60;
	private final OauthService oauthService;

	@PostMapping("/login")
	public ResponseEntity<?> oauthLogin(
		@RequestBody @Valid OauthRequest oauthReq,
		HttpServletResponse response
	) {
		TokenDto token = oauthService.login(oauthReq);
		setRefreshTokenInCookie(response, token.refreshToken());
		return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
	}

	@PostMapping("/guest-login")
	public ResponseEntity<?> guestLogin(
		@RequestBody @Valid GuestCodeRequest guestCode
	) {
		final String matchCode = "GUEST-CODE-1928112";
		final String key = Base64.getEncoder().encodeToString(matchCode.getBytes());
		final String code = guestCode.code();

		if (!code.equals(key)) {
			//Qk9UVExFTk9URS1HVUVTVC1DT0RFLTIwMjQ=
			throw new UserException(UserExceptionCode.NOT_MATCH_GUEST_CODE);
		}

		final String token = oauthService.guestLogin();
		final OauthResponse oauthResponse = OauthResponse.of(token);
		return GlobalResponse.ok(oauthResponse);
	}

	@PostMapping("/reissue")
	public ResponseEntity<?> oauthReissue(
		HttpServletRequest request,
		HttpServletResponse response) {

		String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
		log.info("refresh token in request header : {}", refreshToken);

		TokenDto token = oauthService.refresh(refreshToken);

		setRefreshTokenInCookie(response, token.refreshToken());

		return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
	}

	private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(COOKIE_EXPIRE_TIME);
		response.addCookie(cookie);
	}
}
