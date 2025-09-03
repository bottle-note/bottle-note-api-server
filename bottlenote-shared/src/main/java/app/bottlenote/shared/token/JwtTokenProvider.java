package app.bottlenote.shared.token;

import app.bottlenote.shared.users.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

public class JwtTokenProvider {
	public static final int ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 2;
	public static final int REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
	public static final String KEY_ROLES = "roles";
	private final Key secretKey;

	public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public TokenItem generateToken(String userEmail, UserType role, Long userId) {
		String accessToken = createAccessToken(userEmail, role, userId);
		String refreshToken = createRefreshToken(userEmail, role, userId);
		return TokenItem.builder().accessToken(accessToken).refreshToken(refreshToken).build();
	}

	public String createAccessToken(String userEmail, UserType role, Long userId) {
		Claims claims = createClaims(userEmail, role, userId);
		Date now = new Date();
		return Jwts.builder().setClaims(claims).setIssuedAt(now).setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME)).signWith(secretKey, SignatureAlgorithm.HS512).compact();
	}

	public String createRefreshToken(String userEmail, UserType role, Long userId) {
		Claims claims = createClaims(userEmail, role, userId);
		Date now = new Date();
		return Jwts.builder().setClaims(claims).setIssuedAt(now).setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME)).signWith(secretKey, SignatureAlgorithm.HS512).compact();
	}

	private Claims createClaims(String userEmail, UserType role, Long userId) {
		Claims claims = Jwts.claims().setSubject(userEmail);
		claims.put(KEY_ROLES, role.name());
		claims.put("userId", userId);
		return claims;
	}
}
