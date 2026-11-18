package app.bottlenote.global.security.jwt;

import static app.bottlenote.global.security.jwt.JwtTokenProvider.KEY_ROLES;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_TOKEN;
import static java.util.stream.Collectors.toList;

import app.bottlenote.global.security.CustomAdminUserDetailsService;
import app.bottlenote.user.exception.UserException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminJwtAuthenticationManager {

  private final CustomAdminUserDetailsService customAdminUserDetailsService;
  private final Key secretKey;

  public AdminJwtAuthenticationManager(
      @Value("${security.jwt.secret-key}") String secret,
      CustomAdminUserDetailsService customAdminUserDetailsService) {
    this.customAdminUserDetailsService = customAdminUserDetailsService;
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /** Admin 토큰을 받아 Authentication 객체를 생성 */
  public Authentication getAuthentication(String accessToken) {
    Claims claims = parseClaims(accessToken);
    log.debug("Admin 클레임 정보 : {}", claims.toString());

    Boolean isAdmin = claims.get("isAdmin", Boolean.class);
    if (isAdmin == null || !isAdmin) {
      throw new UserException(INVALID_TOKEN);
    }

    String rolesStr = claims.get(KEY_ROLES, String.class);
    if (rolesStr == null) {
      throw new UserException(INVALID_TOKEN);
    }

    List<GrantedAuthority> authorities =
        Arrays.stream(rolesStr.split(","))
            .map(String::trim)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(toList());

    UserDetails userDetails = customAdminUserDetailsService.loadUserByUsername(claims.getSubject());
    return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
  }

  private Claims parseClaims(String accessToken) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(accessToken)
          .getBody();
    } catch (ExpiredJwtException e) {
      return e.getClaims();
    }
  }
}
