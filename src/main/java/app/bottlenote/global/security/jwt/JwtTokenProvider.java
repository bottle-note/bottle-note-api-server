package app.bottlenote.global.security.jwt;

import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.dto.response.TokenItem;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  public static final int ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 2;
  public static final int REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
  public static final String KEY_ROLES = "roles";
  private final Key secretKey;

  /**
   * secretKey를 받아 JwtTokenProvider를 생성하는 생성자
   *
   * @param secret 토큰 생성용 시크릿 키
   */
  public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * 필수적인 파라미터를 받아 엑세스 토큰과 리프레시 토큰을 생성하는 메소드
   *
   * @param userEmail 토큰 생성용 유저 이메일
   * @param role 유저의 권한
   * @param userId 유저 고유 아이디
   * @return OauthResponse ( 엑세스 토큰과 리프레시 토큰을 담은 객체 )
   */
  public TokenItem generateToken(String userEmail, UserType role, Long userId) {
    String accessToken = createAccessToken(userEmail, role, userId);
    String refreshToken = createRefreshToken(userEmail, role, userId);
    return TokenItem.builder().accessToken(accessToken).refreshToken(refreshToken).build();
  }

  /**
   * 필수적인 파라미터를 받아 엑세스 토큰을 생성하는 메소드
   *
   * @param userEmail 토큰 생성용 유저 이메일
   * @param role 유저의 권한
   * @param userId 유저 고유 아이디
   * @return access token ( 엑세스 토큰 )
   */
  public String createAccessToken(String userEmail, UserType role, Long userId) {
    Claims claims = createClaims(userEmail, role, userId);
    Date now = new Date();
    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  public String createGuestToken(Long userId, int expireTime) {
    Claims claims = Jwts.claims();
    claims.put("userId", userId);
    claims.put(KEY_ROLES, UserType.ROLE_GUEST.name());
    Date now = new Date();
    return Jwts.builder()
        .setClaims(claims)
        .setSubject("guest")
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + expireTime))
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 필수적인 파라미터를 받아 리프레시 토큰을 생성하는 메소드
   *
   * @param userEmail 토큰 생성용 유저 이메일
   * @param role 유저의 권한
   * @param userId 유저 고유 아이디
   * @return refresh token ( 리프레시 토큰 )
   */
  public String createRefreshToken(String userEmail, UserType role, Long userId) {
    Claims claims = createClaims(userEmail, role, userId);
    Date now = new Date();
    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 필수적인 파라미터를 받아 클레임을 생성하는 메소드
   *
   * @param userEmail 토큰 생성용 유저 이메일
   * @param role 유저의 권한
   * @param userId 유저 고유 아이디
   * @return 클레임 객체 ( 토큰에 담을 정보 )
   */
  private Claims createClaims(String userEmail, UserType role, Long userId) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    claims.put(KEY_ROLES, role.name());
    claims.put("userId", userId);
    return claims;
  }
}
