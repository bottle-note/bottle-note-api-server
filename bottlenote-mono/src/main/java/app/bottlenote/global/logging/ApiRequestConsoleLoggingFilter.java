package app.bottlenote.global.logging;

import app.bottlenote.global.security.SecurityContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

@Slf4j
public class ApiRequestConsoleLoggingFilter extends OncePerRequestFilter {

  static final String VISITOR_COOKIE_NAME = "__Host-bn-visitor-id";
  private static final Duration VISITOR_COOKIE_MAX_AGE = Duration.ofDays(365);
  private static final Set<String> ACTIVITY_METHODS =
      Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.currentTimeMillis();
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    boolean completed = false;

    try {
      filterChain.doFilter(request, responseWrapper);
      completed = true;
    } finally {
      long durationMs = System.currentTimeMillis() - startedAt;
      if (completed && isSuccessful(responseWrapper)) {
        findVisitorId(request)
            .ifPresentOrElse(
                visitorId -> logActivity(request, responseWrapper, durationMs, visitorId),
                () -> issueVisitorCookie(responseWrapper));
      }
      responseWrapper.copyBodyToResponse();
    }
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !ACTIVITY_METHODS.contains(request.getMethod())
        || !(path.startsWith("/api/v1/") || path.startsWith("/api/v2/"));
  }

  private Optional<String> findVisitorId(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, VISITOR_COOKIE_NAME);
    if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
      return Optional.empty();
    }

    try {
      return Optional.of(UUID.fromString(cookie.getValue()).toString());
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  private void issueVisitorCookie(HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(VISITOR_COOKIE_NAME, UUID.randomUUID().toString())
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(VISITOR_COOKIE_MAX_AGE)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private void logActivity(
      HttpServletRequest request, HttpServletResponse response, long durationMs, String visitorId) {
    log.info(
        "api_request traceId={} visitorId={} userId={} method={} path={} queryPresent={} status={} durationMs={}",
        MDC.get("traceId"),
        digest(visitorId),
        SecurityContextUtil.getUserIdByContext().orElse(null),
        request.getMethod(),
        request.getRequestURI(),
        hasQuery(request),
        response.getStatus(),
        durationMs);
  }

  private boolean isSuccessful(HttpServletResponse response) {
    return response.getStatus() >= 200 && response.getStatus() < 300;
  }

  private boolean hasQuery(HttpServletRequest request) {
    String queryString = request.getQueryString();
    return queryString != null && !queryString.isBlank();
  }

  private String digest(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
    }
  }
}
