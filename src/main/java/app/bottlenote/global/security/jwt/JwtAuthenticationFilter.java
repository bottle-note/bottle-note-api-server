package app.bottlenote.global.security.jwt;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	private final JwtAuthenticationManager jwtAuthenticationManager;

	/**
	 * 내부 필터링 로직을 처리하는 메서드.
	 *
	 * @param request     클라이언트의 요청
	 * @param response    서버의 응답
	 * @param filterChain 필터 체인
	 * @throws ServletException 서블릿 예외
	 * @throws IOException      입출력 예외
	 */
	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		final String method = request.getMethod();
		final String path = request.getServletPath();
		String token = resolveToken(request).orElse(null);

		log.info("Performs filtering inside the JWT. >> {} : {} ", method, path);

		try {
			Authentication authentication = jwtAuthenticationManager.getAnonymousAuthentication();

			if (skipFilter(method, path)) {
				log.info("선택적인 인증이 필요한 경우 익명 사용자로 설정합니다.");
				authentication = jwtAuthenticationManager.getAnonymousAuthentication();
			}

			if (token != null && !token.isBlank()) {
				if (!JwtTokenValidator.validateToken(token)) {
					log.info("MalformedJwtException : 토큰이 유효하지 않습니다.");
					request.setAttribute("exception", new MalformedJwtException("토큰이 유효하지 않습니다."));
					filterChain.doFilter(request, response);
					return;
				}
				authentication = jwtAuthenticationManager.getAuthentication(token);
			}
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.warn("잘못된 JWT 서명 입니다.");
			request.setAttribute("exception", e);
		} catch (ExpiredJwtException e) {
			log.warn("만료된 JWT 토큰 입니다.");
			request.setAttribute("exception", e);
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 JWT 토큰 입니다.");
			request.setAttribute("exception", e);
		} catch (IllegalArgumentException e) {
			log.warn("JWT 토큰이 잘못 되었습니다.");
			request.setAttribute("exception", e);
		} catch (CustomJwtException e) {
			log.warn("JWT 토큰이 존재하지 않습니다. {} ", e.getMessage());
			request.setAttribute("exception", e);
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * JWT 토큰을 이용하여 Authentication 객체를 생성
	 */
	private Authentication getAuthentication(HttpServletRequest request, String method, String path, String token) throws SecurityException {
		Authentication authentication = jwtAuthenticationManager.getAnonymousAuthentication();

		if (skipFilter(method, path)) {
			log.info("선택적인 인증이 필요한 경우 익명 사용자로 설정합니다.");
			authentication = jwtAuthenticationManager.getAnonymousAuthentication();
		}

		log.info(" 비회원 이용가능 api: {}", path);

		if (token != null && !token.isBlank()) { // 토큰이 존재하는 경우
			if (!JwtTokenValidator.validateToken(token)) {
				request.setAttribute("exception", new MalformedJwtException("토큰이 유효하지 않습니다."));
			}
			authentication = jwtAuthenticationManager.getAuthentication(token);
		}

		return authentication;
	}

	/**
	 * Authorization 헤더에서 토큰을 추출하는 메서드.
	 */
	private Optional<String> resolveToken(HttpServletRequest request) {

		String bearerToken = requireNonNullElse(request.getHeader(AUTHORIZATION_HEADER), "");

		if (bearerToken.startsWith(BEARER_PREFIX)) {
			return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
		}

		return Optional.empty();
	}

	/**
	 * 필터 제외 대상 경로를 설정
	 * : userId검증이 필요없는 API 경로를 필터링
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		List<String> excludePath = List.of(
			"login",
			"guest-login",
			"regions",
			"alcohols/categories"
		);

		return excludePath
			.stream()
			.anyMatch(path::contains);
	}

	/**
	 * 유저의 인증 정보가 선택적으로 필요한 경우를 판단하기 위한 메소드<br>
	 * 특정 API에 대한 인증이 필요하지 않은 경우 익명 사용자로 설정<br>
	 * <p>
	 * 예를 들어 리뷰 조회, 평점 조회, 인기술 등의 API는 인증이 필요하지 않음 <br>
	 * 다만 내가 픽했는지 여부등을 파악하기 위해 선택적으로 제공
	 * <p>
	 * 수정 : 2024-11-10
	 */
	private boolean skipFilter(String method, String url) {
		log.info("Checking skipFilter : {}", method + ":" + url);
		final String targetPath = method + ":" + url;
		final Set<String> skipPaths = Set.of(
			"GET:/api/v1/reviews",
			"GET:/api/v1/rating",
			"GET:/api/v1/popular",
			"GET:/api/v1/alcohols"
		);
		return skipPaths.stream()
			.anyMatch(targetPath::startsWith);
	}

}
