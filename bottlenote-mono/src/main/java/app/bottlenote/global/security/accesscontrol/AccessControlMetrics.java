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

  private static Counter decisionCounter(MeterRegistry registry, String type) {
    return Counter.builder("access_control_decisions_total")
        .description("Access control decisions")
        .tag("type", type)
        .register(registry);
  }
}
