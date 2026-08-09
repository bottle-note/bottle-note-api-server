package app.bottlenote.global.security.accesscontrol.fixture;

import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 단위 테스트 전용. 운영 multi-pod 경로에서는 사용하지 않는다. */
public class InMemoryAccessControlStore implements AccessControlStore {

  private final Map<String, BanEntry> bans = new ConcurrentHashMap<>();
  private final Map<String, Long> projectionVersions = new ConcurrentHashMap<>();
  private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();

  @Override
  public BanLookup lookupBan(String ip) {
    BanInfo ban = getBan(ip);
    return ban == null ? BanLookup.notBanned() : new BanLookup(true, ban.ttlSeconds());
  }

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
  public void projectBan(String ip, Duration ttl, String reason, long eventId) {
    projectionVersions.compute(
        ip,
        (ignored, current) -> {
          if (current == null || eventId >= current) {
            bans.put(ip, new BanEntry(reason == null ? "" : reason, Instant.now().plus(ttl)));
            return eventId;
          }
          return current;
        });
  }

  @Override
  public void projectUnban(String ip, long eventId) {
    projectionVersions.compute(
        ip,
        (ignored, current) -> {
          if (current == null || eventId >= current) {
            bans.remove(ip);
            return eventId;
          }
          return current;
        });
  }

  @Override
  public void removeUnversionedBan(String ip) {
    if (!projectionVersions.containsKey(ip)) {
      bans.remove(ip);
    }
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
  public List<BanInfo> listBans(int max) {
    int limit = Math.max(max, 0);
    List<BanInfo> result = new ArrayList<>();
    Instant now = Instant.now();
    for (Map.Entry<String, BanEntry> entry : bans.entrySet()) {
      if (result.size() >= limit) {
        break;
      }
      BanEntry ban = entry.getValue();
      if (ban.expiresAt().isBefore(now)) {
        bans.remove(entry.getKey(), ban);
        continue;
      }
      long ttl = Duration.between(now, ban.expiresAt()).getSeconds();
      result.add(new BanInfo(entry.getKey(), ban.reason(), Math.max(ttl, 0)));
    }
    return result;
  }

  @Override
  public ConsumeResult tryConsume(String key, int limit, Duration window) {
    Instant now = Instant.now();
    CounterEntry updated =
        counters.compute(
            key,
            (k, existing) -> {
              if (existing == null || !existing.windowStart().plus(window).isAfter(now)) {
                return new CounterEntry(1, now, window);
              }
              return new CounterEntry(
                  existing.count() + 1, existing.windowStart(), existing.window());
            });
    long retryAfter =
        Math.max(
            1, Duration.between(now, updated.windowStart().plus(updated.window())).getSeconds());
    if (updated.count() > limit) {
      return ConsumeResult.deny(retryAfter);
    }
    return ConsumeResult.allow(limit - updated.count());
  }

  private record BanEntry(String reason, Instant expiresAt) {}

  private record CounterEntry(long count, Instant windowStart, Duration window) {}
}
