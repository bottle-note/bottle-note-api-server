package app.bottlenote.global.security.jwt;

import static java.util.Objects.requireNonNullElse;

import app.bottlenote.global.security.constant.MaliciousPathPattern;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.user.exception.UserException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";

  private final AdminJwtAuthenticationManager adminJwtAuthenticationManager;
  private final SecurityPolicyRegistry securityPolicyRegistry;
  private final RequestMatcher maliciousPathMatcher;

  public AdminJwtAuthenticationFilter(
      AdminJwtAuthenticationManager adminJwtAuthenticationManager,
      SecurityPolicyRegistry securityPolicyRegistry) {
    this(adminJwtAuthenticationManager, securityPolicyRegistry, MaliciousPathPattern::matches);
  }

  /**
   * 악성 경로 판정 기준을 체인에 맞춰 받는다.
   *
   * <p>관리자 API를 context-path 없이 {@code /admin/api} 아래에 둔 통합 앱은 체인의 인가 규칙과 같은 기준({@link
   * MaliciousPathPattern#matchesUnder})을 넘긴다. 필터와 인가 규칙의 판정이 어긋나면 공개 경로의 토큰 처리가 달라진다.
   */
  public AdminJwtAuthenticationFilter(
      AdminJwtAuthenticationManager adminJwtAuthenticationManager,
      SecurityPolicyRegistry securityPolicyRegistry,
      RequestMatcher maliciousPathMatcher) {
    this.adminJwtAuthenticationManager = adminJwtAuthenticationManager;
    this.securityPolicyRegistry = securityPolicyRegistry;
    this.maliciousPathMatcher = maliciousPathMatcher;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String method = request.getMethod();
    final String path = SecurityPolicyRegistry.lookupPath(request);
    String token = resolveToken(request).orElse(null);

    log.debug("Admin JWT 필터 수행 >> {} : {}", method, path);

    try {
      if (token == null || token.isBlank()) {
        if (maliciousPathMatcher.matches(request)
            || securityPolicyRegistry.shouldUseAnonymousAuthentication(request, token)) {
          filterChain.doFilter(request, response);
          return;
        }
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
    } catch (UserException e) {
      log.warn("Admin 토큰이 아닙니다.");
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
    return CorsUtils.isPreFlightRequest(request)
        || (!maliciousPathMatcher.matches(request)
            && securityPolicyRegistry.shouldSkipJwtFilter(request));
  }
}
