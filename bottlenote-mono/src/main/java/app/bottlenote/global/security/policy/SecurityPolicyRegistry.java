package app.bottlenote.global.security.policy;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;

public final class SecurityPolicyRegistry {

  private final List<SecurityPolicyRoute> routes;
  private static final AuthType UNKNOWN_ROUTE_AUTH = AuthType.PUBLIC;

  public SecurityPolicyRegistry(List<SecurityPolicyRoute> routes) {
    this.routes = List.copyOf(routes);
  }

  public static SecurityPolicyRegistry of(
      List<SecurityPolicyRoute> routes, List<SecurityPolicyRoute> extraRoutes) {
    List<SecurityPolicyRoute> combined = new ArrayList<>(extraRoutes);
    combined.addAll(routes);
    return new SecurityPolicyRegistry(combined);
  }

  public AuthType resolve(HttpServletRequest request) {
    return resolve(request.getMethod(), lookupPath(request));
  }

  public AuthType resolve(String method, String path) {
    String lookupPath = normalizePath(path);
    PathContainer pathContainer = PathContainer.parsePath(lookupPath);
    return routes.stream()
        .filter(route -> route.matches(method, pathContainer))
        .min(
            Comparator.comparing(
                SecurityPolicyRoute::pathPattern, PathPattern.SPECIFICITY_COMPARATOR))
        .map(SecurityPolicyRoute::auth)
        .orElse(UNKNOWN_ROUTE_AUTH);
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
