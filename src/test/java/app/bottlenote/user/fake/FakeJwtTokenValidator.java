package app.bottlenote.user.fake;

import app.bottlenote.global.security.jwt.CustomJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;

@Slf4j
public class FakeJwtTokenValidator {

	private final Key secretKey;

	public FakeJwtTokenValidator() {
		// 테스트용 시크릿 키로 초기화
		String secret = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * 토큰의 유효성을 검사하는 메소드
	 */
	public boolean validateToken(String token) throws
			io.jsonwebtoken.security.SecurityException, MalformedJwtException,
			ExpiredJwtException, UnsupportedJwtException, IllegalArgumentException,
			CustomJwtException {

		if (token == null || token.trim().isEmpty()) {
			return false;
		}

		// invalid 토큰은 항상 검증 실패
		if ("invalid-refresh-token".equals(token)) {
			return false;
		}

		try {
			Jwts.parserBuilder()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}