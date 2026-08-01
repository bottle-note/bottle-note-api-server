package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.AuthStatus;

public record AuthResponse(
    TokenItem token, String signupToken, AuthStatus status, Boolean isFirstLogin, String nickname) {

  public static AuthResponse login(TokenItem token, Boolean isFirstLogin, String nickname) {
    return new AuthResponse(token, null, null, isFirstLogin, nickname);
  }

  public static AuthResponse signupPending(String signupToken) {
    return new AuthResponse(null, signupToken, AuthStatus.SIGNUP_PENDING, null, null);
  }
}
