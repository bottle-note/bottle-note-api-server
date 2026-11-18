package app.bottlenote.operation.utils;

import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.repository.OauthRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TestAuthenticationSupport {

  private final OauthRepository oauthRepository;
  private final JwtTokenProvider jwtTokenProvider;

  public TestAuthenticationSupport(
      OauthRepository oauthRepository, JwtTokenProvider jwtTokenProvider) {
    this.oauthRepository = oauthRepository;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  public User getFirstUser() {
    return oauthRepository.getFirstUser().orElseGet(this::createRandomUser);
  }

  /** 기본 유저의 Access Token 반환 (첫 번째 유저 또는 새로 생성) */
  public String getAccessToken() {
    User user = oauthRepository.getFirstUser().orElseGet(this::createRandomUser);
    TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    return token.accessToken();
  }

  /** 랜덤 유저의 Access Token 반환 (항상 새로 생성) */
  public String getRandomAccessToken() {
    User user = createRandomUser();
    TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    return token.accessToken();
  }

  /** User 객체로 TokenItem 생성 (리프레시 토큰까지 저장) */
  public TokenItem createToken(User user) {
    TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(token.refreshToken());
    oauthRepository.save(user);
    return token;
  }

  /** 기본 유저의 ID 반환 */
  public Long getDefaultUserId() {
    User user =
        oauthRepository
            .getFirstUser()
            .orElseThrow(() -> new RuntimeException("init 처리된 유저가 없습니다."));
    return user.getId();
  }

  /** 이메일로 유저 ID 조회 */
  public Long getUserId(String email) {
    User user =
        oauthRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("해당 이메일의 유저가 없습니다: " + email));
    return user.getId();
  }

  /** 랜덤 테스트 유저 생성 */
  private User createRandomUser() {
    UUID key = UUID.randomUUID();
    return oauthRepository.save(
        User.builder()
            .email(key + "@example.com")
            .age(20)
            .gender(GenderType.MALE)
            .nickName("testUser" + key)
            .socialType(List.of(SocialType.KAKAO))
            .role(UserType.ROLE_USER)
            .build());
  }
}
