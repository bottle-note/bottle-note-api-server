package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 단위 테스트·로컬 전용. 다중 Pod에서는 사용하지 않는다. */
public class InMemoryAccessControlStore implements AccessControlStore {

  private final Map<String, BanEntry> bans = new ConcurrentHashMap<>();
  private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();

  @Override
  public boolean isBanned(String ip) {
    BanEntry entry = bans.get(ip);
    if (entry == null) {
      return false;
    }
    if (entry.expiresAt().isBefore(Instant.now())) {
      bans.remove(ip, entry);
      return false;
    }
    return true;
  }

  @Override
  public void ban(String ip, Duration ttl, String reason) {
    bans.put(ip, new BanEntry(reason == null ? "" : reason, Instant.now().plus(ttl)));
  }

  @Override
  public void unban(String ip) {
    bans.remove(ip);
  }

  @Override
  public BanInfo getBan(String ip) {
    BanEntry entry = bans.get(ip);
    if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
      bans.remove(ip);
      return null;
    }
    long ttl = Duration.between(Instant.now(), entry.expiresAt()).getSeconds();
    return new BanInfo(ip, entry.reason(), Math.max(ttl, 0));
  }

  @Override
  public long tryConsume(String key, int limit, Duration window) {
    Instant now = Instant.now();
    CounterEntry updated =
        counters.compute(
            key,
            (k, existing) -> {
              if (existing == null || existing.windowStart().plus(window).isBefore(now)) {
                return new CounterEntry(1, now);
              }
              return new CounterEntry(existing.count() + 1, existing.windowStart());
            });
    if (updated.count() > limit) {
      return -1;
    }
    return limit - updated.count();
  }

  private record BanEntry(String reason, Instant expiresAt) {}

  private record CounterEntry(long count, Instant windowStart) {}
}
