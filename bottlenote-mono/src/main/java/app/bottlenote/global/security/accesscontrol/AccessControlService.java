package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.ConsumeResult;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AccessControlService {

  private static final String UNKNOWN_IP_TOKEN = "_unknown";

  private final AccessControlStore store;
  private final AccessControlProperties properties;
  private final AccessControlMetrics metrics;

  public Decision evaluate(String clientIp, String requestPath) {
    // self-lockout 탈출: 관리 API는 ban/RL 모두 스킵
    if (isManagementPath(requestPath)) {
      Decision decision = Decision.allow();
      metrics.record(decision);
      return decision;
    }

    String effectiveIp = (clientIp == null || clientIp.isBlank()) ? UNKNOWN_IP_TOKEN : clientIp;

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

      RateLimitRule rule =
          UNKNOWN_IP_TOKEN.equals(effectiveIp)
              ? properties.getUnknownIpRateLimit()
              : resolveRule(requestPath);
      String counterKey = rateLimitKey(effectiveIp, requestPath, rule);
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
          "access-control store failure ip={} path={}: {}",
          effectiveIp,
          requestPath,
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

  private String rateLimitKey(String ip, String path, RateLimitRule rule) {
    String ns =
        properties.getKeyNamespace() == null || properties.getKeyNamespace().isBlank()
            ? "default"
            : properties.getKeyNamespace();
    String scope = UNKNOWN_IP_TOKEN.equals(ip) ? "unknown" : ruleScope(path);
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
      return new Decision(Type.RATE_LIMITED, retryAfterSeconds, 0, limit);
    }

    public boolean allowed() {
      return type == Type.ALLOW;
    }
  }
}
