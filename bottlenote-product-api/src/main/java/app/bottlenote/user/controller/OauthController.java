package app.bottlenote.user.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.dto.request.TokenVerifyRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 토큰 재발급·검증 표면. 로그인 자체는 v2(AuthV2Controller)가 담당한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@SecurityPolicy(auth = PUBLIC)
public class OauthController {
  private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";
  private final AuthService authService;
  private final OauthConfigProperties configProperties;

  @PostMapping("/reissue")
  public ResponseEntity<?> reissueOauthToken(
      HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
    TokenItem token = authService.reissue(refreshToken);
    setRefreshTokenInCookie(response, token.refreshToken());
    return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
  }

  @PutMapping("/token/verify")
  public ResponseEntity<?> verifyToken(@RequestBody @Valid TokenVerifyRequest token) {
    return GlobalResponse.ok(authService.verifyToken(token.token()));
  }

  private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
    final int cookieExpireTime = configProperties.getCookieExpireTime();
    Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(cookieExpireTime);
    response.addCookie(cookie);
  }
}
