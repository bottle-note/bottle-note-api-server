package app.bottlenote.global.security.jwt;


import static java.util.Objects.requireNonNullElse;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

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

		log.info("JWT Filtering....{} , entry point : {} ", request.getServletPath(), request.getRequestURI());

		String token = resolveToken(request).orElse(null);

		try {
			if (!JwtTokenValidator.validateToken(token)) {
				log.warn("토큰이 유효하지 않습니다. : {}", token);
				request.setAttribute("exception", new MalformedJwtException("토큰이 유효하지 않습니다."));
				filterChain.doFilter(request, response);
				return;
			}
			Authentication authentication = jwtAuthenticationManager.getAuthentication(token);
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
			log.warn("JWT 토큰이 존재하지 않습니다.");
			request.setAttribute("exception", e);
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Authorization 헤더에서 토큰을 추출하는 메서드.
	 *
	 * @param request HttpServletRequest 객체
	 * @return 토큰이 포함된 Optional 객체. 토큰이 없으면 빈 Optional 반환
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
	 *
	 * @param request the request
	 * @return the boolean
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		List<String> excludePath = List.of(
			"/api/v1/alcohols/categories",
			"/api/v1/regions",
			"/api/v1/popular/week"
		);

		return excludePath
			.stream()
			.anyMatch(path::contains);
	}

}
