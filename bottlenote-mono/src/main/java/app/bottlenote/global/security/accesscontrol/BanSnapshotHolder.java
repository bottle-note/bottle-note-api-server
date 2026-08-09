package app.bottlenote.global.security.accesscontrol;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** 요청 경로에서 lock 없이 읽는 ban snapshot holder. */
public class BanSnapshotHolder {

  private final AtomicReference<BanSnapshot> current = new AtomicReference<>(BanSnapshot.empty());
  private final int maxEntries;

  public BanSnapshotHolder(int maxEntries) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException("maxEntries must be positive");
    }
    this.maxEntries = maxEntries;
  }

  public BanSnapshot get() {
    return current.get();
  }

  public void replace(BanSnapshot snapshot) {
    BanSnapshot replacement = Objects.requireNonNull(snapshot, "snapshot must not be null");
    if (replacement.entries().size() > maxEntries) {
      Map<String, BanSnapshot.BanEntry> entries =
          replacement.entries().entrySet().stream()
              .sorted(
                  Comparator.comparing(
                          (Map.Entry<String, BanSnapshot.BanEntry> entry) ->
                              entry.getValue().expiresAt())
                      .reversed()
                      .thenComparing(Map.Entry::getKey))
              .limit(maxEntries)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      replacement = new BanSnapshot(entries, replacement.refreshedAt());
    }
    current.set(replacement);
  }
}
