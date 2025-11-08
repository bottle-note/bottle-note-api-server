package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.AuthResponse;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.fake.FakeJwtTokenProvider;
import app.bottlenote.user.fake.FakeOauthRepository;
import app.bottlenote.user.repository.RootAdminRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Field;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] AuthService")
class AuthServiceTest {

  private AuthService authService;
  private FakeOauthRepository oauthRepository;
  private FakeJwtTokenProvider jwtTokenProvider;
  private RootAdminRepository rootAdminRepository;
  private AppleAuthService appleAuthService;
  private KakaoAuthService kakaoAuthService;

  @BeforeEach
  void setUp() throws Exception {
    initializeJwtTokenValidator();

    oauthRepository = new FakeOauthRepository();
    jwtTokenProvider = new FakeJwtTokenProvider();
    rootAdminRepository = mock(RootAdminRepository.class);
    appleAuthService = mock(AppleAuthService.class);
    kakaoAuthService = mock(KakaoAuthService.class);

    authService =
        new AuthService(
            rootAdminRepository,
            oauthRepository,
            jwtTokenProvider,
            appleAuthService,
            kakaoAuthService);

    oauthRepository.clear();
  }

  private void initializeJwtTokenValidator() throws Exception {
    String secret =
        "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    Key secretKey = Keys.hmacShaKeyFor(keyBytes);

    Field secretKeyField = JwtTokenValidator.class.getDeclaredField("secretKey");
    secretKeyField.setAccessible(true);
    secretKeyField.set(null, secretKey);
  }

  @Test
  @DisplayName("카카오 최초 로그인 시 isFirstLogin이 true를 반환한다")
  void test_Kakao_FirstLogin_ReturnsIsFirstLoginTrue() {
    // given
    KakaoUserResponse.KakaoAccount kakaoAccount =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "test@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");

    KakaoUserResponse kakaoUser =
        new KakaoUserResponse(123456789L, LocalDateTime.now(), kakaoAccount);

    when(kakaoAuthService.getUserInfo(anyString())).thenReturn(kakaoUser);

    // when
    AuthResponse result = authService.loginWithKakao("valid-kakao-token");

    // then
    assertThat(result.isFirstLogin()).isTrue();
    assertThat(result.token().accessToken()).isNotNull();
    assertThat(result.token().refreshToken()).isNotNull();
    assertThat(result.nickname()).isNotNull();
  }

  @Test
  @DisplayName("카카오 재로그인 시 isFirstLogin이 false를 반환한다")
  void test_Kakao_ReLogin_ReturnsIsFirstLoginFalse() {
    // given
    KakaoUserResponse.KakaoAccount kakaoAccount =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "test@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");

    KakaoUserResponse kakaoUser =
        new KakaoUserResponse(123456789L, LocalDateTime.now(), kakaoAccount);

    when(kakaoAuthService.getUserInfo(anyString())).thenReturn(kakaoUser);

    authService.loginWithKakao("valid-kakao-token");

    // when
    AuthResponse result = authService.loginWithKakao("valid-kakao-token");

    // then
    assertThat(result.isFirstLogin()).isFalse();
    assertThat(result.token().accessToken()).isNotNull();
    assertThat(result.token().refreshToken()).isNotNull();
  }

  @Test
  @DisplayName("애플 최초 로그인 시 isFirstLogin이 true를 반환한다")
  void test_Apple_FirstLogin_ReturnsIsFirstLoginTrue() {
    // given
    AppleAuthService.AppleUserInfo appleUserInfo =
        new AppleAuthService.AppleUserInfo("apple-user-123", "apple@test.com");

    when(appleAuthService.validateAndGetUserInfo(anyString(), anyString()))
        .thenReturn(appleUserInfo);

    // when
    AuthResponse result = authService.loginWithApple("valid-id-token", "valid-nonce");

    // then
    assertThat(result.isFirstLogin()).isTrue();
    assertThat(result.token().accessToken()).isNotNull();
    assertThat(result.token().refreshToken()).isNotNull();
    assertThat(result.nickname()).isNotNull();
  }

  @Test
  @DisplayName("애플 재로그인 시 isFirstLogin이 false를 반환한다")
  void test_Apple_ReLogin_ReturnsIsFirstLoginFalse() {
    // given
    AppleAuthService.AppleUserInfo appleUserInfo =
        new AppleAuthService.AppleUserInfo("apple-user-123", "apple@test.com");

    when(appleAuthService.validateAndGetUserInfo(anyString(), anyString()))
        .thenReturn(appleUserInfo);

    authService.loginWithApple("valid-id-token", "valid-nonce");

    // when
    AuthResponse result = authService.loginWithApple("valid-id-token", "valid-nonce");

    // then
    assertThat(result.isFirstLogin()).isFalse();
    assertThat(result.token().accessToken()).isNotNull();
    assertThat(result.token().refreshToken()).isNotNull();
  }

  @Test
  @DisplayName("기존 사용자가 카카오로 로그인하면 isFirstLogin이 false를 반환한다")
  void test_ExistingUser_KakaoLogin_ReturnsIsFirstLoginFalse() {
    // given
    User existingUser =
        User.builder()
            .email("test@kakao.com")
            .socialUniqueId("123456789")
            .socialType(List.of(SocialType.KAKAO))
            .role(UserType.ROLE_USER)
            .nickName("기존유저")
            .gender(GenderType.FEMALE)
            .age(24)
            .build();

    Field lastLoginAtField = getLastLoginAtField();
    setLastLoginAt(existingUser, lastLoginAtField, LocalDateTime.now().minusDays(1));

    oauthRepository.save(existingUser);

    KakaoUserResponse.KakaoAccount kakaoAccount =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "test@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");

    KakaoUserResponse kakaoUser =
        new KakaoUserResponse(123456789L, LocalDateTime.now(), kakaoAccount);

    when(kakaoAuthService.getUserInfo(anyString())).thenReturn(kakaoUser);

    // when
    AuthResponse result = authService.loginWithKakao("valid-kakao-token");

    // then
    assertThat(result.isFirstLogin()).isFalse();
    assertThat(result.nickname()).isEqualTo("기존유저");
  }

  private Field getLastLoginAtField() {
    try {
      Field field = User.class.getDeclaredField("lastLoginAt");
      field.setAccessible(true);
      return field;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setLastLoginAt(User user, Field field, LocalDateTime dateTime) {
    try {
      field.set(user, dateTime);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
