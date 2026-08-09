package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bottlenote.access-control")
public class AccessControlProperties {

  /** 액세스 컨트롤 필터 활성화 */
  private boolean enabled = true;

  /** Redis 장애 시 요청 허용 (true) / 거절 (false) */
  private boolean failOpen = true;

  /**
   * access-control 전용 Redis 명령 타임아웃. 전역 {@code spring.data.redis.timeout}(기본 15s)과 분리한다. Redis 장애 시
   * 요청 스레드가 장시간 점유되지 않도록 짧게 유지한다.
   */
  private Duration redisCommandTimeout = Duration.ofMillis(200);

  /** rate-limit Lua 명령을 분산할 전용 Redis 연결 수 */
  private int rateLimitConnectionCount = 4;

  private Snapshot snapshot = new Snapshot();

  private BurstAdmission burstAdmission = new BurstAdmission();

  /** rate-limit 키 네임스페이스 (product/admin 분리). ban 키는 공유 차단을 위해 네임스페이스를 쓰지 않는다. */
  private String keyNamespace = "default";

  private RateLimitRule defaultRateLimit = new RateLimitRule(300, 60);

  /** IP 해석 실패 시 적용할 보수적 한도 */
  private RateLimitRule unknownIpRateLimit = new RateLimitRule(30, 60);

  private List<PathRateLimitRule> pathRules = new ArrayList<>();

  /** rate limit만 스킵 (ban은 적용) */
  private List<String> excludedPathPrefixes = new ArrayList<>(List.of("/actuator", "/favicon.ico"));

  /** ban + rate limit 모두 스킵 (self-lockout 탈출용 관리 API 등). admin `/v1/access-control` 권장. */
  private List<String> managementPathPrefixes = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isFailOpen() {
    return failOpen;
  }

  public void setFailOpen(boolean failOpen) {
    this.failOpen = failOpen;
  }

  public Duration getRedisCommandTimeout() {
    return redisCommandTimeout;
  }

  public void setRedisCommandTimeout(Duration redisCommandTimeout) {
    this.redisCommandTimeout = redisCommandTimeout;
  }

  public int getRateLimitConnectionCount() {
    return rateLimitConnectionCount;
  }

  public void setRateLimitConnectionCount(int rateLimitConnectionCount) {
    this.rateLimitConnectionCount = rateLimitConnectionCount;
  }

  public Snapshot getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(Snapshot snapshot) {
    this.snapshot = snapshot;
  }

  public BurstAdmission getBurstAdmission() {
    return burstAdmission;
  }

  public void setBurstAdmission(BurstAdmission burstAdmission) {
    this.burstAdmission = burstAdmission;
  }

  public String getKeyNamespace() {
    return keyNamespace;
  }

  public void setKeyNamespace(String keyNamespace) {
    this.keyNamespace = keyNamespace;
  }

  public RateLimitRule getDefaultRateLimit() {
    return defaultRateLimit;
  }

  public void setDefaultRateLimit(RateLimitRule defaultRateLimit) {
    this.defaultRateLimit = defaultRateLimit;
  }

  public RateLimitRule getUnknownIpRateLimit() {
    return unknownIpRateLimit;
  }

  public void setUnknownIpRateLimit(RateLimitRule unknownIpRateLimit) {
    this.unknownIpRateLimit = unknownIpRateLimit;
  }

  public List<PathRateLimitRule> getPathRules() {
    return pathRules;
  }

  public void setPathRules(List<PathRateLimitRule> pathRules) {
    this.pathRules = pathRules;
  }

  public List<String> getExcludedPathPrefixes() {
    return excludedPathPrefixes;
  }

  public void setExcludedPathPrefixes(List<String> excludedPathPrefixes) {
    this.excludedPathPrefixes = excludedPathPrefixes;
  }

  public List<String> getManagementPathPrefixes() {
    return managementPathPrefixes;
  }

  public void setManagementPathPrefixes(List<String> managementPathPrefixes) {
    this.managementPathPrefixes = managementPathPrefixes;
  }

  /** YAML 생성자 바인딩을 위해 no-arg 생성자는 두지 않는다. */
  public record RateLimitRule(int limit, int windowSeconds) {
    public RateLimitRule {
      if (limit <= 0) {
        throw new IllegalArgumentException("limit must be positive");
      }
      if (windowSeconds <= 0) {
        throw new IllegalArgumentException("windowSeconds must be positive");
      }
    }
  }

  public static class PathRateLimitRule {
    private String pathPrefix = "";

    /** 비어 있으면 모든 HTTP method에 적용 */
    private List<String> methods = new ArrayList<>();

    private int limit = 60;
    private int windowSeconds = 60;

    public String getPathPrefix() {
      return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }

    public List<String> getMethods() {
      return methods;
    }

    public void setMethods(List<String> methods) {
      this.methods = methods;
    }

    public int getLimit() {
      return limit;
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }

    public int getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
      this.windowSeconds = windowSeconds;
    }
  }

  public static class Snapshot {
    private long refreshIntervalMs = 30_000L;
    private Duration staleThreshold = Duration.ofMinutes(3);
    private int maxEntries = 10_000;

    public long getRefreshIntervalMs() {
      return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
      this.refreshIntervalMs = refreshIntervalMs;
    }

    public Duration getStaleThreshold() {
      return staleThreshold;
    }

    public void setStaleThreshold(Duration staleThreshold) {
      this.staleThreshold = staleThreshold;
    }

    public int getMaxEntries() {
      return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
      this.maxEntries = maxEntries;
    }
  }

  public static class BurstAdmission {
    private int maxConcurrent = 32;
    private Duration cooldown = Duration.ofSeconds(1);

    public int getMaxConcurrent() {
      return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
      this.maxConcurrent = maxConcurrent;
    }

    public Duration getCooldown() {
      return cooldown;
    }

    public void setCooldown(Duration cooldown) {
      this.cooldown = cooldown;
    }
  }
}
