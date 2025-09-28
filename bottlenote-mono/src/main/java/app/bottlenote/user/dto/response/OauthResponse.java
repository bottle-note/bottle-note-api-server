package app.bottlenote.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class OauthResponse {

  private final String accessToken;
}
