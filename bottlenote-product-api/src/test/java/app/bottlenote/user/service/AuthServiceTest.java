package app.bottlenote.user.service;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static app.bottlenote.agent.constant.AgentStatus.INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import app.bottlenote.agent.support.AgentKeyHasher;
import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.fixture.InMemoryUserAgreementRepository;
import app.bottlenote.agreement.service.AgreementEvaluator;
import app.bottlenote.agreement.service.DefaultAgreementFacade;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.AuthResponse;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fake.FakeJwtTokenProvider;
import app.bottlenote.user.fake.FakeOauthRepository;
import app.bottlenote.user.repository.RootAdminRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Field;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] AuthService")
class AuthServiceTest {

  private static String apiKey(char value) {
    return "bn_agent_" + String.valueOf(value).repeat(43);
  }

  private AuthService authService;
  private FakeOauthRepository oauthRepository;
  private FakeJwtTokenProvider jwtTokenProvider;
  private RootAdminRepository rootAdminRepository;
  private AppleAuthService appleAuthService;
  private KakaoAuthService kakaoAuthService;
  private InMemoryAgentRepository agentRepository;
  private AgentFacade agentFacade;
  private InMemoryUserAgreementRepository agreementRepository;

  @BeforeEach
  void setUp() throws Exception {
    initializeJwtTokenValidator();

    oauthRepository = new FakeOauthRepository();
    jwtTokenProvider = new FakeJwtTokenProvider();
    rootAdminRepository = mock(RootAdminRepository.class);
    appleAuthService = mock(AppleAuthService.class);
    kakaoAuthService = mock(KakaoAuthService.class);
    agentRepository = new InMemoryAgentRepository();
    agentFacade = new DefaultAgentFacade(agentRepository);
    agreementRepository = new InMemoryUserAgreementRepository();
    AgreementEvaluator agreementEvaluator = new AgreementEvaluator(agreementRepository);
    DefaultAgreementFacade agreementFacade = new DefaultAgreementFacade(agreementEvaluator);

    authService =
        new AuthService(
            rootAdminRepository,
            oauthRepository,
            jwtTokenProvider,
            appleAuthService,
            kakaoAuthService,
            agreementFacade,
            agentFacade);

    oauthRepository.clear();
    agentRepository.clear();
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
    assertThat(result.agreementRequired()).isTrue();
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
    assertThat(result.agreementRequired()).isTrue();
  }

  @Test
  @DisplayName("동의 충족 사용자가 카카오로 로그인하면 동의 필요 힌트를 false로 반환한다")
  void loginWithKakao_whenEligible_returnsAgreementRequiredFalse() {
    // given
    saveRequiredAgreements(1L);
    KakaoUserResponse.KakaoAccount kakaoAccount =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "eligible@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");
    KakaoUserResponse kakaoUser =
        new KakaoUserResponse(111111111L, LocalDateTime.now(), kakaoAccount);
    when(kakaoAuthService.getUserInfo(anyString())).thenReturn(kakaoUser);

    // when
    AuthResponse result = authService.loginWithKakao("valid-kakao-token");

