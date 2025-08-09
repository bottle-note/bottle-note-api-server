package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fake.FakeAppleTokenValidator;
import app.bottlenote.user.fake.FakeBCryptPasswordEncoder;
import app.bottlenote.user.fake.FakeJwtAuthenticationManager;
import app.bottlenote.user.fake.FakeJwtTokenProvider;
import app.bottlenote.user.fake.FakeNonceService;
import app.bottlenote.user.fake.FakeOauthRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Field;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] OauthService")
class OauthServiceTest {

  private OauthService oauthService;
  private FakeJwtAuthenticationManager jwtAuthenticationManager;
  private FakeOauthRepository oauthRepository;
  private FakeJwtTokenProvider jwtTokenProvider;
  private FakeBCryptPasswordEncoder passwordEncoder;
  private FakeAppleTokenValidator tokenValidator;
  private FakeNonceService nonceService;

  private String reissueRefreshToken;
  private OauthRequest request;
  private User user;

  @BeforeEach
  void setUp() throws Exception {
    // JwtTokenValidator의 static secretKey 필드 초기화
    initializeJwtTokenValidator();

    // FakeStub 객체들 초기화
    oauthRepository = new FakeOauthRepository();
    jwtTokenProvider = new FakeJwtTokenProvider();
    jwtAuthenticationManager = new FakeJwtAuthenticationManager();
    passwordEncoder = new FakeBCryptPasswordEncoder();
    tokenValidator = new FakeAppleTokenValidator();
    nonceService = new FakeNonceService();

    // OauthService 초기화
    oauthService =
        new OauthService(
            oauthRepository,
            jwtTokenProvider,
            jwtAuthenticationManager,
            passwordEncoder,
            tokenValidator,
            nonceService);

    request = new OauthRequest("cdm2883@naver.com", null, SocialType.KAKAO, null, 26);

    String nickName = "mockNickname";

    user =
        User.builder()
            .id(1L)
            .email("cdm2883@naver.com")
            .gender(GenderType.MALE)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .age(26)
            .nickName(nickName)
            .role(UserType.ROLE_USER)
            .build();

    // 실제 JWT 토큰 생성 (검증 가능한 토큰)
    reissueRefreshToken =
        jwtTokenProvider.createRefreshToken("cdm2883@naver.com", UserType.ROLE_USER, 1L);
  }

  private void initializeJwtTokenValidator() throws Exception {
    String secret =
        "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    Key secretKey = Keys.hmacShaKeyFor(keyBytes);

    // reflection을 사용해서 JwtTokenValidator의 static secretKey 필드 설정
    Field secretKeyField = JwtTokenValidator.class.getDeclaredField("secretKey");
    secretKeyField.setAccessible(true);
    secretKeyField.set(null, secretKey);
  }

  @Test
  @DisplayName("로그인을 할 수 있다.")
  void signin_test() {
    // given
    oauthRepository.save(user); // 사용자 미리 저장

    // when
    TokenItem result = oauthService.login(request);

    // then
    assertThat(result.accessToken()).isNotNull();
    assertThat(result.refreshToken()).isNotNull();
    assertThat(result.accessToken()).isNotEmpty();
    assertThat(result.refreshToken()).isNotEmpty();
  }

  @Test
  @DisplayName("로그인 요청 Email이 DB에 존재하지 않으면 회원가입 로직이 실행된다")
  void test_Login_Or_CreateAccount_Based_On_EmailExistence() {
    // given - 사용자가 DB에 존재하지 않는 상태

    // when
    TokenItem result = oauthService.login(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isNotNull().isNotEmpty();
    assertThat(result.refreshToken()).isNotNull().isNotEmpty();
    // 사용자가 저장되었는지 확인
    assertThat(oauthRepository.findByEmail("cdm2883@naver.com")).isPresent();
  }

  @Test
  @DisplayName("로그인 요청 Email이 DB에 존재하면, 회원가입 로직이 실행되지 않는다")
  void test_Login_Or_CreateAccount_Based_On_Email_Not_Existence() {
    // given
    long initialUserCount = oauthRepository.count();
    oauthRepository.save(user); // 사용자 미리 저장

    // when
    TokenItem result = oauthService.login(request);

    // then
    // 새로운 사용자가 생성되지 않았는지 확인
    assertThat(oauthRepository.count()).isEqualTo(initialUserCount + 1);
    assertThat(result.accessToken()).isNotNull().isNotEmpty();
    assertThat(result.refreshToken()).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("토큰 재발급을 할 수 있다.")
  void reissue_token() {
    // given
    User userWithRefreshToken =
        User.builder()
            .id(1L)
            .email("cdm2883@naver.com")
            .gender(GenderType.MALE)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .age(26)
            .nickName("mockNickname")
            .role(UserType.ROLE_USER)
            .refreshToken(reissueRefreshToken)
            .build();
    oauthRepository.save(userWithRefreshToken);

    // when
    TokenItem response = oauthService.refresh(reissueRefreshToken);

    // then
    assertNotNull(response);
    assertThat(response.accessToken()).isNotNull().isNotEmpty();
    assertThat(response.refreshToken()).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("토큰 검증에 통과하지 못하면 토큰 재발급에 실패한다")
  void reissue_token_fail() {
    // given
    String invalidRefreshToken = "invalid-refresh-token";

    // when & then
    assertThrows(UserException.class, () -> oauthService.refresh(invalidRefreshToken));
  }

  @Test
  @DisplayName("Apple 로그인 시 Nonce 검증에 실패하면 예외를 발생시킨다.")
  void loginWithApple_invalidNonce_throwsException() {
    // Given
    String idToken = "mockIdToken";
    String invalidNonce = "invalidNonce";

    // When & Then
    assertThrows(UserException.class, () -> oauthService.loginWithApple(idToken, invalidNonce));
  }

  @Test
  @DisplayName("Apple 로그인이 성공할 수 있다.")
  void loginWithApple_success() {
    // Given
    String idToken = "validIdToken";
    String validNonce = nonceService.generateNonce();

    // When
    TokenItem result = oauthService.loginWithApple(idToken, validNonce);

    // Then
    assertNotNull(result);
    assertThat(result.accessToken()).isNotNull().isNotEmpty();
    assertThat(result.refreshToken()).isNotNull().isNotEmpty();

    // 사용자가 저장되었는지 확인
    assertThat(oauthRepository.findByEmail("apple.user@example.com")).isPresent();
  }
}
