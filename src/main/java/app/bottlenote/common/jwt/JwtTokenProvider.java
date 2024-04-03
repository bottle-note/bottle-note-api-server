package app.bottlenote.common.jwt;

import java.security.Key;
import java.util.Date;

import app.bottlenote.common.jwt.dto.response.OauthResponse;
import app.bottlenote.user.constant.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory
	.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class JwtTokenProvider {

    private final Key secretKey;
    public static final int ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
    public static final int REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
    public static final String KEY_ROLES = "roles";

	public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	// 토큰 생성 메소드
	public OauthResponse generateToken(String userEmail, UserType role, Long userId){
		String accessToken = createAccessToken(userEmail, role, userId);
		String refreshToken = createRefreshToken(userEmail, role, userId);
		return OauthResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	// 엑세스 토큰 생성 메소드
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

	// 리프레시 토큰 생성 메소드
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

	// Claims 생성을 위한 공통 메소드
	private Claims createClaims(String userEmail, UserType role, Long userId) {
		Claims claims = Jwts.claims().setSubject(userEmail);
		claims.put(KEY_ROLES, role);
		claims.put("userId", userId);
		return claims;
	}


}