    // then
    assertThat(result.agreementRequired()).isFalse();
    assertThat(result.token().accessToken()).isNotBlank();
    assertThat(result.token().refreshToken()).isNotBlank();
    assertThat(result.isFirstLogin()).isTrue();
    assertThat(result.nickname()).isNotBlank();
    assertThat(oauthRepository.findByEmail("eligible@kakao.com").orElseThrow().getRefreshToken())
        .isEqualTo(result.token().refreshToken());
  }

  @Test
  @DisplayName("동의 미충족 사용자가 애플로 로그인하면 동의 필요 힌트를 true로 반환한다")
  void loginWithApple_whenNotEligible_returnsAgreementRequiredTrue() {
    // given
    saveAgreement(1L, AgreementType.TERMS_OF_SERVICE);
    AppleAuthService.AppleUserInfo appleUserInfo =
        new AppleAuthService.AppleUserInfo("apple-ineligible", "ineligible@apple.com");
    when(appleAuthService.validateAndGetUserInfo(anyString(), anyString()))
        .thenReturn(appleUserInfo);

    // when
    AuthResponse result = authService.loginWithApple("valid-id-token", "valid-nonce");

    // then
    assertThat(result.agreementRequired()).isTrue();
    assertThat(result.token().accessToken()).isNotBlank();
    assertThat(result.token().refreshToken()).isNotBlank();
    assertThat(result.isFirstLogin()).isTrue();
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

  @Test
  @DisplayName("탈퇴한 회원이 재로그인 하는 경우 예외가 발생한다")
  void deleted_user_cannot_login() {
    // given
    KakaoUserResponse.KakaoAccount kakaoAccount =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "deleted@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");
    KakaoUserResponse kakaoUser =
        new KakaoUserResponse(987654321L, LocalDateTime.now(), kakaoAccount);
    when(kakaoAuthService.getUserInfo(anyString())).thenReturn(kakaoUser);

    authService.loginWithKakao("valid-kakao-token");
    User user = oauthRepository.findByEmail("deleted@kakao.com").orElseThrow();
    user.withdrawUser();

    // when & then
    assertThrows(UserException.class, () -> authService.loginWithKakao("valid-kakao-token"));
  }

  @Test
  @DisplayName("토큰 재발급을 할 수 있다.")
  void reissue_token() {
    // given
    String reissueRefreshToken =
        jwtTokenProvider.createRefreshToken("cdm2883@naver.com", UserType.ROLE_USER, 1L);

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
    TokenItem response = authService.reissue(reissueRefreshToken);

    // then
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isNotNull().isNotEmpty();
    assertThat(response.refreshToken()).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("토큰 검증에 통과하지 못하면 토큰 재발급에 실패한다")
  void reissue_token_fail() {
    // given
    String invalidRefreshToken = "invalid-refresh-token";

    // when & then
    assertThrows(UserException.class, () -> authService.reissue(invalidRefreshToken));
  }

  @Test
  @DisplayName("활성 에이전트 키로 매핑된 계정으로 로그인할 수 있다")
  void loginWithAgent_success() {
    // given
    String rawKey = apiKey('A');
    User agentUser =
        User.builder()
            .email("agent0001@bottlenote.com")
            .nickName("에이전트0001")
            .socialType(new ArrayList<>())
            .role(UserType.ROLE_USER)
            .build();
    oauthRepository.save(agentUser);
    agentRepository.save(
        Agent.builder()
            .id("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            .profileCode("0001")
            .name("BottleNote Agent A")
            .status(ACTIVE)
            .productUserId(agentUser.getId())
            .adminUserId(1L)
            .apiKeyHash(AgentKeyHasher.validateAndHash(rawKey))
            .build());

    // when
    AuthResponse result = authService.loginWithAgent(rawKey);

    // then
    assertThat(result.token().accessToken()).isNotNull();
    assertThat(result.token().refreshToken()).isNotNull();
    assertThat(result.nickname()).isEqualTo("에이전트0001");
  }

  @Test
  @DisplayName("에이전트 키가 API Key 형식이 아니면 400에 해당하는 예외가 발생한다")
  void loginWithAgent_malformedKey_throwsInvalidFormat() {
    // when
    UserException exception =
        assertThrows(UserException.class, () -> authService.loginWithAgent("invalid-key"));

    // then
    assertThat(exception.getExceptionCode()).isEqualTo(UserExceptionCode.AGENT_KEY_INVALID_FORMAT);
  }

  @Test
  @DisplayName("등록되지 않은 에이전트 키는 401에 해당하는 예외가 발생한다")
  void loginWithAgent_unknownKey_throwsAuthenticationFailed() {
    // given
    String unknownKey = apiKey('B');

    // when
    UserException exception =
        assertThrows(UserException.class, () -> authService.loginWithAgent(unknownKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("비활성 에이전트 키는 401에 해당하는 예외가 발생한다")
  void loginWithAgent_inactiveAgent_throwsAuthenticationFailed() {
    // given
    String rawKey = apiKey('C');
    agentRepository.save(
        Agent.builder()
            .id("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
            .profileCode("0002")
            .name("BottleNote Agent B")
            .status(INACTIVE)
            .productUserId(1L)
            .adminUserId(1L)
            .apiKeyHash(AgentKeyHasher.validateAndHash(rawKey))
            .build());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> authService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("활성 에이전트라도 매핑된 계정이 없으면 401에 해당하는 예외가 발생한다")
  void loginWithAgent_missingAccountMapping_throwsAuthenticationFailed() {
    // given
    String rawKey = apiKey('D');
    agentRepository.save(
        Agent.builder()
            .id("cccccccc-cccc-cccc-cccc-cccccccccccc")
            .profileCode("0003")
            .name("BottleNote Agent C")
            .status(ACTIVE)
            .productUserId(Long.MAX_VALUE)
            .adminUserId(1L)
            .apiKeyHash(AgentKeyHasher.validateAndHash(rawKey))
            .build());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> authService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("에이전트에 매핑된 계정이 탈퇴 상태면 401에 해당하는 예외가 발생한다")
  void loginWithAgent_withdrawnUser_throwsAuthenticationFailed() {
    // given
    String rawKey = apiKey('E');
    User withdrawnUser =
        User.builder()
            .email("agent0004@bottlenote.com")
            .nickName("에이전트0004")
            .socialType(new ArrayList<>())
            .role(UserType.ROLE_USER)
            .build();
    withdrawnUser.withdrawUser();
    oauthRepository.save(withdrawnUser);
    agentRepository.save(
        Agent.builder()
            .id("dddddddd-dddd-dddd-dddd-dddddddddddd")
            .profileCode("0004")
            .name("BottleNote Agent D")
            .status(ACTIVE)
            .productUserId(withdrawnUser.getId())
            .adminUserId(1L)
            .apiKeyHash(AgentKeyHasher.validateAndHash(rawKey))
            .build());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> authService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
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

  private void saveRequiredAgreements(Long userId) {
    saveAgreement(userId, AgreementType.TERMS_OF_SERVICE);
    saveAgreement(userId, AgreementType.PRIVACY_COLLECTION_USE);
  }

  private void saveAgreement(Long userId, AgreementType type) {
    agreementRepository.save(
        UserAgreement.create(
            userId,
            type,
            AgreementAction.AGREE,
            "document",
            LocalDateTime.of(2026, 8, 1, 0, 0),
            AgreementInputContext.INDIVIDUAL,
            "127.0.0.1",
            "test-agent"));
  }
}
