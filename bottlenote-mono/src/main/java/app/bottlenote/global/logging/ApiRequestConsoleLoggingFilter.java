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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    ClientInfo clientInfo = parseClientInfo(request.getHeader(HttpHeaders.USER_AGENT));
    log.info(
        "api_request 추적ID={} 방문자ID={} 회원ID={} 기기유형={} 운영체제={} 브라우저={} 브라우저주버전={} 웹뷰={} 메서드={} 경로={} 쿼리존재={} 상태={} 처리시간ms={}",
        MDC.get("traceId"),
        digest(visitorId),
        SecurityContextUtil.getUserIdByContext().orElse(null),
        clientInfo.deviceType(),
        clientInfo.operatingSystem(),
        clientInfo.browser(),
        clientInfo.browserMajorVersion(),
        clientInfo.webView(),
        request.getMethod(),
        request.getRequestURI(),
        hasQuery(request),
        response.getStatus(),
        durationMs);
  }

  private ClientInfo parseClientInfo(String userAgent) {
    if (!StringUtils.hasText(userAgent)) {
      return new ClientInfo("알수없음", "알수없음", "알수없음", "알수없음", false);
    }

    boolean webView =
        userAgent.contains("; wv)")
            || (userAgent.contains("Android") && userAgent.contains("Version/4.0"))
            || (userAgent.contains("Mobile/")
                && userAgent.contains("AppleWebKit/")
                && !userAgent.contains("Safari/"));

    String deviceType;
    if (userAgent.contains("bot") || userAgent.contains("Bot") || userAgent.contains("spider")) {
      deviceType = "봇";
    } else if (userAgent.startsWith("curl/") || userAgent.startsWith("PostmanRuntime/")) {
      deviceType = "도구";
    } else if (userAgent.contains("iPad")
        || userAgent.contains("Tablet")
        || (userAgent.contains("Android") && !userAgent.contains("Mobile"))) {
      deviceType = "태블릿";
    } else if (userAgent.contains("Mobile") || userAgent.contains("Android")) {
      deviceType = "모바일";
    } else {
      deviceType = "데스크톱";
    }

    String operatingSystem =
        userAgent.contains("Android")
            ? "Android"
            : userAgent.contains("iPhone") || userAgent.contains("iPad")
                ? "iOS"
                : userAgent.contains("Windows")
                    ? "Windows"
                    : userAgent.contains("Macintosh")
                        ? "macOS"
                        : userAgent.contains("Linux") ? "Linux" : "알수없음";

    String browser =
        userAgent.contains("Whale/")
            ? "Whale"
            : userAgent.contains("Edg")
                ? "Edge"
                : userAgent.contains("SamsungBrowser/")
                    ? "SamsungInternet"
                    : userAgent.contains("CriOS/") || userAgent.contains("Chrome/")
                        ? "Chrome"
                        : userAgent.contains("FxiOS/") || userAgent.contains("Firefox/")
                            ? "Firefox"
                            : userAgent.contains("Safari/") ? "Safari" : webView ? "WebView" : "기타";

    String browserMajorVersion = extractBrowserMajorVersion(userAgent, browser);
    return new ClientInfo(deviceType, operatingSystem, browser, browserMajorVersion, webView);
  }

  private String extractBrowserMajorVersion(String userAgent, String browser) {
    String token =
        switch (browser) {
          case "Whale" -> "Whale";
          case "Edge" ->
              userAgent.contains("EdgiOS/")
                  ? "EdgiOS"
                  : userAgent.contains("EdgA/") ? "EdgA" : "Edg";
          case "SamsungInternet" -> "SamsungBrowser";
          case "Chrome" -> userAgent.contains("CriOS/") ? "CriOS" : "Chrome";
          case "Firefox" -> userAgent.contains("FxiOS/") ? "FxiOS" : "Firefox";
          case "Safari" -> "Version";
          default -> null;
        };
    if (token == null) {
      return "알수없음";
    }

    Matcher matcher = Pattern.compile(Pattern.quote(token) + "/(\\d+)").matcher(userAgent);
    return matcher.find() ? matcher.group(1) : "알수없음";
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

  private record ClientInfo(
      String deviceType,
      String operatingSystem,
      String browser,
      String browserMajorVersion,
      boolean webView) {}
}
