package app.bottlenote.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class OauthResponse {

  private final String accessToken;
  private final Boolean isFirstLogin;
  private final String nickname;

  public static OauthResponse of(String accessToken) {
    return new OauthResponse(accessToken, null, null);
  }
}
