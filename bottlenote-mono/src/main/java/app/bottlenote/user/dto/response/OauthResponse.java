package app.bottlenote.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public final class OauthResponse implements AuthLoginResponse {
  private String accessToken;
  private Boolean isFirstLogin;
  private String nickname;

  public static OauthResponse of(String accessToken) {
    return new OauthResponse(accessToken, null, null);
  }

  public static OauthResponse from(AuthResponse response) {
    return new OauthResponse(
        response.token().accessToken(), response.isFirstLogin(), response.nickname());
  }
}
