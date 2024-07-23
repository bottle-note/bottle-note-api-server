package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.CustomJwtException;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@DisplayName("[unit] [service] JwtTokenValidator")
@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

	@Test
	@DisplayName("리프레시 토큰이 만료되었을떄, 토큰 검증 메서드는 IllegalArgumentException을 던진다.")
	void test_token_validator_when_refresh_token_is_expired() {

		Date now = new Date();

		String tmpRefreshToken = Jwts.builder()
			.setClaims(Jwts.claims().setSubject("cha"))
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() - 3600 * 1000))
			.signWith(Keys.hmacShaKeyFor(
					"c2VjdXJlU2VjcmV0S2V5MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6QWJDRGVGR2hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlrSg==".getBytes()),
				SignatureAlgorithm.HS512)
			.compact();

		assertThrows(IllegalArgumentException.class, () -> JwtTokenValidator.validateToken(tmpRefreshToken));
	}

	@Test
	@DisplayName("리프레시 토큰이 null일떄, 토큰 검증 메서드는 CustomJwtException을 던진다.")
	void test_token_validator_when_refresh_token_is_null() {

		assertThrows(CustomJwtException.class, () -> JwtTokenValidator.validateToken(null));
	}
}
