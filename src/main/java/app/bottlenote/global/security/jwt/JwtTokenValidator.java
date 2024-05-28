package app.bottlenote.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtTokenValidator {

	private static Key secretKey;

	public JwtTokenValidator(
		@Value("${security.jwt.secret-key}") String secret
	) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * 토큰의 유효성을 검사하는 메소드
	 */
	public static boolean validateToken(String token) {
		try {

			if (token == null || token.trim().isEmpty()) {
				log.warn("토큰이 존재하지 않습니다.");
				return false;
			}

			Jwts.parserBuilder()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token);

			return true;

		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.debug("잘못된 JWT 서명 입니다.");
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT 토큰 입니다.");
		} catch (UnsupportedJwtException e) {
			log.debug("지원되지 않는 JWT 토큰 입니다.");
		} catch (IllegalArgumentException e) {
			log.debug("JWT 토큰이 잘못 되었습니다.");
		}
		return false;
	}

}
