package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AccessControlService {

  private final AccessControlStore store;
  private final AccessControlProperties properties;
  private final AccessControlMetrics metrics;

  public Decision evaluate(String clientIp, String requestPath) {
    if (clientIp == null || clientIp.isBlank()) {
      Decision decision = Decision.allow();
      metrics.record(decision);
      return decision;
    }

    try {
      // ban은 exclude 경로에서도 적용 (actuator 등 ban 우회 방지)
      if (store.isBanned(clientIp)) {
        BanInfo ban = store.getBan(clientIp);
        long retryAfter = ban == null || ban.ttlSeconds() < 0 ? 60 : ban.ttlSeconds();
        Decision decision = Decision.banned(retryAfter);
        metrics.record(decision);
        return decision;
      }

      // exclude는 rate limit만 스킵
      if (isExcluded(requestPath)) {
        Decision decision = Decision.allow();
        metrics.record(decision);
        return decision;
      }

      RateLimitRule rule = resolveRule(requestPath);
      String counterKey = clientIp + "|" + ruleScope(requestPath);
      long remaining =
          store.tryConsume(counterKey, rule.limit(), Duration.ofSeconds(rule.windowSeconds()));
      if (remaining < 0) {
        Decision decision = Decision.rateLimited(rule.windowSeconds(), rule.limit());
        metrics.record(decision);
        return decision;
      }
      Decision decision = Decision.allow(remaining, rule.limit());
      metrics.record(decision);
      return decision;
    } catch (RuntimeException ex) {
      log.warn(
          "access-control store failure ip={} path={}: {}", clientIp, requestPath, ex.toString());
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

  private RateLimitRule resolveRule(String path) {
    List<PathRateLimitRule> pathRules = properties.getPathRules();
    if (pathRules != null && path != null) {
      return pathRules.stream()
          .filter(rule -> rule.getPathPrefix() != null && path.startsWith(rule.getPathPrefix()))
          .max(Comparator.comparingInt(rule -> rule.getPathPrefix().length()))
          .map(rule -> new RateLimitRule(rule.getLimit(), rule.getWindowSeconds()))
          .orElse(properties.getDefaultRateLimit());
    }
    return properties.getDefaultRateLimit();
  }

  private String ruleScope(String path) {
    List<PathRateLimitRule> pathRules = properties.getPathRules();
    if (pathRules != null && path != null) {
      return pathRules.stream()
          .filter(r -> r.getPathPrefix() != null && path.startsWith(r.getPathPrefix()))
          .max(Comparator.comparingInt(r -> r.getPathPrefix().length()))
          .map(PathRateLimitRule::getPathPrefix)
          .orElse("default");
    }
    return "default";
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
      return new Decision(Type.RATE_LIMITED, retryAfterSeconds, -1, limit);
    }

    public boolean allowed() {
      return type == Type.ALLOW;
    }
  }
}
