package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.global.security.jwt.SignupTokenProvider;
import app.bottlenote.user.constant.AgreementAction;
import app.bottlenote.user.constant.AgreementType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.SignupToken;
import app.bottlenote.user.domain.SignupTokenRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserAgreement;
import app.bottlenote.user.domain.UserAgreementRepository;
import app.bottlenote.user.dto.request.SignupAgreementRequest;
import app.bottlenote.user.dto.request.SignupRequest;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("unit")
@DisplayName("[unit] [service] SignupService")
class SignupServiceTest {

  private static final String SECRET =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";

  private OauthRepository oauthRepository;
  private SignupTokenRepository signupTokenRepository;
  private UserAgreementRepository userAgreementRepository;
  private SignupTokenProvider signupTokenProvider;
  private SignupService signupService;

  @BeforeEach
  void setUp() {
    oauthRepository = mock(OauthRepository.class);
    signupTokenRepository = mock(SignupTokenRepository.class);
    userAgreementRepository = mock(UserAgreementRepository.class);
    signupTokenProvider = new SignupTokenProvider(SECRET);
    signupService =
        new SignupService(
            oauthRepository, signupTokenRepository, userAgreementRepository, signupTokenProvider);
  }

  @Test
  @DisplayName("가입 대기 사용자에게 가입 완료 전용 토큰을 발급한다")
  void issue_signup_token() {
    // given
    User user = pendingUser();

    // when
    String token = signupService.issueToken(user, SocialType.KAKAO);

    // then
    SignupTokenProvider.SignupTokenClaims claims = signupTokenProvider.parse(token);
    assertThat(claims.userId()).isEqualTo(user.getId());
    assertThat(claims.socialType()).isEqualTo(SocialType.KAKAO);
    verify(signupTokenRepository).saveToken(any(SignupToken.class));
  }

  @Test
  @DisplayName("필수 동의를 저장하고 가입 대기 사용자를 활성화한다")
  void complete_signup() {
    // given
    User user = pendingUser();
    UUID tokenId = UUID.randomUUID();
    String token = signupTokenProvider.createToken(user.getId(), SocialType.KAKAO, tokenId);
    SignupToken signupToken =
        SignupToken.issue(
            tokenId, user.getId(), SocialType.KAKAO, LocalDateTime.now().plusMinutes(10));
    when(oauthRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    when(signupTokenRepository.findByTokenIdForUpdate(tokenId))
        .thenReturn(Optional.of(signupToken));

    SignupRequest request = validRequest(token);

    // when
    signupService.complete(request);

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(signupToken.isConsumed()).isTrue();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UserAgreement>> agreementsCaptor = ArgumentCaptor.forClass(List.class);
    verify(userAgreementRepository).saveAgreements(agreementsCaptor.capture());
    assertThat(agreementsCaptor.getValue())
        .hasSize(2)
        .allSatisfy(
            agreement -> {
              assertThat(agreement.getUserId()).isEqualTo(user.getId());
              assertThat(agreement.getAction()).isEqualTo(AgreementAction.AGREE);
              assertThat(agreement.getRecordedAt()).isNotNull();
            });
  }

  @Test
  @DisplayName("필수 동의가 누락되면 가입을 완료하지 않는다")
  void reject_missing_required_agreement() {
    // given
    User user = pendingUser();
    UUID tokenId = UUID.randomUUID();
    String token = signupTokenProvider.createToken(user.getId(), SocialType.APPLE, tokenId);
    SignupToken signupToken =
        SignupToken.issue(
            tokenId, user.getId(), SocialType.APPLE, LocalDateTime.now().plusMinutes(10));
    when(oauthRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    when(signupTokenRepository.findByTokenIdForUpdate(tokenId))
        .thenReturn(Optional.of(signupToken));
    SignupRequest request =
        new SignupRequest(
            token,
            List.of(new SignupAgreementRequest(AgreementType.TERMS_OF_SERVICE, "2026-08-01")));

    // when & then
    assertThatThrownBy(() -> signupService.complete(request)).isInstanceOf(UserException.class);
    assertThat(user.getStatus()).isEqualTo(UserStatus.SIGNUP_PENDING);
    assertThat(signupToken.isConsumed()).isFalse();
    verify(userAgreementRepository, never()).saveAgreements(any());
  }

  @Test
  @DisplayName("소진된 가입 토큰은 다시 사용할 수 없다")
  void reject_consumed_signup_token() {
    // given
    User user = pendingUser();
    UUID tokenId = UUID.randomUUID();
    String token = signupTokenProvider.createToken(user.getId(), SocialType.KAKAO, tokenId);
    SignupToken signupToken =
        SignupToken.issue(
            tokenId, user.getId(), SocialType.KAKAO, LocalDateTime.now().plusMinutes(10));
    signupToken.consume(LocalDateTime.now());
    when(oauthRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    when(signupTokenRepository.findByTokenIdForUpdate(tokenId))
        .thenReturn(Optional.of(signupToken));

    // when & then
    assertThatThrownBy(() -> signupService.complete(validRequest(token)))
        .isInstanceOf(UserException.class);
    verify(userAgreementRepository, never()).saveAgreements(any());
  }

  private SignupRequest validRequest(String token) {
    return new SignupRequest(
        token,
        List.of(
            new SignupAgreementRequest(AgreementType.TERMS_OF_SERVICE, "2026-08-01"),
            new SignupAgreementRequest(AgreementType.PRIVACY_COLLECTION_USE, "2026-08-01")));
  }

  private User pendingUser() {
    return User.builder()
        .id(1L)
        .email("pending@test.com")
        .nickName("가입대기유저")
        .role(UserType.ROLE_USER)
        .status(UserStatus.SIGNUP_PENDING)
        .socialType(List.of(SocialType.KAKAO))
        .build();
  }
}
