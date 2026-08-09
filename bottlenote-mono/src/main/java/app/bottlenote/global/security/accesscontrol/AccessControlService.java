package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.ConsumeResult;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AccessControlService {

  private static final String UNKNOWN_IP_TOKEN = "_unknown";

  private final AccessControlStore store;
  private final AccessControlProperties properties;
  private final AccessControlMetrics metrics;

  public Decision evaluate(String clientIp, String requestPath, String httpMethod) {
    // self-lockout 탈출: 관리 API는 ban/RL 모두 스킵
    if (isManagementPath(requestPath)) {
      Decision decision = Decision.allow();
      metrics.record(decision);
      return decision;
    }

    String effectiveIp = (clientIp == null || clientIp.isBlank()) ? UNKNOWN_IP_TOKEN : clientIp;
    String method = normalizeMethod(httpMethod);

    try {
      if (!UNKNOWN_IP_TOKEN.equals(effectiveIp) && store.isBanned(effectiveIp)) {
        BanInfo ban = store.getBan(effectiveIp);
        long retryAfter = ban == null || ban.ttlSeconds() < 0 ? 60 : Math.max(ban.ttlSeconds(), 1);
        Decision decision = Decision.banned(retryAfter);
        metrics.record(decision);
        return decision;
      }

      // exclude는 rate limit만 스킵 (ban은 위에서 이미 적용)
      if (isExcluded(requestPath)) {
        Decision decision = Decision.allow();
        metrics.record(decision);
        return decision;
      }

      Optional<PathRateLimitRule> matched =
          UNKNOWN_IP_TOKEN.equals(effectiveIp)
              ? Optional.empty()
              : matchPathRule(requestPath, method);
      RateLimitRule rule =
          UNKNOWN_IP_TOKEN.equals(effectiveIp)
              ? properties.getUnknownIpRateLimit()
              : matched
                  .map(r -> new RateLimitRule(r.getLimit(), r.getWindowSeconds()))
                  .orElse(properties.getDefaultRateLimit());
      String counterKey = rateLimitKey(effectiveIp, matched);
      ConsumeResult consumed =
          store.tryConsume(counterKey, rule.limit(), Duration.ofSeconds(rule.windowSeconds()));
      if (!consumed.allowed()) {
        Decision decision = Decision.rateLimited(consumed.retryAfterSeconds(), rule.limit());
        metrics.record(decision);
        return decision;
      }
      Decision decision = Decision.allow(consumed.remaining(), rule.limit());
      metrics.record(decision);
      return decision;
    } catch (RuntimeException ex) {
      log.warn(
          "access-control store failure ip={} path={} method={}: {}",
          effectiveIp,
          requestPath,
          method,
          ex.toString());
      if (properties.isFailOpen()) {
        metrics.recordStoreError(true);
        Decision decision = Decision.allow();
        metrics.record(decision);
        return decision;
      }
      metrics.recordStoreError(false);
      Decision decision = Decision.rateLimited(60, properties.getDefaultRateLimit().limit());
      metrics.record(decision);
      return decision;
    }
  }

  public void banIp(String ip, Duration ttl, String reason) {
    store.ban(requireNormalizedIp(ip), ttl, reason);
  }

  public void unbanIp(String ip) {
    store.unban(requireNormalizedIp(ip));
  }

  public BanInfo getBan(String ip) {
    return store.getBan(requireNormalizedIp(ip));
  }

  public List<BanInfo> listBans(int max) {
    return store.listBans(max);
  }

  private String rateLimitKey(String ip, Optional<PathRateLimitRule> matched) {
    String ns =
        properties.getKeyNamespace() == null || properties.getKeyNamespace().isBlank()
            ? "default"
            : properties.getKeyNamespace();
    String scope = UNKNOWN_IP_TOKEN.equals(ip) ? "unknown" : ruleScope(matched);
    return ns + ":" + ip + "|" + scope;
  }

  private boolean isManagementPath(String path) {
    if (path == null) {
      return false;
    }
    List<String> prefixes = properties.getManagementPathPrefixes();
    if (prefixes == null || prefixes.isEmpty()) {
      return false;
    }
    return prefixes.stream().anyMatch(path::startsWith);
  }

  private boolean isExcluded(String path) {
    if (path == null) {
      return true;
    }
    List<String> prefixes = properties.getExcludedPathPrefixes();
    if (prefixes == null) {
      return false;
    }
    return prefixes.stream().anyMatch(path::startsWith);
  }

  private Optional<PathRateLimitRule> matchPathRule(String path, String method) {
    List<PathRateLimitRule> pathRules = properties.getPathRules();
    if (pathRules == null || path == null) {
      return Optional.empty();
    }
    return pathRules.stream()
        .filter(rule -> rule.getPathPrefix() != null && path.startsWith(rule.getPathPrefix()))
        .filter(rule -> matchesMethod(rule, method))
        .max(Comparator.comparingInt(rule -> rule.getPathPrefix().length()));
  }

  private static boolean matchesMethod(PathRateLimitRule rule, String method) {
    List<String> methods = rule.getMethods();
    if (methods == null || methods.isEmpty()) {
      return true;
    }
    if (method == null || method.isBlank()) {
      return false;
    }
    return methods.stream()
        .filter(m -> m != null && !m.isBlank())
        .map(AccessControlService::normalizeMethod)
        .anyMatch(method::equals);
  }

  private static String ruleScope(Optional<PathRateLimitRule> matched) {
    if (matched.isEmpty()) {
      return "default";
    }
    PathRateLimitRule rule = matched.get();
    String prefix = rule.getPathPrefix() == null ? "default" : rule.getPathPrefix();
    String methodScope = methodScope(rule);
    return methodScope.isEmpty() ? prefix : prefix + "#" + methodScope;
  }

  private static String methodScope(PathRateLimitRule rule) {
    List<String> methods = rule.getMethods();
    if (methods == null || methods.isEmpty()) {
      return "";
    }
    return methods.stream()
        .filter(m -> m != null && !m.isBlank())
        .map(AccessControlService::normalizeMethod)
        .sorted()
        .collect(Collectors.joining(","));
  }

  private static String normalizeMethod(String method) {
    if (method == null || method.isBlank()) {
      return "";
    }
    return method.trim().toUpperCase(Locale.ROOT);
  }

  private static String requireNormalizedIp(String ip) {
    String normalized = ClientIpResolver.normalize(ip);
    if (normalized == null) {
      throw new AccessControlException(AccessControlExceptionCode.INVALID_IP);
    }
    return normalized;
  }

  public record Decision(Type type, long retryAfterSeconds, long remaining, long limit) {
    public enum Type {
      ALLOW,
      BANNED,
      RATE_LIMITED
    }

    public static Decision allow() {
      return new Decision(Type.ALLOW, 0, -1, -1);
    }

    public static Decision allow(long remaining, long limit) {
      return new Decision(Type.ALLOW, 0, remaining, limit);
    }

    public static Decision banned(long retryAfterSeconds) {
      return new Decision(Type.BANNED, retryAfterSeconds, -1, -1);
    }

    public static Decision rateLimited(long retryAfterSeconds, long limit) {
      return new Decision(Type.RATE_LIMITED, retryAfterSeconds, 0, limit);
    }

    public boolean allowed() {
      return type == Type.ALLOW;
    }
  }
}
