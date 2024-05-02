package app.bottlenote.common.jwt;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_TOKEN;
import static java.util.stream.Collectors.toList;

import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.exception.UserException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class JwtTokenProvider {

	private final Key secretKey;
	public static final int ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
	public static final int REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
	public static final String KEY_ROLES = "roles";

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
	 *  필수적인 파라미터를 받아 엑세스 토큰과 리프레시 토큰을 생성하는 메소드
	 *
	 * @param userEmail 토큰 생성용 유저 이메일
	 * @param role 유저의 권한
	 * @param userId 유저 고유 아이디
	 * @return OauthResponse ( 엑세스 토큰과 리프레시 토큰을 담은 객체 )
	 */
	public OauthResponse generateToken(String userEmail, UserType role, Long userId){
		String accessToken = createAccessToken(userEmail, role, userId);
		String refreshToken = createRefreshToken(userEmail, role, userId);
		return OauthResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	/**
	 *  필수적인 파라미터를 받아 엑세스 토큰을 생성하는 메소드
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

	/**
	 * 필수적인 파라미터를 받아 클레임을 생성하는 메소드
	 *
	 * @param userEmail
	 * @param role
	 * @param userId
	 * @return 클레임 객체 ( 토큰에 담을 정보 )
	 */
	private Claims createClaims(String userEmail, UserType role, Long userId) {
		Claims claims = Jwts.claims().setSubject(userEmail);
		claims.put(KEY_ROLES, role.name());
		claims.put("userId", userId);
		return claims;
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
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

	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);
		log.info("클레임 정보 : {}", claims.toString());
		String rolesStr = claims.get(KEY_ROLES, String.class);
		if (rolesStr == null) {
			throw new UserException(INVALID_TOKEN);
		}
		List<GrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
			.map(String::trim)
			.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
			.collect(toList());
		UserDetails principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

}
