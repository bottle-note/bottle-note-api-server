package app.bottlenote.user.controller;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.dto.request.AppleLoginRequest;
import app.bottlenote.user.dto.response.NonceResponse;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.NonceService;
import app.bottlenote.user.service.OauthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
public class AuthV2Controller {
  private final AuthService authService;
  private final NonceService nonceService;
  private final OauthService oauthService;
  private final OauthConfigProperties configProperties;
  private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";

  @GetMapping("/admin/permissions")
  public ResponseEntity<?> checkAdminStatus() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    boolean is = authService.checkAdminStatus(currentUserId);
    return GlobalResponse.ok(is);
  }

  /** Apple 로그인 전, 클라이언트에게 일회성 Nonce 값을 발급 */
  @GetMapping("/apple/nonce")
  public ResponseEntity<NonceResponse> getAppleNonce() {
    return ResponseEntity.ok(new NonceResponse(nonceService.generateNonce()));
  }

  /** Apple 로그인 v2 */
  @PostMapping("/apple")
  public ResponseEntity<?> executeAppleLogin(
      @RequestBody @Valid AppleLoginRequest appleLoginRequest, HttpServletResponse response) {
    TokenItem token =
        oauthService.loginWithApple(appleLoginRequest.idToken(), appleLoginRequest.nonce());
    setRefreshTokenInCookie(response, token.refreshToken());
    return ResponseEntity.ok(OauthResponse.of(token.accessToken()));
  }

  private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
    final int COOKIE_EXPIRE_TIME = 14 * 24 * 60 * 60;
    final int cookieExpireTime = configProperties.getCookieExpireTime();
    log.info(
        "cookie basic expire time : {} properties time :{}", COOKIE_EXPIRE_TIME, cookieExpireTime);
    Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(COOKIE_EXPIRE_TIME);
    response.addCookie(cookie);
  }
}
