package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenDto;
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


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OauthController {

	private final OauthService oauthService;
	private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";
	private static final int COOKIE_EXPIRE_TIME = 14 * 24 * 60 * 60;

	@PostMapping("/login")
	public ResponseEntity<GlobalResponse> oauthLogin(@RequestBody @Valid OauthRequest oauthReq,
		HttpServletResponse response) {

		TokenDto token = oauthService.oauthLogin(oauthReq);

		setRefreshTokenInCookie(response, token.getRefreshToken());

		return ResponseEntity.ok(
			GlobalResponse.success(OauthResponse.of(token.getAccessToken())));
	}

	@PostMapping("/reissue")
	public ResponseEntity<GlobalResponse> oauthReissue(
		HttpServletRequest request,
		HttpServletResponse response) {

		String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
		log.info("refresh token in request header : {}", refreshToken);

		TokenDto token = oauthService.refresh(refreshToken);

		setRefreshTokenInCookie(response, token.getRefreshToken());

		return ResponseEntity.ok(
			GlobalResponse.success(OauthResponse.of(token.getAccessToken()))
		);
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
