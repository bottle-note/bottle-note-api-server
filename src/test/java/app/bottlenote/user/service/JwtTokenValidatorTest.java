package app.bottlenote.user.service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import app.bottlenote.global.security.jwt.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("JwtTokenValidator 테스트")
@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {
	
	@Test
	@DisplayName("리프레시 토큰이 만료되었을떄, 토큰 검증 메서드는 false를 반환한다.")
	void test_token_validator_when_refresh_token_is_expired() {

		Date now = new Date();

		String tmpRefreshToken = Jwts.builder()
			.setClaims(Jwts.claims().setSubject("cha"))
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() - 1000))
			.signWith(Keys.hmacShaKeyFor(
					"c2VjdXJasdfasdgagasgasgIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6QWJDRGVGR2hJSktMTU5asfdasgdsldYWVphYmNkZWZnaGlrSg==".getBytes()),
				SignatureAlgorithm.HS512)
			.compact();

		boolean isValid = JwtTokenValidator.validateToken(tmpRefreshToken);
		assertFalse(isValid);
	}

	@Test
	@DisplayName("리프레시 토큰이 null일떄, 토큰 검증 메서드는 false를 반환한다.")
	void test_token_validator_when_refresh_token_is_null() {

		boolean isValid = JwtTokenValidator.validateToken(null);
		assertFalse(isValid);
	}

}
