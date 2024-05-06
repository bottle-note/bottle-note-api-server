package app.bottlenote.global.security.jwt;

import app.bottlenote.global.security.customPrincipal.CustomUserDetailsService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static app.bottlenote.user.exception.UserExceptionCode.INVALID_TOKEN;
import static java.util.stream.Collectors.toList;


@Slf4j
@Component
public class JwtTokenProvider {

	private final Key secretKey;
	public static final int ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
	public static final int REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
	public static final String KEY_ROLES = "roles";
	private final CustomUserDetailsService customUserDetailsService;

	/**
	 * secretKey를 받아 JwtTokenProvider를 생성하는 생성자
	 *
	 * @param secret 토큰 생성용 시크릿 키
	 */
	public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret, CustomUserDetailsService customUserDetailsService) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.customUserDetailsService = customUserDetailsService;
	}


	/**
	 * 필수적인 파라미터를 받아 엑세스 토큰과 리프레시 토큰을 생성하는 메소드
	 *
	 * @param userEmail 토큰 생성용 유저 이메일
	 * @param role      유저의 권한
	 * @param userId    유저 고유 아이디
	 * @return OauthResponse ( 엑세스 토큰과 리프레시 토큰을 담은 객체 )
	 */
	public OauthResponse generateToken(String userEmail, UserType role, Long userId) {
		String accessToken = createAccessToken(userEmail, role, userId);
		String refreshToken = createRefreshToken(userEmail, role, userId);
		return OauthResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	/**
	 * 필수적인 파라미터를 받아 엑세스 토큰을 생성하는 메소드
	 *
	 * @param userEmail 토큰 생성용 유저 이메일
	 * @param role      유저의 권한
	 * @param userId    유저 고유 아이디
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


	/**
	 * 필수적인 파라미터를 받아 리프레시 토큰을 생성하는 메소드
	 *
	 * @param userEmail 토큰 생성용 유저 이메일
	 * @param role      유저의 권한
	 * @param userId    유저 고유 아이디
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
	 * @param role      유저의 권한
	 * @param userId    유저 고유 아이디
	 * @return 클레임 객체 ( 토큰에 담을 정보 )
	 */
	private Claims createClaims(String userEmail, UserType role, Long userId) {
		Claims claims = Jwts.claims().setSubject(userEmail);
		claims.put(KEY_ROLES, role.name());
		claims.put("userId", userId);
		return claims;
	}

	/**
	 * 토큰의 유효성을 검사하는 메소드
	 */
	public boolean validateToken(String token) {
		try {

			if (token == null || token.trim().isEmpty())
				return false;

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

	/**
	 * 토큰을 받아 클레임을 추출하고 Authentication 객체를 생성하는 메소드
	 */
	public Authentication getAuthentication(String accessToken) {

		Claims claims = parseClaims(accessToken);

		log.info("클레임 정보 : {}", claims.toString());

		String rolesStr = claims.get(KEY_ROLES, String.class);

		if (rolesStr == null) {
			throw new UserException(INVALID_TOKEN);
		}

		List<GrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
			.map(String::trim)
			.map(SimpleGrantedAuthority::new)
			.collect(toList());

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());

		return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
	}

	/**
	 * 토큰을 받아 클레임을 추출하는 메소드
	 */
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
