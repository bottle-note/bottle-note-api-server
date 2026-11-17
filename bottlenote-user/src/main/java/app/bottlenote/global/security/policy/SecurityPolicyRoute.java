package app.bottlenote.global.security.policy;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public record SecurityPolicyRoute(
    Set<String> methods, PathPattern pathPattern, AuthType auth, Source source, String handler) {

  private static final PathPatternParser PATTERN_PARSER = PathPatternParser.defaultInstance;

  public static SecurityPolicyRoute explicit(String method, String pattern, AuthType auth) {
    Set<String> methods = method == null || method.isBlank() ? Set.of() : Set.of(normalize(method));
    return new SecurityPolicyRoute(
        methods, PATTERN_PARSER.parse(pattern), auth, Source.EXPLICIT, pattern);
  }

  public boolean matches(String method, String path) {
    return matches(method, PathContainer.parsePath(path));
  }

  public boolean matches(String method, PathContainer path) {
    return matchesMethod(method) && pathPattern.matches(path);
  }

  private boolean matchesMethod(String method) {
    return methods.isEmpty() || methods.contains(normalize(method));
  }

  private static String normalize(String method) {
    return method == null ? "" : method.toUpperCase(Locale.ROOT);
  }

  public enum Source {
    ANNOTATION,
    FALLBACK,
    EXPLICIT
  }
}
