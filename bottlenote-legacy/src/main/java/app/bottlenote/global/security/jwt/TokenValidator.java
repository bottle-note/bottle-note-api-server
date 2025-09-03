package app.bottlenote.global.security.jwt;

import io.jsonwebtoken.Claims;

public interface TokenValidator {

  // todo: 포지션 변경 (apple 토큰 검증용 인터페이스)
  //  core 모듈로 이동.
  Claims validateAndGetClaims(String idToken, String expectedNonce);

  String getSocialUniqueId(Claims claims);

  String getEmail(Claims claims);
}
