package app.bottlenote.common.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);
		if (isValidToken(token)) {
			Authentication authentication = jwtTokenProvider.getAuthentication(token);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

		//resolveToken 메서드는 request에서 요청을 추출하는 책임만을 갖고 있기 떄문에 기본적인 검사만 수행
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			return extractToken(bearerToken);
		}
		return null;
	}

	/**
	 * 추출된 토큰이 실제 유효한 토큰인지 검증.
	 */
	private boolean isValidToken(String token) {
		return jwtTokenProvider.validateToken(token) && token != null && !token.trim().isEmpty();
	}

	/**
	 * 요청에서 토큰만 추출하는 메서드
	 */
	public static String extractToken(String request) {
		return request.substring(BEARER_PREFIX.length()).trim();
	}

}
