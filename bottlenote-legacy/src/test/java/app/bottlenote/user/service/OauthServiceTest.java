package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.shared.token.TokenItem;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fake.FakeAppleTokenValidator;
import app.bottlenote.user.fake.FakeBCryptPasswordEncoder;
import app.bottlenote.user.fake.FakeJwtAuthenticationManager;
import app.bottlenote.user.fake.FakeJwtTokenProvider;
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

    // OauthService 초기화
    oauthService =
        new OauthService(
            oauthRepository,
            jwtTokenProvider,
            jwtAuthenticationManager,
            passwordEncoder,
            tokenValidator);

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
        jwtTokenProvider.createRefreshToken(
            "cdm2883@naver.com",
            app.bottlenote.shared.users.UserType.valueOf(UserType.ROLE_USER.name()),
            1L);

    // 각 테스트 전에 Repository 상태 초기화
    oauthRepository.clear();
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

  // @Test
  // @DisplayName("Apple 로그인 시 Nonce 검증에 실패하면 예외를 발생시킨다.")
  // void loginWithApple_invalidNonce_throwsException() {
  //   // Given
  //   String idToken = "mockIdToken";
  //   String invalidNonce = "invalidNonce";

  //   // When & Then
  //   assertThrows(UserException.class, () -> oauthService.loginWithApple(idToken, invalidNonce));
  // }

  // @Test
  // @DisplayName("Apple 로그인이 성공할 수 있다.")
  // void loginWithApple_success() {
  //   // Given
  //   String idToken = "validIdToken";
  //   String validNonce = nonceService.generateNonce();

  //   // When
  //   TokenItem result = oauthService.loginWithApple(idToken, validNonce);

  //   // Then
  //   assertNotNull(result);
  //   assertThat(result.accessToken()).isNotNull().isNotEmpty();
  //   assertThat(result.refreshToken()).isNotNull().isNotEmpty();

  //   // 사용자가 저장되었는지 확인
  //   assertThat(oauthRepository.findByEmail("apple.user@example.com")).isPresent();
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 신규 회원가입이 성공할 수 있다")
  // void loginWithKakao_신규회원가입_성공() {
  //   // Given
  //   String accessToken = "valid_token_with_email";

  //   // When
  //   TokenItem result = oauthService.loginWithKakao(accessToken);

  //   // Then
  //   assertNotNull(result);
  //   assertThat(result.accessToken()).isNotNull().isNotEmpty();
  //   assertThat(result.refreshToken()).isNotNull().isNotEmpty();

  //   // 카카오 ID로 사용자가 저장되었는지 확인
  //   assertThat(oauthRepository.findBySocialUniqueId("123456789")).isPresent();

  //   // 이메일로도 조회 가능한지 확인
  //   assertThat(oauthRepository.findByEmail("test@kakao.com")).isPresent();

  //   User savedUser = oauthRepository.findBySocialUniqueId("123456789").get();
  //   assertThat(savedUser.getSocialType()).contains(SocialType.KAKAO);
  //   assertThat(savedUser.getGender()).isEqualTo(GenderType.FEMALE);
  //   assertThat(savedUser.getAge()).isEqualTo(24); // 20~29 -> 24
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 이메일 없는 사용자도 회원가입할 수 있다")
  // void loginWithKakao_이메일없는사용자_회원가입_성공() {
  //   // Given
  //   String accessToken = "valid_token_without_email";

  //   // When
  //   TokenItem result = oauthService.loginWithKakao(accessToken);

  //   // Then
  //   assertNotNull(result);
  //   assertThat(result.accessToken()).isNotNull().isNotEmpty();
  //   assertThat(result.refreshToken()).isNotNull().isNotEmpty();

  //   // 카카오 ID로 사용자가 저장되었는지 확인
  //   User savedUser = oauthRepository.findBySocialUniqueId("987654321").orElseThrow();
  //   assertThat(savedUser.getEmail()).startsWith("kakao").endsWith("@bottlenote.com");
  //   assertThat(savedUser.getSocialType()).contains(SocialType.KAKAO);
  //   assertThat(savedUser.getGender()).isEqualTo(GenderType.MALE);
  //   assertThat(savedUser.getAge()).isEqualTo(34); // 30~39 -> 34
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 기존 회원은 로그인할 수 있다")
  // void loginWithKakao_기존회원_로그인_성공() {
  //   // Given
  //   // 기존 사용자를 DB에 미리 저장
  //   User existingUser =
  //       User.builder()
  //           .email("existing@test.com")
  //           .socialUniqueId("555555555")
  //           .socialType(List.of(SocialType.KAKAO))
  //           .role(UserType.ROLE_USER)
  //           .nickName("기존회원")
  //           .build();
  //   oauthRepository.save(existingUser);

  //   String accessToken = "existing_user_token";

  //   // When
  //   TokenItem result = oauthService.loginWithKakao(accessToken);

  //   // Then
  //   assertNotNull(result);
  //   assertThat(result.accessToken()).isNotNull().isNotEmpty();
  //   assertThat(result.refreshToken()).isNotNull().isNotEmpty();

  //   // 기존 사용자가 조회되는지 확인
  //   User loginUser = oauthRepository.findBySocialUniqueId("555555555").orElseThrow();
  //   assertThat(loginUser.getNickName()).isEqualTo("기존회원");
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 잘못된 토큰으로 요청하면 예외가 발생한다")
  // void loginWithKakao_잘못된토큰_예외발생() {
  //   // Given
  //   String invalidToken = "invalid_token";

  //   // When & Then
  //   assertThrows(UserException.class, () -> oauthService.loginWithKakao(invalidToken));
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 카카오 서버 에러시 예외가 발생한다")
  // void loginWithKakao_서버에러_예외발생() {
  //   // Given
  //   kakaoFeignClient.simulateServerError();
  //   String accessToken = "valid_token_with_email";

  //   // When & Then
  //   assertThrows(UserException.class, () -> oauthService.loginWithKakao(accessToken));
  // }

  // @Test
  // @DisplayName("카카오 로그인 - 이메일로 기존 회원 연동이 가능하다")
  // void loginWithKakao_이메일기반_기존회원연동_성공() {
  //   // Given
  //   // 기존 사용자를 이메일로만 저장 (카카오 연동 안된 상태)
  //   User existingUser =
  //       User.builder()
  //           .email("test@kakao.com")
  //           .socialType(new ArrayList<>(List.of(SocialType.BASIC)))
  //           .role(UserType.ROLE_USER)
  //           .nickName("기존회원")
  //           .build();
  //   oauthRepository.save(existingUser);

  //   String accessToken = "valid_token_with_email";

  //   // When
  //   TokenItem result = oauthService.loginWithKakao(accessToken);

  //   // Then
  //   assertNotNull(result);

  //   // 기존 회원에 카카오 ID가 업데이트되었는지 확인
  //   User updatedUser = oauthRepository.findByEmail("test@kakao.com").orElseThrow();
  //   assertThat(updatedUser.getSocialUniqueId()).isEqualTo("123456789");
  //   assertThat(updatedUser.getNickName()).isEqualTo("기존회원"); // 기존 정보 유지
  // }
}
