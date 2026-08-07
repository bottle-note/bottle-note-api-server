package app.bottlenote.global.security.accesscontrol;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bottlenote.access-control")
public class AccessControlProperties {

  /** 액세스 컨트롤 필터 활성화 */
  private boolean enabled = true;

  /** Redis 장애 시 요청 허용 (true) / 거절 (false) */
  private boolean failOpen = true;

  private RateLimitRule defaultRateLimit = new RateLimitRule(300, 60);

  private List<PathRateLimitRule> pathRules = new ArrayList<>();

  private List<String> excludedPathPrefixes = new ArrayList<>(List.of("/actuator", "/favicon.ico"));

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

  public RateLimitRule getDefaultRateLimit() {
    return defaultRateLimit;
  }

  public void setDefaultRateLimit(RateLimitRule defaultRateLimit) {
    this.defaultRateLimit = defaultRateLimit;
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

  public record RateLimitRule(int limit, int windowSeconds) {
    public RateLimitRule {
      if (limit <= 0) {
        throw new IllegalArgumentException("limit must be positive");
      }
      if (windowSeconds <= 0) {
        throw new IllegalArgumentException("windowSeconds must be positive");
      }
    }

    // Spring Boot relaxed binding default ctor support
    public RateLimitRule() {
      this(300, 60);
    }
  }

  public static class PathRateLimitRule {
    private String pathPrefix = "";
    private int limit = 60;
    private int windowSeconds = 60;

    public String getPathPrefix() {
      return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
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
}
