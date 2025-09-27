package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.TokenValidator;
import app.bottlenote.shared.annotation.ThirdPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ThirdPartyService
@RequiredArgsConstructor
public class AppleAuthService {

  private final TokenValidator tokenValidator;
  private final NonceService nonceService;

  @Transactional(readOnly = true)
  public AppleUserInfo validateAndGetUserInfo(String idToken, String nonce) {
    // 1. Nonce 검증 (가장 먼저 수행하여 재전송 공격 방어)
    nonceService.validateNonce(nonce);

    // 2. id_token 검증 및 Claims 추출 (nonce 포함)
    var claims = tokenValidator.validateAndGetClaims(idToken, nonce);

    String socialUniqueId = tokenValidator.getSocialUniqueId(claims);
    String email = tokenValidator.getEmail(claims);

    return new AppleUserInfo(socialUniqueId, email);
  }

  public record AppleUserInfo(String socialUniqueId, String email) {}
}
