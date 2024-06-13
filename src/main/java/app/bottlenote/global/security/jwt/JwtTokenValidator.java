package app.bottlenote.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	public static boolean validateToken(String token) throws
		io.jsonwebtoken.security.SecurityException, MalformedJwtException,
		ExpiredJwtException, UnsupportedJwtException, IllegalArgumentException,
		CustomJwtException {

		if (token == null || token.trim().isEmpty()) {
			log.warn("토큰이 존재하지 않습니다.");
			throw new CustomJwtException(CustomJwtExceptionCode.EMPTY_JWT_TOKEN);
		}

		Jwts.parserBuilder()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token);

		return true;
	}

}
