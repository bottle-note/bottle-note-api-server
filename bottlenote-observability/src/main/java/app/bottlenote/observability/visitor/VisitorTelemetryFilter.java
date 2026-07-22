package app.bottlenote.observability.visitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/** 성공한 Product API 요청을 쿠키 기반 방문 단위로 수집하는 필터다. */
@Slf4j
public final class VisitorTelemetryFilter extends OncePerRequestFilter {

  public static final String VISITOR_COOKIE_NAME = "__Host-bn-visitor-id";

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final Duration VISITOR_COOKIE_MAX_AGE = Duration.ofDays(365);
  private static final Set<String> TELEMETRY_METHODS =
      Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
  private static final Set<String> SENSITIVE_QUERY_KEYWORDS =
      Set.of(
          "token",
          "authorization",
          "password",
          "secret",
          "apikey",
          "code",
          "csrf",
          "email",
          "phone");

  private final VisitorTelemetryPublisher publisher;
  private final Supplier<Long> userIdSupplier;
  private final Function<HttpServletRequest, String> clientIpResolver;
  private final Clock clock;

  public VisitorTelemetryFilter(
      VisitorTelemetryPublisher publisher,
      Supplier<Long> userIdSupplier,
      Function<HttpServletRequest, String> clientIpResolver) {
    this(publisher, userIdSupplier, clientIpResolver, Clock.system(KOREA_ZONE_ID));
  }

  VisitorTelemetryFilter(
      VisitorTelemetryPublisher publisher,
      Supplier<Long> userIdSupplier,
      Function<HttpServletRequest, String> clientIpResolver,
      Clock clock) {
    this.publisher = publisher;
    this.userIdSupplier = userIdSupplier;
    this.clientIpResolver = clientIpResolver;
    this.clock = clock;
  }

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
        String visitorId = findVisitorId(request).orElseGet(() -> issueVisitorCookie(responseWrapper));
        VisitorTelemetry telemetry = createTelemetry(request, responseWrapper, durationMs, visitorId);
        logTelemetry(telemetry);
        publishTelemetry(telemetry);
      }
      responseWrapper.copyBodyToResponse();
    }
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !TELEMETRY_METHODS.contains(request.getMethod())
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

  private String issueVisitorCookie(HttpServletResponse response) {
    String visitorId = UUID.randomUUID().toString();
    ResponseCookie cookie =
        ResponseCookie.from(VISITOR_COOKIE_NAME, visitorId)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(VISITOR_COOKIE_MAX_AGE)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return visitorId;
  }

  private VisitorTelemetry createTelemetry(
      HttpServletRequest request, HttpServletResponse response, long durationMs, String visitorId) {
    ClientInfo clientInfo = parseClientInfo(request.getHeader(HttpHeaders.USER_AGENT));
    Object bestMatchingPattern =
        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String normalizedRequestPath =
        bestMatchingPattern == null ? request.getRequestURI() : bestMatchingPattern.toString();

    return new VisitorTelemetry(
        LocalDateTime.now(clock),
        digest(visitorId),
        userIdSupplier.get(),
        clientIpResolver.apply(request),
        MDC.get("traceId"),
        request.getMethod(),
        sanitizedRequestPath(request),
        request.getRequestURI(),
        normalizedRequestPath,
        response.getStatus(),
        durationMs,
        clientInfo.deviceType(),
        clientInfo.operatingSystem(),
        clientInfo.browser(),
        clientInfo.browserMajorVersion(),
        clientInfo.webview());
  }

  private String sanitizedRequestPath(HttpServletRequest request) {
    String queryString = request.getQueryString();
    if (!StringUtils.hasText(queryString)) {
      return request.getRequestURI();
    }

    String[] parameters = queryString.split("&", -1);
    for (int index = 0; index < parameters.length; index++) {
      String parameter = parameters[index];
      int separatorIndex = parameter.indexOf('=');
      String rawKey = separatorIndex < 0 ? parameter : parameter.substring(0, separatorIndex);
      if (isSensitiveQueryKey(rawKey)) {
        parameters[index] = rawKey + "=[REDACTED]";
      }
    }
    return request.getRequestURI() + "?" + String.join("&", parameters);
  }

  private boolean isSensitiveQueryKey(String rawKey) {
    String decodedKey;
    try {
      decodedKey = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      decodedKey = rawKey;
    }
    String normalizedKey =
        decodedKey.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    return SENSITIVE_QUERY_KEYWORDS.stream().anyMatch(normalizedKey::contains);
  }

  private ClientInfo parseClientInfo(String userAgent) {
    if (!StringUtils.hasText(userAgent)) {
      return new ClientInfo("알수없음", "알수없음", "알수없음", "알수없음", false);
    }

    boolean webview =
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
                            : userAgent.contains("Safari/")
                                ? "Safari"
                                : webview ? "WebView" : "기타";

    return new ClientInfo(
        deviceType,
        operatingSystem,
        browser,
        extractBrowserMajorVersion(userAgent, browser),
        webview);
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

  private void logTelemetry(VisitorTelemetry telemetry) {
    log.debug(
        "visitor_telemetry 추적ID={} 방문자ID={} 기기유형={} 운영체제={} 브라우저={} "
            + "브라우저주버전={} 웹뷰={} 메서드={} 요청경로={} 정규화경로={} 상태={} 처리시간ms={}",
        telemetry.traceId(),
        telemetry.visitorId(),
        telemetry.deviceType(),
        telemetry.operatingSystem(),
        telemetry.browser(),
        telemetry.browserMajorVersion(),
        telemetry.webview(),
        telemetry.httpMethod(),
        telemetry.requestPath(),
        telemetry.normalizedRequestPath(),
        telemetry.statusCode(),
        telemetry.durationMs());
  }

  private void publishTelemetry(VisitorTelemetry telemetry) {
    try {
      publisher.publish(telemetry);
    } catch (RuntimeException exception) {
      log.warn("VisitorTelemetry 발행 실패", exception);
    }
  }

  private boolean isSuccessful(HttpServletResponse response) {
    return response.getStatus() >= 200 && response.getStatus() < 300;
  }

  private String digest(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
    }
  }

  private record ClientInfo(
      String deviceType,
      String operatingSystem,
      String browser,
      String browserMajorVersion,
      boolean webview) {}
}
