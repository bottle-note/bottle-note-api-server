package app.bottlenote.global.security.jwt;

import static app.bottlenote.global.security.jwt.JwtTokenProvider.KEY_ROLES;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_TOKEN;
import static java.util.stream.Collectors.toList;

import app.bottlenote.global.security.customPrincipal.CustomUserDetailsService;
import app.bottlenote.user.domain.constant.SocialType;
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
public class JwtAuthenticationManager {

	private final CustomUserDetailsService customUserDetailsService;
	
	private final Key secretKey;

	public JwtAuthenticationManager(@Value("${security.jwt.secret-key}") String secret,
		CustomUserDetailsService customUserDetailsService) {
		this.customUserDetailsService = customUserDetailsService;
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
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
		
		UserDetails userDetails = customUserDetailsService.loadUserByUsernameAndSocialType(
			claims.getSubject(),
			SocialType.parsing((String) claims.get("socialType")));

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
