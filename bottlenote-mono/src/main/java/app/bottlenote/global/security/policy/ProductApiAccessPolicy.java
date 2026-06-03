package app.bottlenote.global.security.policy;

import java.util.List;
import java.util.Locale;

public final class ProductApiAccessPolicy {

  private static final List<EndpointRule> PUBLIC_RULES =
      List.of(
          EndpointRule.exact("POST", "/api/v1/oauth/login"),
          EndpointRule.prefix("POST", "/api/v1/oauth/"),
          EndpointRule.exact("GET", "/api/v1/regions"),
          EndpointRule.exact("GET", "/api/v1/alcohols/categories"),
          EndpointRule.prefix("GET", "/api/v1/banners"),
          EndpointRule.prefix("GET", "/api/v2/auth/apple/nonce"));

  private static final List<EndpointRule> OPTIONAL_AUTH_RULES =
      List.of(
          EndpointRule.prefix("GET", "/api/v1/alcohols"),
          EndpointRule.prefix("GET", "/api/v1/popular"),
          EndpointRule.prefix("GET", "/api/v1/reviews/detail"),
          EndpointRule.prefix("GET", "/api/v1/reviews/explore"),
          EndpointRule.prefix("GET", "/api/v1/reviews"),
          EndpointRule.prefix("GET", "/api/v1/review/reply"),
          EndpointRule.prefix("GET", "/api/v1/rating"),
          EndpointRule.prefix("GET", "/api/v1/my-page"));

  private static final List<EndpointRule> REQUIRED_AUTH_RULES =
      List.of(
          EndpointRule.prefix("ANY", "/api/v1/picks"),
          EndpointRule.prefix("ANY", "/api/v1/s3"),
          EndpointRule.prefix("ANY", "/api/v1/follow"),
          EndpointRule.prefix("GET", "/api/v1/reviews/me"),
          EndpointRule.prefix("POST", "/api/v1/reviews"),
          EndpointRule.prefix("PATCH", "/api/v1/reviews"),
          EndpointRule.prefix("DELETE", "/api/v1/reviews"),
          EndpointRule.prefix("ANY", "/api/v1/users"),
          EndpointRule.prefix("ANY", "/api/v1/help"),
          EndpointRule.prefix("ANY", "/api/v1/reports"),
          EndpointRule.prefix("ANY", "/api/v1/history"),
          EndpointRule.prefix("ANY", "/api/v1/blocks"),
          EndpointRule.prefix("ANY", "/api/v1/likes"),
          EndpointRule.prefix("POST", "/api/v1/rating"),
          EndpointRule.prefix("PUT", "/api/v1/rating"),
          EndpointRule.prefix("PATCH", "/api/v1/rating"),
          EndpointRule.prefix("DELETE", "/api/v1/rating"),
          EndpointRule.prefix("POST", "/api/v1/review/reply"),
          EndpointRule.prefix("PUT", "/api/v1/review/reply"),
          EndpointRule.prefix("PATCH", "/api/v1/review/reply"),
          EndpointRule.prefix("DELETE", "/api/v1/review/reply"));

  private ProductApiAccessPolicy() {}

  public static AccessType resolve(String method, String path) {
    String normalizedMethod = normalizeMethod(method);
    String normalizedPath = normalizePath(path);

    if (matches(REQUIRED_AUTH_RULES, normalizedMethod, normalizedPath)) {
      return AccessType.REQUIRED_AUTH;
    }
    if (matches(PUBLIC_RULES, normalizedMethod, normalizedPath)) {
      return AccessType.PUBLIC;
    }
    if (matches(OPTIONAL_AUTH_RULES, normalizedMethod, normalizedPath)) {
      return AccessType.OPTIONAL_AUTH;
    }
    return AccessType.PUBLIC;
  }

  public static boolean shouldSkipJwtFilter(String method, String path) {
    return matches(PUBLIC_RULES, normalizeMethod(method), normalizePath(path));
  }

  public static boolean requiresAuthentication(String method, String path) {
    return resolve(method, path) == AccessType.REQUIRED_AUTH;
  }

  private static boolean matches(List<EndpointRule> rules, String method, String path) {
    return rules.stream().anyMatch(rule -> rule.matches(method, path));
  }

  private static String normalizeMethod(String method) {
    return method == null ? "" : method.toUpperCase(Locale.ROOT);
  }

  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }
    int queryIndex = path.indexOf('?');
    String withoutQuery = queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    return withoutQuery.endsWith("/") && withoutQuery.length() > 1
        ? withoutQuery.substring(0, withoutQuery.length() - 1)
        : withoutQuery;
  }

  public enum AccessType {
    PUBLIC,
    OPTIONAL_AUTH,
    REQUIRED_AUTH
  }

  private record EndpointRule(String method, String path, MatchType matchType) {

    static EndpointRule exact(String method, String path) {
      return new EndpointRule(method, path, MatchType.EXACT);
    }

    static EndpointRule prefix(String method, String path) {
      return new EndpointRule(method, path, MatchType.PREFIX);
    }

    boolean matches(String requestMethod, String requestPath) {
      return matchesMethod(requestMethod) && matchesPath(requestPath);
    }

    private boolean matchesMethod(String requestMethod) {
      return "ANY".equals(method) || method.equals(requestMethod);
    }

    private boolean matchesPath(String requestPath) {
      return switch (matchType) {
        case EXACT -> path.equals(requestPath);
        case PREFIX -> path.equals(requestPath) || requestPath.startsWith(path + "/");
      };
    }
  }

  private enum MatchType {
    EXACT,
    PREFIX
  }
}
