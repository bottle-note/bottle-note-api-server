package app.bottlenote.user.service;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_SIGNUP_AGREEMENTS;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_SIGNUP_TOKEN;
import static app.bottlenote.user.exception.UserExceptionCode.SIGNUP_ALREADY_COMPLETED;
import static app.bottlenote.user.exception.UserExceptionCode.SIGNUP_TOKEN_CONSUMED;

import app.bottlenote.global.security.jwt.SignupTokenProvider;
import app.bottlenote.global.security.jwt.SignupTokenProvider.SignupTokenClaims;
import app.bottlenote.user.constant.AgreementType;
import app.bottlenote.user.constant.SocialType;
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
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

  private static final Set<AgreementType> REQUIRED_AGREEMENTS =
      EnumSet.of(AgreementType.TERMS_OF_SERVICE, AgreementType.PRIVACY_COLLECTION_USE);

  private final OauthRepository oauthRepository;
  private final SignupTokenRepository signupTokenRepository;
  private final UserAgreementRepository userAgreementRepository;
  private final SignupTokenProvider signupTokenProvider;

  @Transactional
  public String issueToken(User user, SocialType socialType) {
    if (!user.isSignupPending()) {
      throw new UserException(SIGNUP_ALREADY_COMPLETED);
    }

    UUID tokenId = UUID.randomUUID();
    String token = signupTokenProvider.createToken(user.getId(), socialType, tokenId);
    SignupTokenClaims claims = signupTokenProvider.parse(token);
    LocalDateTime expiresAt = LocalDateTime.ofInstant(claims.expiresAt(), ZoneId.systemDefault());
    signupTokenRepository.saveToken(
        SignupToken.issue(tokenId, user.getId(), socialType, expiresAt));
    return token;
  }

  @Transactional
  public void complete(SignupRequest request) {
    validateAgreements(request.agreements());
    SignupTokenClaims claims = parse(request.signupToken());

    User user =
        oauthRepository
            .findByIdForUpdate(claims.userId())
            .orElseThrow(() -> new UserException(INVALID_SIGNUP_TOKEN));
    if (!user.isSignupPending()) {
      throw new UserException(SIGNUP_ALREADY_COMPLETED);
    }

    SignupToken signupToken =
        signupTokenRepository
            .findByTokenIdForUpdate(claims.tokenId())
            .orElseThrow(() -> new UserException(INVALID_SIGNUP_TOKEN));
    LocalDateTime now = LocalDateTime.now();
    if (signupToken.isConsumed()) {
      throw new UserException(SIGNUP_TOKEN_CONSUMED);
    }
    if (!signupToken.belongsTo(claims.userId(), claims.socialType())
        || signupToken.isExpired(now)) {
      throw new UserException(INVALID_SIGNUP_TOKEN);
    }

    List<UserAgreement> agreements =
        request.agreements().stream()
            .map(
                agreement ->
                    UserAgreement.agree(
                        user.getId(), agreement.type(), agreement.version().trim(), now))
            .toList();
    userAgreementRepository.saveAgreements(agreements);
    user.activate();
    signupToken.consume(now);
  }

  private SignupTokenClaims parse(String token) {
    try {
      return signupTokenProvider.parse(token);
    } catch (IllegalArgumentException exception) {
      throw new UserException(INVALID_SIGNUP_TOKEN);
    }
  }

  private void validateAgreements(List<SignupAgreementRequest> agreements) {
    if (agreements == null || agreements.size() != REQUIRED_AGREEMENTS.size()) {
      throw new UserException(INVALID_SIGNUP_AGREEMENTS);
    }
    if (agreements.stream()
        .anyMatch(
            agreement ->
                agreement == null
                    || agreement.type() == null
                    || agreement.version() == null
                    || agreement.version().isBlank())) {
      throw new UserException(INVALID_SIGNUP_AGREEMENTS);
    }

    Set<AgreementType> submitted =
        agreements.stream().map(SignupAgreementRequest::type).collect(Collectors.toSet());
    if (!submitted.equals(REQUIRED_AGREEMENTS)) {
      throw new UserException(INVALID_SIGNUP_AGREEMENTS);
    }
  }
}
