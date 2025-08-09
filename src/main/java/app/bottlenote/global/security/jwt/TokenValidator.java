package app.bottlenote.global.security.jwt;

import io.jsonwebtoken.Claims;

public interface TokenValidator {

  Claims validateAndGetClaims(String idToken, String expectedNonce);

  String getSocialUniqueId(Claims claims);

  String getEmail(Claims claims);
}
