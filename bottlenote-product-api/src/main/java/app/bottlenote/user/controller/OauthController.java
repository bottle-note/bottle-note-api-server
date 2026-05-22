package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.dto.request.BasicLoginRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.TokenVerifyRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
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
public class OauthController {
  private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";
  private final OauthService oauthService;
  private final OauthConfigProperties configProperties;

  @PostMapping("/login")
  public ResponseEntity<?> executeOauthLogin(
      @RequestBody @Valid OauthRequest oauthReq, HttpServletResponse response) {
    TokenItem token = oauthService.login(oauthReq);
    setRefreshTokenInCookie(response, token.refreshToken());
    return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
  }

  @PostMapping("/reissue")
  public ResponseEntity<?> reissueOauthToken(
      HttpServletRequest request, HttpServletResponse response) {

    String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
    log.info("refresh token in request header : {}", refreshToken);

    TokenItem token = oauthService.refresh(refreshToken);

    setRefreshTokenInCookie(response, token.refreshToken());

    return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
  }

  @PutMapping("/token/verify")
  public ResponseEntity<?> verifyToken(@RequestBody @Valid TokenVerifyRequest token) {
    final String message = oauthService.verifyToken(token.token());
    return GlobalResponse.ok(message);
  }

  @PostMapping("/restore")
  public ResponseEntity<?> restoreAccount(@RequestBody @Valid BasicLoginRequest request) {
    oauthService.restoreUser(request.getEmail(), request.getPassword());
    return GlobalResponse.ok("restore success");
  }

  private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
    final int COOKIE_EXPIRE_TIME = 14 * 24 * 60 * 60;
    final int cookieExpireTime = configProperties.getCookieExpireTime();
    log.debug(
        "cookie basic expire time : {} properties time :{}", COOKIE_EXPIRE_TIME, cookieExpireTime);
    Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(COOKIE_EXPIRE_TIME);
    response.addCookie(cookie);
  }
}
