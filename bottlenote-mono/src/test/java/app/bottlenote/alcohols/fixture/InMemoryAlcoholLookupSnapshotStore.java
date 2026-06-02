package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private List<AlcoholLookupSnapshotItem> snapshot = new ArrayList<>();
  private final AtomicLong version = new AtomicLong();
  private int findAllCount;
  private int replaceAllCount;
  private boolean readFailure;

  @Override
  public List<AlcoholLookupSnapshotItem> findAll() {
    if (readFailure) {
      throw new IllegalStateException("snapshot read failed");
    }
    findAllCount++;
    return List.copyOf(snapshot);
  }

  @Override
  public Optional<String> findVersion() {
    if (readFailure) {
      throw new IllegalStateException("snapshot version read failed");
    }
    if (version.get() == 0L) {
      return Optional.empty();
    }
    return Optional.of(Long.toString(version.get()));
  }

  @Override
  public void replaceAll(List<AlcoholLookupSnapshotItem> items) {
    snapshot = new ArrayList<>(items);
    replaceAllCount++;
    version.incrementAndGet();
  }

  public int findAllCount() {
    return findAllCount;
  }

  public int replaceAllCount() {
    return replaceAllCount;
  }

  public void failReads() {
    readFailure = true;
  }
}
