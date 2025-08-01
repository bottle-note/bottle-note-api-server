package app.bottlenote.global.security.jwt;

import io.jsonwebtoken.Claims;

public interface TokenValidator {

    Claims validateAndGetClaims(String idToken, String expectedNonce);

    String getEmail(Claims claims);
}
