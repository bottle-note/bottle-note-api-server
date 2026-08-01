package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.AuthStatus;

public record SignupPendingResponse(AuthStatus status, String signupToken)
    implements AuthLoginResponse {
  public static SignupPendingResponse from(AuthResponse response) {
    return new SignupPendingResponse(response.status(), response.signupToken());
  }
}
