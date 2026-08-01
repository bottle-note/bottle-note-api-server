package app.bottlenote.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] SignupTokenProvider")
class SignupTokenProviderTest {

  private static final String SECRET =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";

  private final SignupTokenProvider signupTokenProvider = new SignupTokenProvider(SECRET);

  @Test
  @DisplayName("가입 토큰에서 사용자·소셜 공급자·일회성 식별자를 검증한다")
  void parse_signup_token_claims() {
    // given
    UUID tokenId = UUID.randomUUID();
    String token = signupTokenProvider.createToken(1L, SocialType.KAKAO, tokenId);

    // when
    SignupTokenProvider.SignupTokenClaims claims = signupTokenProvider.parse(token);

    // then
    assertThat(claims.userId()).isEqualTo(1L);
    assertThat(claims.socialType()).isEqualTo(SocialType.KAKAO);
    assertThat(claims.tokenId()).isEqualTo(tokenId);
    assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
  }

  @Test
  @DisplayName("일반 로그인 JWT는 가입 토큰으로 사용할 수 없다")
  void reject_login_token() {
    // given
    String accessToken =
        new JwtTokenProvider(SECRET).createAccessToken("user@test.com", UserType.ROLE_USER, 1L);

    // when & then
    assertThatThrownBy(() -> signupTokenProvider.parse(accessToken))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
