package app.bottlenote.user.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.agreement.annotation.AgreementExempt;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.controller.docs.AuthApiDocs;
import app.bottlenote.user.dto.request.AgentLoginRequest;
import app.bottlenote.user.dto.request.AppleLoginRequest;
import app.bottlenote.user.dto.request.KakaoLoginRequest;
import app.bottlenote.user.dto.request.TokenVerifyRequest;
import app.bottlenote.user.dto.response.AuthResponse;
import app.bottlenote.user.dto.response.NonceResponse;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.NonceService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
@AuthApiDocs.ApiTag
public class AuthV2Controller {
  private final AuthService authService;
  private final NonceService nonceService;
  private final OauthConfigProperties configProperties;
  private static final String REFRESH_TOKEN_HEADER_PREFIX = "refresh-token";

  @AuthApiDocs.CheckAdminStatus
  @GetMapping("/admin/permissions")
  public ResponseEntity<GlobalResponse> checkAdminStatus() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    boolean is = authService.checkAdminStatus(currentUserId);
    return GlobalResponse.ok(is);
  }

  /** Apple 로그인 전, 클라이언트에게 일회성 Nonce 값을 발급 */
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.GetAppleNonce
  @GetMapping("/apple/nonce")
  public ResponseEntity<NonceResponse> getAppleNonce() {
    return ResponseEntity.ok(new NonceResponse(nonceService.generateNonce()));
  }

  /** Apple 로그인 v2 */
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.ExecuteAppleLogin
  @PostMapping("/apple")
  public ResponseEntity<OauthResponse> executeAppleLogin(
      @RequestBody @Valid AppleLoginRequest appleLoginRequest, HttpServletResponse response) {
    AuthResponse result =
        authService.loginWithApple(appleLoginRequest.idToken(), appleLoginRequest.nonce());
    setRefreshTokenInCookie(response, result.token().refreshToken());
    return ResponseEntity.ok(
        OauthResponse.of(
            result.token().accessToken(),
            result.isFirstLogin(),
            result.nickname(),
            result.agreementRequired()));
  }

  /** 카카오 로그인 v2 */
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.ExecuteKakaoLogin
  @PostMapping("/kakao")
  public ResponseEntity<OauthResponse> executeKakaoLogin(
      @RequestBody @Valid KakaoLoginRequest kakaoLoginRequest, HttpServletResponse response) {
    AuthResponse result = authService.loginWithKakao(kakaoLoginRequest.accessToken());
    setRefreshTokenInCookie(response, result.token().refreshToken());
    return ResponseEntity.ok(
        OauthResponse.of(
            result.token().accessToken(),
            result.isFirstLogin(),
            result.nickname(),
            result.agreementRequired()));
  }

  /** 에이전트 키 기반 로그인 */
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.ExecuteAgentLogin
  @PostMapping("/agent")
  public ResponseEntity<OauthResponse> executeAgentLogin(
      @RequestBody @Valid AgentLoginRequest agentLoginRequest, HttpServletResponse response) {
    AuthResponse result = authService.loginWithAgent(agentLoginRequest.agentKey());
    setRefreshTokenInCookie(response, result.token().refreshToken());
    return ResponseEntity.ok(
        OauthResponse.of(
            result.token().accessToken(),
            result.isFirstLogin(),
            result.nickname(),
            result.agreementRequired()));
  }

  /** 토큰 재발급 */
  @AgreementExempt
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.ReissueToken
  @PostMapping("/reissue")
  public ResponseEntity<GlobalResponse> reissueToken(
      HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER_PREFIX);
    TokenItem token = authService.reissue(refreshToken);
    setRefreshTokenInCookie(response, token.refreshToken());
    return GlobalResponse.ok(OauthResponse.of(token.accessToken()));
  }

  /** 토큰 유효성 검증 */
  @SecurityPolicy(auth = PUBLIC)
  @AuthApiDocs.VerifyToken
  @PutMapping("/token/verify")
  public ResponseEntity<GlobalResponse> verifyToken(
      @RequestBody @Valid TokenVerifyRequest request) {
    return GlobalResponse.ok(authService.verifyToken(request.token()));
  }

  private void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
    final int cookieExpireTime = configProperties.getCookieExpireTime();
    log.info(
        "cookie basic expire time : {} properties time :{}", cookieExpireTime, cookieExpireTime);
    Cookie cookie = new Cookie(REFRESH_TOKEN_HEADER_PREFIX, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(cookieExpireTime);
    response.addCookie(cookie);
  }
}
