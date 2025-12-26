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
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";

  private final AdminJwtAuthenticationManager adminJwtAuthenticationManager;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String method = request.getMethod();
    final String path = request.getServletPath();
    String token = resolveToken(request).orElse(null);

    log.debug("Admin JWT 필터 수행 >> {} : {}", method, path);

    try {
      if (token == null || token.isBlank()) {
        log.debug("Admin API 접근 시 토큰이 필요합니다. path: {}", path);
        request.setAttribute(
            "exception", new CustomJwtException(CustomJwtExceptionCode.EMPTY_JWT_TOKEN));
        filterChain.doFilter(request, response);
        return;
      }

      if (!JwtTokenValidator.validateToken(token)) {
        log.warn("유효하지 않은 Admin 토큰입니다.");
        request.setAttribute("exception", new MalformedJwtException("토큰이 유효하지 않습니다."));
        filterChain.doFilter(request, response);
        return;
      }

      Authentication authentication = adminJwtAuthenticationManager.getAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
      log.warn("잘못된 JWT 서명입니다.");
      request.setAttribute("exception", e);
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰입니다.");
      request.setAttribute("exception", e);
    } catch (UnsupportedJwtException e) {
      log.warn("지원되지 않는 JWT 토큰입니다.");
      request.setAttribute("exception", e);
    } catch (IllegalArgumentException e) {
      log.warn("JWT 토큰이 잘못되었습니다.");
      request.setAttribute("exception", e);
    } catch (CustomJwtException e) {
      log.warn("JWT 예외: {}", e.getMessage());
      request.setAttribute("exception", e);
    }

    filterChain.doFilter(request, response);
  }

  private Optional<String> resolveToken(HttpServletRequest request) {
    String bearerToken = requireNonNullElse(request.getHeader(AUTHORIZATION_HEADER), "");
    if (bearerToken.startsWith(BEARER_PREFIX)) {
      return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
    }
    return Optional.empty();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    List<String> excludePaths = List.of("/auth/login", "/auth/refresh", "/actuator");
    return excludePaths.stream().anyMatch(path::contains);
  }
}
