package app.bottlenote.global.security.policy;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public final class SecurityPolicyRegistry {

  private final List<SecurityPolicyRoute> routes;
  private final AuthType fallback;

  public SecurityPolicyRegistry(List<SecurityPolicyRoute> routes, AuthType fallback) {
    this.routes = List.copyOf(routes);
    this.fallback = fallback;
  }

  public static SecurityPolicyRegistry of(
      List<SecurityPolicyRoute> routes, AuthType fallback, List<SecurityPolicyRoute> extraRoutes) {
    List<SecurityPolicyRoute> combined = new ArrayList<>(extraRoutes);
    combined.addAll(routes);
    return new SecurityPolicyRegistry(combined, fallback);
  }

  public AuthType resolve(HttpServletRequest request) {
    return resolve(request.getMethod(), lookupPath(request));
  }

  public AuthType resolve(String method, String path) {
    String lookupPath = normalizePath(path);
    return routes.stream()
        .filter(route -> route.matches(method, lookupPath))
        .findFirst()
        .map(SecurityPolicyRoute::auth)
        .orElse(fallback);
  }

  public boolean requiresAuthentication(HttpServletRequest request) {
    return resolve(request) == AuthType.REQUIRED_AUTH;
  }

  public boolean shouldSkipJwtFilter(HttpServletRequest request) {
    return resolve(request) == AuthType.PUBLIC;
  }

  public boolean shouldUseAnonymousAuthentication(String method, String path, String token) {
    return (token == null || token.isBlank()) && resolve(method, path) != AuthType.REQUIRED_AUTH;
  }

  public int routeCount() {
    return routes.size();
  }

  private static String lookupPath(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
      requestUri = requestUri.substring(contextPath.length());
    }
    return normalizePath(requestUri);
  }

  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }
    int queryIndex = path.indexOf('?');
    String withoutQuery = queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    return withoutQuery.isBlank() ? "/" : withoutQuery;
  }
}
