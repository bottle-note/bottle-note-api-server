package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Redis ban 목록의 immutable 로컬 snapshot. */
public record BanSnapshot(Map<String, BanEntry> entries, Instant refreshedAt) {

  public BanSnapshot {
    entries = Map.copyOf(entries);
    Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
  }

  public static BanSnapshot empty() {
    return new BanSnapshot(Map.of(), Instant.EPOCH);
  }

  public Optional<BanEntry> lookup(String ip, Instant now) {
    BanEntry entry = entries.get(ip);
    if (entry == null || entry.isExpired(now)) {
      return Optional.empty();
    }
    return Optional.of(entry);
  }

  public boolean isStale(Instant now, Duration staleThreshold) {
    return Duration.between(refreshedAt, now).compareTo(staleThreshold) > 0;
  }

  public record BanEntry(Instant expiresAt, String reason) {

    public BanEntry {
      Objects.requireNonNull(expiresAt, "expiresAt must not be null");
      reason = reason == null ? "" : reason;
    }

    public boolean isExpired(Instant now) {
      return !now.isBefore(expiresAt);
    }

    public long remainingSeconds(Instant now) {
      return Math.max(Duration.between(now, expiresAt).getSeconds(), 0L);
    }
  }
}
