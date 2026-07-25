package app.bottlenote.user.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.dto.request.BasicLoginRequest;
import app.bottlenote.user.dto.request.TokenVerifyRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.OauthService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@SecurityPolicy(auth = PUBLIC)
public class OauthController {
  private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";
  private final AuthService authService;
  private final OauthService oauthService;
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

  @PostMapping("/restore")
  public ResponseEntity<?> restoreAccount(@RequestBody @Valid BasicLoginRequest request) {
    oauthService.restoreUser(request.getEmail(), request.getPassword());
    return GlobalResponse.ok("restore success");
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
