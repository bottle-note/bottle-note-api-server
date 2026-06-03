package app.bottlenote.global.security.jwt;

import static java.util.Objects.requireNonNullElse;

import app.bottlenote.global.security.policy.ProductApiAccessPolicy;
import app.bottlenote.global.security.policy.ProductApiAccessPolicy.AccessType;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
   * @param request 클라이언트의 요청
   * @param response 서버의 응답
   * @param filterChain 필터 체인
   * @throws ServletException 서블릿 예외
   * @throws IOException 입출력 예외
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String method = request.getMethod();
    final String path = request.getRequestURI();
    String token = resolveToken(request).orElse(null);

    log.debug("Performs filtering inside the JWT. >> {} : {} ", method, path);

    try {
      Authentication authentication = null;

      if (token != null && !token.isBlank()) {
        if (!JwtTokenValidator.validateToken(token)) {
          log.info("MalformedJwtException : 토큰이 유효하지 않습니다.");
          request.setAttribute("exception", new MalformedJwtException("토큰이 유효하지 않습니다."));
          filterChain.doFilter(request, response);
          return;
        }
        authentication = jwtAuthenticationManager.getAuthentication(token);
      } else if (shouldUseAnonymousAuthentication(method, path, token)) {
        authentication = jwtAuthenticationManager.getAnonymousAuthentication();
      }
      if (authentication != null) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }

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

  /** Authorization 헤더에서 토큰을 추출하는 메서드. */
  private Optional<String> resolveToken(HttpServletRequest request) {

    String bearerToken = requireNonNullElse(request.getHeader(AUTHORIZATION_HEADER), "");

    if (bearerToken.startsWith(BEARER_PREFIX)) {
      return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
    }

    return Optional.empty();
  }

  static boolean shouldUseAnonymousAuthentication(String method, String path, String token) {
    return (token == null || token.isBlank())
        && ProductApiAccessPolicy.resolve(method, path) != AccessType.REQUIRED_AUTH;
  }

  /** 인증 컨텍스트가 필요 없는 공개 API만 필터에서 제외한다. */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return ProductApiAccessPolicy.shouldSkipJwtFilter(request.getMethod(), request.getRequestURI());
  }
}
