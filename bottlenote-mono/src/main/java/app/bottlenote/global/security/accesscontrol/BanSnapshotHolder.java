package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** 요청 경로에서 lock 없이 읽는 ban snapshot holder. */
public class BanSnapshotHolder {

  private final AtomicReference<Snapshot> current = new AtomicReference<>(Snapshot.empty());
  private final int maxEntries;

  public BanSnapshotHolder(int maxEntries) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException("maxEntries must be positive");
    }
    this.maxEntries = maxEntries;
  }

  public Snapshot get() {
    return current.get();
  }

  public void replace(Snapshot snapshot) {
    Snapshot replacement = Objects.requireNonNull(snapshot, "snapshot must not be null");
    if (replacement.entries().size() > maxEntries) {
      Map<String, Entry> entries =
          replacement.entries().entrySet().stream()
              .sorted(
                  Comparator.comparing(
                          (Map.Entry<String, Entry> entry) -> entry.getValue().expiresAt())
                      .reversed()
                      .thenComparing(Map.Entry::getKey))
              .limit(maxEntries)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      replacement = new Snapshot(entries, replacement.refreshedAt());
    }
    current.set(replacement);
  }

  int maxEntries() {
    return maxEntries;
  }

  /** Redis ban 목록의 immutable 로컬 snapshot. */
  public record Snapshot(Map<String, Entry> entries, Instant refreshedAt) {

    public Snapshot {
      entries = Map.copyOf(entries);
      Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
    }

    public static Snapshot empty() {
      return new Snapshot(Map.of(), Instant.EPOCH);
    }

    public Optional<Entry> lookup(String ip, Instant now) {
      Entry entry = entries.get(ip);
      if (entry == null || entry.isExpired(now)) {
        return Optional.empty();
      }
      return Optional.of(entry);
    }

    public boolean isStale(Instant now, Duration staleThreshold) {
      return Duration.between(refreshedAt, now).compareTo(staleThreshold) > 0;
    }
  }

  public record Entry(Instant expiresAt) {

    public Entry {
      Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public boolean isExpired(Instant now) {
      return !now.isBefore(expiresAt);
    }

    public long remainingSeconds(Instant now) {
      return Math.max(Duration.between(now, expiresAt).getSeconds(), 0L);
    }

    public boolean isIndefinite() {
      return Instant.MAX.equals(expiresAt);
    }
  }
}
