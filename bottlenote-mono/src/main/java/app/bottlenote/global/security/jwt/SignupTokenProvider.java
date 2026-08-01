package app.bottlenote.global.security.jwt;

import app.bottlenote.user.constant.SocialType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SignupTokenProvider {

  static final long SIGNUP_TOKEN_EXPIRE_SECONDS = 60 * 10;
  private static final String TOKEN_USE = "signup";
  private static final String TOKEN_USE_CLAIM = "tokenUse";
  private static final String USER_ID_CLAIM = "userId";
  private static final String SOCIAL_TYPE_CLAIM = "socialType";

  private final Key secretKey;

  public SignupTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String createToken(Long userId, SocialType socialType, UUID tokenId) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(SIGNUP_TOKEN_EXPIRE_SECONDS);
    return Jwts.builder()
        .setId(tokenId.toString())
        .setIssuedAt(Date.from(issuedAt))
        .setExpiration(Date.from(expiresAt))
        .claim(TOKEN_USE_CLAIM, TOKEN_USE)
        .claim(USER_ID_CLAIM, userId)
        .claim(SOCIAL_TYPE_CLAIM, socialType.name())
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  public SignupTokenClaims parse(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(secretKey)
              .require(TOKEN_USE_CLAIM, TOKEN_USE)
              .build()
              .parseClaimsJws(token)
              .getBody();
      return new SignupTokenClaims(
          Long.valueOf(claims.get(USER_ID_CLAIM).toString()),
          SocialType.valueOf(claims.get(SOCIAL_TYPE_CLAIM, String.class)),
          UUID.fromString(claims.getId()),
          claims.getIssuedAt().toInstant(),
          claims.getExpiration().toInstant());
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("유효하지 않은 가입 토큰입니다.", exception);
    }
  }

  public record SignupTokenClaims(
      Long userId, SocialType socialType, UUID tokenId, Instant issuedAt, Instant expiresAt) {}
}
