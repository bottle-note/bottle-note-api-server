package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.security.accesscontrol.AccessControlService.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** access-control 결정·장애 카운터 (Phase1 최소 메트릭). */
public class AccessControlMetrics {

  private final Counter allow;
  private final Counter banned;
  private final Counter rateLimited;
  private final Counter storeError;
  private final Counter failOpen;
  private final Counter banFallbackSnapshotHit;
  private final Counter banFallbackSnapshotMiss;
  private final Counter banFallbackSnapshotStale;
  private final Counter rateLimitFallbackFailOpen;
  private final Counter rateLimitFallbackFailClosed;

  public AccessControlMetrics(MeterRegistry registry) {
    this.allow = decisionCounter(registry, "allow");
    this.banned = decisionCounter(registry, "banned");
    this.rateLimited = decisionCounter(registry, "rate_limited");
    this.storeError =
        Counter.builder("access_control_store_errors_total")
            .description("Access control store failures")
            .register(registry);
    this.failOpen =
        Counter.builder("access_control_fail_open_total")
            .description("Access control fail-open allows after store failure")
            .register(registry);
    this.banFallbackSnapshotHit = banFallbackCounter(registry, "snapshot_hit");
    this.banFallbackSnapshotMiss = banFallbackCounter(registry, "snapshot_miss");
    this.banFallbackSnapshotStale = banFallbackCounter(registry, "snapshot_stale");
    this.rateLimitFallbackFailOpen = rateLimitFallbackCounter(registry, "fail_open");
    this.rateLimitFallbackFailClosed = rateLimitFallbackCounter(registry, "fail_closed");
  }

  public static AccessControlMetrics noop() {
    return new AccessControlMetrics(new SimpleMeterRegistry());
  }

  public void record(Decision decision) {
    switch (decision.type()) {
      case ALLOW -> allow.increment();
      case BANNED -> banned.increment();
      case RATE_LIMITED -> rateLimited.increment();
    }
  }

  public void recordStoreError(boolean opened) {
    storeError.increment();
    if (opened) {
      failOpen.increment();
    }
  }

  public void recordBanFallback(BanFallbackResult result, boolean opened) {
    storeError.increment();
    if (opened) {
      failOpen.increment();
    }
    switch (result) {
      case SNAPSHOT_HIT -> banFallbackSnapshotHit.increment();
      case SNAPSHOT_MISS -> banFallbackSnapshotMiss.increment();
      case SNAPSHOT_STALE -> banFallbackSnapshotStale.increment();
    }
  }

  public void recordRateLimitFallback(boolean opened) {
    recordStoreError(opened);
    if (opened) {
      rateLimitFallbackFailOpen.increment();
      return;
    }
    rateLimitFallbackFailClosed.increment();
  }

  public enum BanFallbackResult {
    SNAPSHOT_HIT,
    SNAPSHOT_MISS,
    SNAPSHOT_STALE
  }

  private static Counter decisionCounter(MeterRegistry registry, String type) {
    return Counter.builder("access_control_decisions_total")
        .description("Access control decisions")
        .tag("type", type)
        .register(registry);
  }

  private static Counter banFallbackCounter(MeterRegistry registry, String result) {
    return Counter.builder("access_control_ban_fallback_total")
        .description("Ban lookup fallback results after access control store failure")
        .tag("result", result)
        .register(registry);
  }

  private static Counter rateLimitFallbackCounter(MeterRegistry registry, String result) {
    return Counter.builder("access_control_rate_limit_fallback_total")
        .description("Rate-limit fallback results after access control store failure")
        .tag("result", result)
        .register(registry);
  }
}
