package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.AuthStatus;

public record SignupCompleteResponse(AuthStatus status) {
  public static SignupCompleteResponse completed() {
    return new SignupCompleteResponse(AuthStatus.SIGNUP_COMPLETED);
  }
}
